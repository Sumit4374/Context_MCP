package com.context_mcp.context_mcp.application.facade;

import com.context_mcp.context_mcp.application.audit.AuditService;
import com.context_mcp.context_mcp.application.suppression.SuppressionService;
import com.context_mcp.context_mcp.config.ContextPlatformProperties;
import com.context_mcp.context_mcp.domain.enums.AuditAction;
import com.context_mcp.context_mcp.domain.model.*;
import com.context_mcp.context_mcp.infrastructure.classifier.ContextRelevanceClassifier;
import com.context_mcp.context_mcp.infrastructure.classifier.ContextRelevanceClassifier.ClassificationResult;
import com.context_mcp.context_mcp.infrastructure.security.SecretRedactor;
import com.context_mcp.context_mcp.infrastructure.security.RedactionResult;
import com.context_mcp.context_mcp.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.*;

/**
 * Transport-neutral application boundary.  Owns transactions, orchestrates
 * classification, redaction, persistence, and audit recording.
 */
@Service
public class DefaultContextPlatformFacade implements ContextPlatformFacade {

    private static final Logger log = LoggerFactory.getLogger(DefaultContextPlatformFacade.class);

    private final ContextPlatformProperties properties;
    private final ContextRelevanceClassifier classifier;
    private final SecretRedactor redactor;
    private final AuditService auditService;
    private final SuppressionService suppressionService;
    private final MemoryRepository memoryRepository;
    private final ProjectRepository projectRepository;
    private final TopicRepository topicRepository;
    private final TagRepository tagRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationCheckpointRepository checkpointRepository;
    private final ArtifactRepository artifactRepository;
    private final RelationshipRepository relationshipRepository;
    private final EmbeddingJobRepository embeddingJobRepository;
    private final ObjectMapper objectMapper;

    public DefaultContextPlatformFacade(
            ContextPlatformProperties properties,
            ContextRelevanceClassifier classifier,
            SecretRedactor redactor,
            AuditService auditService,
            SuppressionService suppressionService,
            MemoryRepository memoryRepository,
            ProjectRepository projectRepository,
            TopicRepository topicRepository,
            TagRepository tagRepository,
            ConversationRepository conversationRepository,
            ConversationCheckpointRepository checkpointRepository,
            ArtifactRepository artifactRepository,
            RelationshipRepository relationshipRepository,
            EmbeddingJobRepository embeddingJobRepository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.classifier = classifier;
        this.redactor = redactor;
        this.auditService = auditService;
        this.suppressionService = suppressionService;
        this.memoryRepository = memoryRepository;
        this.projectRepository = projectRepository;
        this.topicRepository = topicRepository;
        this.tagRepository = tagRepository;
        this.conversationRepository = conversationRepository;
        this.checkpointRepository = checkpointRepository;
        this.artifactRepository = artifactRepository;
        this.relationshipRepository = relationshipRepository;
        this.embeddingJobRepository = embeddingJobRepository;
        this.objectMapper = objectMapper;
    }

    // ================= Checkpoint Lifecycle =================

    @Override
    @Transactional
    public CheckpointEvaluation evaluateCheckpoint(CheckpointCandidate candidate) {
        // 1. Redact secrets from summaries before anything else
        RedactionResult userRedaction = redactor.redact(candidate.userSummary());
        RedactionResult assistantRedaction = redactor.redact(candidate.assistantSummary());
        boolean sensitiveDetected = userRedaction.hadSensitiveContent() || assistantRedaction.hadSensitiveContent();

        if (sensitiveDetected) {
            auditService.record(AuditAction.SECRET_REDACTED, "CHECKPOINT", null,
                    Map.of("detections", userRedaction.detections().size() + assistantRedaction.detections().size()));
        }

        // Build redacted candidate for classification
        CheckpointCandidate redactedCandidate = new CheckpointCandidate(
                candidate.conversationId(), candidate.sourceClient(),
                candidate.turnNumber(), candidate.checkpointNumber(),
                candidate.title(), userRedaction.content(), assistantRedaction.content(),
                candidate.candidateFacts(), candidate.candidateArtifacts(),
                candidate.projectHints(), candidate.tags(), candidate.openQuestions(),
                null, // never pass raw transcript to classifier
                candidate.explicitAction(), candidate.scope(),
                candidate.occurredAt(), candidate.attributes());

        // 2. Handle explicit actions
        if (candidate.explicitAction() == ExplicitAction.DO_NOT_SAVE) {
            ConversationCheckpointEntity cp = saveCheckpointRecord(redactedCandidate, "DO_NOT_SAVE", true);
            suppressionService.suppress(new SuppressionCommand(
                    candidate.conversationId(), cp.getId(),
                    CandidateScope.CHECKPOINT, "User explicit DO_NOT_SAVE", null));
            return new CheckpointEvaluation(cp.getId(), RecommendationDecision.DO_NOT_SAVE,
                    BigDecimal.ONE, List.of("User explicitly declined persistence"),
                    null, null, null, sensitiveDetected, List.of(), false, null);
        }

        if (candidate.explicitAction() == ExplicitAction.REMEMBER ||
            candidate.explicitAction() == ExplicitAction.SAVE) {
            // Persist immediately
            ConversationCheckpointEntity cp = saveCheckpointRecord(redactedCandidate, "EXPLICIT_ACTION_REQUIRED", false);
            MemoryEntity memory = createMemoryFromCandidate(redactedCandidate, "EXPLICIT_SAVE",
                    redactedCandidate.assistantSummary());
            linkProject(memory, redactedCandidate);
            linkTags(memory, redactedCandidate);
            cp.setUserConfirmation("CONFIRMED");
            cp.setConfirmedAt(Instant.now());
            checkpointRepository.save(cp);
            queueEmbeddingJob(memory.getId());

            auditService.record(AuditAction.MEMORY_CREATED, "MEMORY", memory.getId(),
                    Map.of("explicitness", "EXPLICIT_SAVE", "type", memory.getMemoryType()));

            return new CheckpointEvaluation(cp.getId(), RecommendationDecision.EXPLICIT_ACTION_REQUIRED,
                    BigDecimal.ONE, List.of("Explicit save — persisted immediately"),
                    memory.getMemoryType(), firstProjectHint(redactedCandidate),
                    memory.getSummary(), sensitiveDetected, List.of(), true, memory.getId());
        }

        // 3. Classify with rules
        ClassificationResult classification = classifier.classify(redactedCandidate);

        // 4. Determine decision
        RecommendationDecision decision;
        boolean autoSave = false;

        if (classification.suggestedMemoryType().equals("TRANSIENT") ||
            classification.durabilityScore().compareTo(new BigDecimal("0.3")) < 0) {
            decision = RecommendationDecision.DO_NOT_SAVE;
        } else if (properties.getCheckpoint().getAutoSaveCategories()
                .contains(classification.suggestedMemoryType())) {
            decision = RecommendationDecision.SAVE_RECOMMENDED;
            autoSave = !properties.getCheckpoint().isRequireUserConfirmation();
        } else {
            decision = properties.getCheckpoint().isRequireUserConfirmation()
                    ? RecommendationDecision.ASK_USER
                    : RecommendationDecision.SAVE_RECOMMENDED;
        }

        // 5. Store checkpoint record
        ConversationCheckpointEntity cp = saveCheckpointRecord(redactedCandidate, decision.name(), false);

        // 6. Auto-save if configured
        UUID persistedMemoryId = null;
        boolean persisted = false;
        if (autoSave && decision == RecommendationDecision.SAVE_RECOMMENDED) {
            MemoryEntity memory = createMemoryFromCandidate(redactedCandidate, "AUTO_POLICY",
                    classification.suggestedSummary());
            memory.setMemoryType(classification.suggestedMemoryType());
            linkProject(memory, redactedCandidate);
            linkTags(memory, redactedCandidate);
            memoryRepository.save(memory);
            cp.setUserConfirmation("AUTO_SAVED");
            cp.setConfirmedAt(Instant.now());
            checkpointRepository.save(cp);
            queueEmbeddingJob(memory.getId());
            persistedMemoryId = memory.getId();
            persisted = true;
            auditService.record(AuditAction.MEMORY_CREATED, "MEMORY", memory.getId(),
                    Map.of("explicitness", "AUTO_POLICY", "type", memory.getMemoryType()));
        }

        auditService.record(AuditAction.CHECKPOINT_EVALUATED, "CHECKPOINT", cp.getId(),
                Map.of("decision", decision.name(), "confidence", classification.confidence().toPlainString()));

        return new CheckpointEvaluation(cp.getId(), decision, classification.confidence(),
                classification.reasons(), classification.suggestedMemoryType(),
                classification.suggestedProject(), classification.suggestedSummary(),
                sensitiveDetected || classification.sensitiveContentDetected(),
                List.of(), persisted, persistedMemoryId);
    }

    @Override
    @Transactional
    public OperationResult confirmCheckpoint(CheckpointConfirmation confirmation) {
        ConversationCheckpointEntity cp = checkpointRepository.findById(confirmation.checkpointId())
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found: " + confirmation.checkpointId()));

        if (!confirmation.confirmed()) {
            cp.setUserConfirmation("DECLINED");
            cp.setSuppressed(true);
            checkpointRepository.save(cp);
            auditService.record(AuditAction.CHECKPOINT_SUPPRESSED, "CHECKPOINT", cp.getId());
            return new OperationResult("DECLINED", true, "Checkpoint declined by user",
                    null, cp.getId(), null, List.of(), Map.of());
        }

        // Create memory from stored checkpoint
        String summary = confirmation.summary() != null ? confirmation.summary() : extractField(cp.getEvaluation(), "suggestedSummary");
        String title = confirmation.title() != null ? confirmation.title() : extractField(cp.getRequestPayload(), "title");
        String memoryType = extractField(cp.getEvaluation(), "suggestedMemoryType");
        if (memoryType == null || memoryType.isBlank()) memoryType = "GENERAL_NOTE";

        MemoryEntity memory = new MemoryEntity();
        memory.setTitle(title);
        memory.setSummary(summary);
        memory.setMemoryType(memoryType);
        memory.setExplicitness("CONFIRMED_RECOMMENDATION");
        memory.setSourceClient(cp.getSourceClient());
        memory.setSourceConversationId(cp.getExternalConversationId());
        memory.setSourceCheckpointId(cp.getId());
        memory.setContent(cp.getRequestPayload());

        if (confirmation.optionalProjectId() != null) {
            projectRepository.findById(confirmation.optionalProjectId())
                    .ifPresent(p -> memory.getProjects().add(p));
        }

        memory = memoryRepository.save(memory);
        queueEmbeddingJob(memory.getId());

        cp.setUserConfirmation("CONFIRMED");
        cp.setConfirmedAt(Instant.now());
        checkpointRepository.save(cp);

        auditService.record(AuditAction.CHECKPOINT_CONFIRMED, "CHECKPOINT", cp.getId(),
                Map.of("memoryId", memory.getId().toString()));
        auditService.record(AuditAction.MEMORY_CREATED, "MEMORY", memory.getId(),
                Map.of("explicitness", "CONFIRMED_RECOMMENDATION", "type", memoryType));

        return new OperationResult("CONFIRMED", true, "Memory created from confirmed checkpoint",
                memory.getId(), cp.getId(), null, List.of(), Map.of());
    }

    // ================= Explicit Memory =================

    @Override
    @Transactional
    public OperationResult remember(ExplicitMemoryCommand command) {
        RedactionResult redaction = redactor.redact(command.content());
        List<String> redactionNotes = redaction.hadSensitiveContent()
                ? List.of("Content was redacted before persistence")
                : List.of();

        MemoryEntity memory = new MemoryEntity();
        memory.setTitle(command.title() != null ? command.title() : truncate(redaction.content(), 100));
        memory.setSummary(redaction.content());
        memory.setMemoryType(command.type() != null ? command.type() : "GENERAL_NOTE");
        memory.setExplicitness("EXPLICIT_SAVE");
        memory.setImportance(command.importance() != null ? command.importance() : new BigDecimal("0.7"));
        memory.setConfidence(BigDecimal.ONE);

        try {
            if (!command.structuredContent().isEmpty()) {
                memory.setContent(objectMapper.writeValueAsString(command.structuredContent()));
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize structured content for explicit memory", e);
        }

        // Link to project by name or ID
        if (command.projectId() != null) {
            projectRepository.findById(command.projectId())
                    .ifPresent(p -> memory.getProjects().add(p));
        } else if (command.projectName() != null && !command.projectName().isBlank()) {
            ProjectEntity project = projectRepository.findByName(command.projectName())
                    .orElseGet(() -> {
                        ProjectEntity p = new ProjectEntity();
                        p.setName(command.projectName());
                        return projectRepository.save(p);
                    });
            memory.getProjects().add(project);
        }

        memory = memoryRepository.save(memory);
        queueEmbeddingJob(memory.getId());

        auditService.record(AuditAction.MEMORY_CREATED, "MEMORY", memory.getId(),
                Map.of("explicitness", "EXPLICIT_SAVE", "type", memory.getMemoryType()));

        return new OperationResult("SAVED", true, "Memory persisted immediately",
                memory.getId(), null, null, redactionNotes, Map.of());
    }

    // ================= Suppression =================

    @Override
    @Transactional
    public OperationResult suppress(SuppressionCommand command) {
        // Also mark checkpoint as suppressed if present
        if (command.checkpointId() != null) {
            checkpointRepository.findById(command.checkpointId()).ifPresent(cp -> {
                cp.setSuppressed(true);
                cp.setUserConfirmation("SUPPRESSED");
                checkpointRepository.save(cp);
            });
        }

        UUID ruleId = suppressionService.suppress(command);
        return new OperationResult("SUPPRESSED", true, "Suppression rule recorded",
                null, command.checkpointId(), null, List.of(),
                Map.of("suppressionRuleId", ruleId.toString()));
    }

    // ================= Search =================

    @Override
    @Transactional(readOnly = true)
    public SearchResult search(SearchRequest request) {
        int limit = Math.min(request.limit(), properties.getSearch().getMaxLimit());
        List<MemoryEntity> results;
        String searchMode;

        // Determine search mode
        if (request.query() != null && !request.query().isBlank() && properties.getSearch().isKeywordEnabled()) {
            if (request.projectId() != null) {
                results = memoryRepository.fullTextSearchByProject(request.query(), request.projectId(), limit);
            } else if (request.project() != null && !request.project().isBlank()) {
                // Resolve project by name, then search
                UUID projectId = projectRepository.findByName(request.project())
                        .map(ProjectEntity::getId).orElse(null);
                if (projectId != null) {
                    results = memoryRepository.fullTextSearchByProject(request.query(), projectId, limit);
                } else {
                    results = memoryRepository.fullTextSearch(request.query(), limit);
                }
            } else {
                results = memoryRepository.fullTextSearch(request.query(), limit);
            }
            searchMode = "keyword";
        } else if (request.projectId() != null) {
            results = memoryRepository.findActiveByProjectId(request.projectId());
            searchMode = "project_filter";
        } else {
            results = memoryRepository.findAll().stream()
                    .filter(m -> "ACTIVE".equals(m.getStatus()))
                    .limit(limit)
                    .toList();
            searchMode = "unfiltered";
        }

        // Apply type filtering
        if (!request.types().isEmpty()) {
            results = results.stream()
                    .filter(m -> request.types().contains(m.getMemoryType()))
                    .toList();
        }

        // Apply time filtering
        if (request.from() != null) {
            results = results.stream()
                    .filter(m -> m.getCreatedAt().isAfter(request.from()) || m.getCreatedAt().equals(request.from()))
                    .toList();
        }
        if (request.to() != null) {
            results = results.stream()
                    .filter(m -> m.getCreatedAt().isBefore(request.to()))
                    .toList();
        }

        // Record retrieval for reinforcement
        results.forEach(MemoryEntity::recordRetrieval);
        memoryRepository.saveAll(results);

        List<SearchHit> hits = results.stream()
                .map(m -> toSearchHit(m, request.includeRelated()))
                .toList();

        auditService.record(AuditAction.SEARCH_EXECUTED, "SEARCH", null,
                Map.of("query", request.query() != null ? request.query() : "", "mode", searchMode,
                       "resultCount", hits.size()));

        return new SearchResult(hits, hits.size(), searchMode);
    }

    // ================= Memory Lookup =================

    @Override
    @Transactional(readOnly = true)
    public Optional<MemoryView> findMemory(UUID memoryId) {
        return memoryRepository.findById(memoryId).map(this::toMemoryView);
    }

    // ================= Projects =================

    @Override
    @Transactional(readOnly = true)
    public List<ProjectSummary> listProjects(ProjectListRequest request) {
        List<ProjectEntity> projects;
        if (request.query() != null && !request.query().isBlank()) {
            projects = projectRepository.searchByQuery(request.query());
        } else {
            projects = request.includeArchived()
                    ? projectRepository.findAll()
                    : projectRepository.findAllActive();
        }
        return projects.stream()
                .limit(request.limit())
                .map(this::toProjectSummary)
                .toList();
    }

    @Override
    @Transactional
    public ProjectView createOrUpdateProject(ProjectUpsert command) {
        ProjectEntity project;
        if (command.id() != null) {
            project = projectRepository.findById(command.id())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + command.id()));
            auditService.record(AuditAction.PROJECT_UPDATED, "PROJECT", project.getId());
        } else {
            project = new ProjectEntity();
            auditService.record(AuditAction.PROJECT_CREATED, "PROJECT", null,
                    Map.of("name", command.name()));
        }

        project.setName(command.name());
        if (command.description() != null) project.setDescription(command.description());
        if (command.parentProjectId() != null) project.setParentProjectId(command.parentProjectId());
        if (command.status() != null) project.setStatus(command.status());
        try {
            if (!command.tags().isEmpty()) project.setTags(objectMapper.writeValueAsString(command.tags()));
            if (!command.metadata().isEmpty()) project.setMetadata(objectMapper.writeValueAsString(command.metadata()));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize project metadata", e);
        }

        project = projectRepository.save(project);
        return toProjectView(project);
    }

    // ================= Artifacts =================

    @Override
    @Transactional
    public OperationResult linkArtifact(ArtifactLinkCommand command) {
        ArtifactEntity artifact = artifactRepository.findByCanonicalPath(command.path())
                .orElseGet(() -> {
                    ArtifactEntity a = new ArtifactEntity();
                    a.setCanonicalPath(command.path());
                    a.setOriginalFilename(command.path().substring(command.path().lastIndexOf('/') + 1));
                    a.setArtifactType(command.kind());
                    return artifactRepository.save(a);
                });

        if (command.memoryId() != null) {
            memoryRepository.findById(command.memoryId()).ifPresent(m -> {
                m.getArtifacts().add(artifact);
                memoryRepository.save(m);
            });
        }

        auditService.record(AuditAction.ARTIFACT_LINKED, "ARTIFACT", artifact.getId(),
                Map.of("path", command.path()));

        return new OperationResult("LINKED", true, "Artifact linked",
                null, null, artifact.getId(), List.of(), Map.of());
    }

    // ================= Open Questions =================

    @Override
    @Transactional(readOnly = true)
    public List<OpenQuestion> listOpenQuestions(OpenQuestionRequest request) {
        List<MemoryEntity> questions;
        if (request.projectId() != null) {
            questions = memoryRepository.findOpenQuestionsByProject(request.projectId());
        } else if (request.project() != null) {
            UUID projectId = projectRepository.findByName(request.project())
                    .map(ProjectEntity::getId).orElse(null);
            questions = projectId != null
                    ? memoryRepository.findOpenQuestionsByProject(projectId)
                    : memoryRepository.findActiveOpenQuestions();
        } else {
            questions = memoryRepository.findActiveOpenQuestions();
        }
        return questions.stream()
                .limit(request.limit())
                .map(m -> new OpenQuestion(m.getId(), m.getSummary(),
                        m.getProjects().stream().findFirst().map(ProjectEntity::getName).orElse(null),
                        null, m.getCreatedAt(), m.getConfidence()))
                .toList();
    }

    // ================= Consolidation (Milestone 6 stub) =================

    @Override
    @Transactional(readOnly = true)
    public ConsolidationResult consolidate(ConsolidationCommand command) {
        // TODO: Implement full consolidation in Milestone 6
        return new ConsolidationResult(false, List.of(), 0,
                "Consolidation dry-run: no proposals generated yet (implementation pending)");
    }

    // ================= Export (Milestone 6 stub) =================

    @Override
    @Transactional(readOnly = true)
    public ExportResult exportContext(ContextExportRequest request) {
        List<MemoryEntity> memories;
        if (request.projectId() != null) {
            memories = memoryRepository.findActiveByProjectId(request.projectId());
        } else if (!request.memoryIds().isEmpty()) {
            memories = memoryRepository.findAllById(request.memoryIds());
        } else {
            memories = memoryRepository.findAll().stream()
                    .filter(m -> "ACTIVE".equals(m.getStatus()))
                    .toList();
        }

        String content;
        String filename;
        if (request.format() == ExportFormat.MARKDOWN) {
            content = exportAsMarkdown(memories);
            filename = "context_export_" + Instant.now().getEpochSecond() + ".md";
        } else {
            content = exportAsJson(memories);
            filename = "context_export_" + Instant.now().getEpochSecond() + ".json";
        }

        auditService.record(AuditAction.EXPORT_GENERATED, "EXPORT", null,
                Map.of("format", request.format().name(), "count", memories.size()));

        return new ExportResult(request.format().name(), content, filename,
                memories.size(), Instant.now());
    }

    // ================= Deletion =================

    @Override
    @Transactional
    public DeletionResult delete(DeletionCommand command) {
        if (!command.confirmed() && command.mode() == DeletionMode.PERMANENT) {
            return new DeletionResult(false, true,
                    "Permanent deletion requires explicit confirmation", 0);
        }

        if ("MEMORY".equalsIgnoreCase(command.targetType()) && command.id() != null) {
            return memoryRepository.findById(command.id()).map(memory -> {
                if (command.mode() == DeletionMode.SOFT) {
                    memory.setStatus("DELETED");
                    memoryRepository.save(memory);
                    auditService.record(AuditAction.MEMORY_DELETED, "MEMORY", memory.getId());
                    return new DeletionResult(true, false, "Memory soft-deleted", 1);
                } else {
                    memoryRepository.delete(memory);
                    auditService.record(AuditAction.MEMORY_PERMANENTLY_DELETED, "MEMORY", command.id());
                    return new DeletionResult(true, false, "Memory permanently deleted", 1);
                }
            }).orElse(new DeletionResult(false, false, "Memory not found", 0));
        }

        if ("PROJECT".equalsIgnoreCase(command.targetType()) && command.id() != null) {
            return projectRepository.findById(command.id()).map(project -> {
                project.setStatus("DELETED");
                projectRepository.save(project);
                auditService.record(AuditAction.PROJECT_UPDATED, "PROJECT", project.getId(),
                        Map.of("action", "deleted"));
                return new DeletionResult(true, false, "Project soft-deleted", 1);
            }).orElse(new DeletionResult(false, false, "Project not found", 0));
        }

        return new DeletionResult(false, false, "Unsupported target type: " + command.targetType(), 0);
    }

    // ================= Conversation Lookup =================

    @Override
    @Transactional(readOnly = true)
    public Optional<ConversationView> findConversation(String externalConversationId) {
        return conversationRepository.findByExternalConversationId(externalConversationId)
                .map(conv -> {
                    List<MemoryEntity> memories = memoryRepository.findByConversationId(externalConversationId);
                    long checkpointCount = checkpointRepository.countByExternalConversationId(externalConversationId);
                    return new ConversationView(conv.getExternalConversationId(),
                            conv.getSourceClient(), conv.getTitle(), conv.getUpdatedAt(),
                            checkpointCount,
                            memories.stream().map(this::toMemoryView).toList());
                });
    }

    // ================= Artifact Lookup =================

    @Override
    @Transactional(readOnly = true)
    public Optional<ArtifactView> findArtifact(UUID artifactId) {
        return artifactRepository.findById(artifactId).map(this::toArtifactView);
    }

    // ================= Health & Configuration =================

    @Override
    public HealthSnapshot health() {
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", "UP");
        components.put("checkpointing", properties.getCheckpoint().isEnabled() ? "ENABLED" : "DISABLED");
        components.put("embeddingProvider", properties.getEmbeddings().getProvider());
        components.put("artifactWatcher", properties.getArtifacts().isWatcherEnabled() ? "ENABLED" : "DISABLED");
        return new HealthSnapshot("UP", components, Instant.now());
    }

    @Override
    public PublicConfiguration publicConfiguration() {
        return new PublicConfiguration(
                properties.getServer().isLocalOnly(),
                properties.getCheckpoint().isEnabled(),
                properties.getCheckpoint().getDefaultTurnThreshold(),
                properties.getCheckpoint().isRequireUserConfirmation(),
                properties.getPersistence().isStoreRawTranscript(),
                properties.getSearch().isSemanticEnabled(),
                properties.getSearch().isKeywordEnabled(),
                properties.getArtifacts().isWatcherEnabled(),
                properties.getArtifacts().getRoots());
    }

    // ================= Internal Helpers =================

    private ConversationCheckpointEntity saveCheckpointRecord(CheckpointCandidate candidate, String decision, boolean suppressed) {
        ConversationCheckpointEntity cp = new ConversationCheckpointEntity();
        cp.setExternalConversationId(candidate.conversationId() != null ? candidate.conversationId() : "unknown");
        cp.setSourceClient(candidate.sourceClient());
        cp.setTurnNumber(candidate.turnNumber());
        cp.setCheckpointNumber(candidate.checkpointNumber());
        cp.setDecision(decision);
        cp.setSuppressed(suppressed);

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", candidate.title());
            payload.put("userSummary", candidate.userSummary());
            payload.put("assistantSummary", candidate.assistantSummary());
            payload.put("candidateFacts", candidate.candidateFacts());
            payload.put("projectHints", candidate.projectHints());
            payload.put("tags", candidate.tags());
            payload.put("openQuestions", candidate.openQuestions());
            cp.setRequestPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize checkpoint payload", e);
        }

        // Ensure conversation record exists
        if (candidate.conversationId() != null) {
            conversationRepository.findByExternalConversationId(candidate.conversationId())
                    .orElseGet(() -> {
                        ConversationEntity conv = new ConversationEntity();
                        conv.setExternalConversationId(candidate.conversationId());
                        conv.setSourceClient(candidate.sourceClient());
                        conv.setTitle(candidate.title());
                        return conversationRepository.save(conv);
                    });
        }

        return checkpointRepository.save(cp);
    }

    private MemoryEntity createMemoryFromCandidate(CheckpointCandidate candidate, String explicitness, String summary) {
        MemoryEntity memory = new MemoryEntity();
        memory.setTitle(candidate.title());
        memory.setSummary(summary != null ? summary : candidate.userSummary());
        memory.setMemoryType("GENERAL_NOTE");
        memory.setExplicitness(explicitness);
        memory.setSourceClient(candidate.sourceClient());
        memory.setSourceConversationId(candidate.conversationId());

        try {
            Map<String, Object> contentMap = new LinkedHashMap<>();
            contentMap.put("userSummary", candidate.userSummary());
            contentMap.put("assistantSummary", candidate.assistantSummary());
            if (!candidate.candidateFacts().isEmpty()) contentMap.put("facts", candidate.candidateFacts());
            if (!candidate.openQuestions().isEmpty()) contentMap.put("openQuestions", candidate.openQuestions());
            memory.setContent(objectMapper.writeValueAsString(contentMap));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize memory content", e);
        }

        // Classify to set type
        ClassificationResult cr = classifier.classify(candidate);
        if (cr.suggestedMemoryType() != null && !cr.suggestedMemoryType().equals("GENERAL_NOTE")) {
            memory.setMemoryType(cr.suggestedMemoryType());
        }
        memory.setImportance(cr.durabilityScore() != null ? cr.durabilityScore() : new BigDecimal("0.5"));
        memory.setConfidence(cr.confidence() != null ? cr.confidence() : new BigDecimal("0.5"));

        return memoryRepository.save(memory);
    }

    private void linkProject(MemoryEntity memory, CheckpointCandidate candidate) {
        if (!candidate.projectHints().isEmpty()) {
            String projectName = candidate.projectHints().getFirst();
            ProjectEntity project = projectRepository.findByName(projectName)
                    .orElseGet(() -> {
                        ProjectEntity p = new ProjectEntity();
                        p.setName(projectName);
                        return projectRepository.save(p);
                    });
            memory.getProjects().add(project);
            memoryRepository.save(memory);
        }
    }

    private void linkTags(MemoryEntity memory, CheckpointCandidate candidate) {
        for (String tagName : candidate.tags()) {
            TagEntity tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        TagEntity t = new TagEntity();
                        t.setName(tagName);
                        return tagRepository.save(t);
                    });
            memory.getTags().add(tag);
        }
        if (!candidate.tags().isEmpty()) {
            memoryRepository.save(memory);
        }
    }

    private void queueEmbeddingJob(UUID memoryId) {
        if (!"none".equals(properties.getEmbeddings().getProvider())) {
            EmbeddingJobEntity job = new EmbeddingJobEntity();
            job.setMemoryId(memoryId);
            embeddingJobRepository.save(job);
        }
    }

    private String firstProjectHint(CheckpointCandidate c) {
        return c.projectHints().isEmpty() ? null : c.projectHints().getFirst();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }

    private String extractField(String json, String field) {
        try {
            var node = objectMapper.readTree(json);
            var value = node.get(field);
            return value != null ? value.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ---- View Mappers ----

    private SearchHit toSearchHit(MemoryEntity m, boolean includeRelated) {
        String project = m.getProjects().stream().findFirst().map(ProjectEntity::getName).orElse(null);
        SourceReference source = new SourceReference(m.getSourceClient(), m.getSourceConversationId(),
                m.getSourceCheckpointId(), m.getCreatedAt());

        List<RelatedReference> related = List.of();
        if (includeRelated) {
            related = relationshipRepository.findBySourceIdOrTargetId(m.getId(), m.getId()).stream()
                    .map(r -> new RelatedReference(
                            r.getSourceId().equals(m.getId()) ? r.getTargetId() : r.getSourceId(),
                            r.getTargetType(), null, r.getRelationshipType()))
                    .toList();
        }

        return new SearchHit(m.getId(), m.getTitle(), m.getSummary(), m.getMemoryType(),
                project, m.getImportance(), source, List.of(), related);
    }

    private MemoryView toMemoryView(MemoryEntity m) {
        SourceReference source = new SourceReference(m.getSourceClient(), m.getSourceConversationId(),
                m.getSourceCheckpointId(), m.getCreatedAt());

        Map<String, Object> content;
        try {
            content = objectMapper.readValue(m.getContent(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            content = Map.of();
        }

        List<String> tagNames = m.getTags().stream().map(TagEntity::getName).toList();
        List<ArtifactView> artifactViews = m.getArtifacts().stream().map(this::toArtifactView).toList();
        List<ProjectSummary> projectSummaries = m.getProjects().stream().map(this::toProjectSummary).toList();
        List<RelatedReference> relationships = relationshipRepository.findBySourceIdOrTargetId(m.getId(), m.getId())
                .stream()
                .map(r -> new RelatedReference(
                        r.getSourceId().equals(m.getId()) ? r.getTargetId() : r.getSourceId(),
                        r.getTargetType(), null, r.getRelationshipType()))
                .toList();

        return new MemoryView(m.getId(), m.getTitle(), m.getSummary(), content,
                m.getMemoryType(), m.getStatus(), m.getImportance(), m.getConfidence(),
                m.getSensitivity(), m.getExplicitness(), source, projectSummaries,
                List.of(), tagNames, artifactViews, relationships,
                m.getCreatedAt(), m.getUpdatedAt(), m.getLastRetrievedAt(), m.getVersion());
    }

    private ProjectSummary toProjectSummary(ProjectEntity p) {
        long memoryCount = memoryRepository.countActiveByProject(p.getId());
        long openQuestionCount = memoryRepository.countOpenQuestionsByProject(p.getId());
        return new ProjectSummary(p.getId(), p.getName(), p.getDescription(),
                memoryCount, 0, openQuestionCount, p.getUpdatedAt(), p.getStatus());
    }

    private ProjectView toProjectView(ProjectEntity p) {
        List<String> tags;
        Map<String, Object> metadata;
        try {
            tags = objectMapper.readValue(p.getTags(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            metadata = objectMapper.readValue(p.getMetadata(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            tags = List.of();
            metadata = Map.of();
        }
        return new ProjectView(p.getId(), p.getName(), p.getDescription(), p.getParentProjectId(),
                tags, p.getStatus(), metadata, p.getCreatedAt(), p.getUpdatedAt());
    }

    private ArtifactView toArtifactView(ArtifactEntity a) {
        Map<String, Object> genMeta;
        try {
            genMeta = objectMapper.readValue(a.getGenerationMetadata(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            genMeta = Map.of();
        }
        return new ArtifactView(a.getId(), a.getOriginalFilename(), a.getCanonicalPath(),
                a.getMimeType(), a.getArtifactType(), a.getSizeBytes(), a.getSha256(),
                a.getSensitivity(), a.isExistsOnDisk(), a.getCreatedAt(), a.getModifiedAt(), genMeta);
    }

    private String exportAsJson(List<MemoryEntity> memories) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(memories.stream().map(this::toMemoryView).toList());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String exportAsMarkdown(List<MemoryEntity> memories) {
        StringBuilder sb = new StringBuilder("# Context Export\n\n");
        sb.append("Generated: ").append(Instant.now()).append("\n\n");
        for (MemoryEntity m : memories) {
            sb.append("## ").append(m.getTitle() != null ? m.getTitle() : "Untitled").append("\n\n");
            sb.append("**Type**: ").append(m.getMemoryType()).append("  \n");
            sb.append("**Created**: ").append(m.getCreatedAt()).append("  \n");
            if (m.getSummary() != null) sb.append("\n").append(m.getSummary()).append("\n");
            sb.append("\n---\n\n");
        }
        return sb.toString();
    }
}
