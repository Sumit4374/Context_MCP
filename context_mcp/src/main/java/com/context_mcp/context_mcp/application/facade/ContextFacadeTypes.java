package com.context_mcp.context_mcp.application.facade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable, transport-independent messages crossing the delivery/application boundary.
 * Content maps intentionally keep structured memory payloads extensible without making HTTP or
 * MCP types part of the application layer.
 */
public final class ContextFacadeTypes {

    private ContextFacadeTypes() {
    }

    public enum ExplicitAction {
        REMEMBER, SAVE, DO_NOT_SAVE, NONE
    }

    public enum CandidateScope {
        CONVERSATION, PROJECT, TOPIC, ARTIFACT, GLOBAL_PREFERENCE
    }

    public enum RecommendationDecision {
        SAVE_RECOMMENDED, ASK_USER, DO_NOT_SAVE, EXPLICIT_ACTION_REQUIRED
    }

    public enum DeletionMode {
        SOFT, PERMANENT
    }

    public enum ExportFormat {
        JSON, MARKDOWN
    }

    public record ArtifactReference(
            String path,
            String name,
            String kind,
            String contentType,
            Map<String, Object> metadata) {
        public ArtifactReference {
            metadata = immutableMap(metadata);
        }
    }

    public record CheckpointCandidate(
            String conversationId,
            String sourceClient,
            Integer turnNumber,
            Integer checkpointNumber,
            String title,
            String userSummary,
            String assistantSummary,
            List<String> candidateFacts,
            List<ArtifactReference> candidateArtifacts,
            List<String> projectHints,
            List<String> tags,
            List<String> openQuestions,
            String rawTranscriptExcerpt,
            ExplicitAction explicitAction,
            CandidateScope scope,
            Instant occurredAt,
            Map<String, Object> attributes) {
        public CheckpointCandidate {
            candidateFacts = immutableList(candidateFacts);
            candidateArtifacts = immutableList(candidateArtifacts);
            projectHints = immutableList(projectHints);
            tags = immutableList(tags);
            openQuestions = immutableList(openQuestions);
            explicitAction = explicitAction == null ? ExplicitAction.NONE : explicitAction;
            scope = scope == null ? CandidateScope.CONVERSATION : scope;
            attributes = immutableMap(attributes);
        }
    }

    public record ArtifactSuggestion(String path, String reason, BigDecimal confidence) {
    }

    public record CheckpointEvaluation(
            UUID checkpointId,
            RecommendationDecision decision,
            BigDecimal confidence,
            List<String> reasons,
            String suggestedMemoryType,
            String suggestedProject,
            String suggestedSummary,
            boolean sensitiveContentDetected,
            List<ArtifactSuggestion> suggestedArtifacts,
            boolean persisted,
            UUID persistedMemoryId) {
        public CheckpointEvaluation {
            reasons = immutableList(reasons);
            suggestedArtifacts = immutableList(suggestedArtifacts);
        }
    }

    public record CheckpointConfirmation(
            UUID checkpointId,
            boolean confirmed,
            UUID optionalProjectId,
            String title,
            String summary,
            Map<String, Object> userEdits) {
        public CheckpointConfirmation {
            userEdits = immutableMap(userEdits);
        }
    }

    public record ExplicitMemoryCommand(
            String content,
            String type,
            CandidateScope scope,
            String projectName,
            UUID projectId,
            BigDecimal importance,
            List<String> artifactPaths,
            String title,
            Map<String, Object> structuredContent) {
        public ExplicitMemoryCommand {
            scope = scope == null ? CandidateScope.GLOBAL_PREFERENCE : scope;
            artifactPaths = immutableList(artifactPaths);
            structuredContent = immutableMap(structuredContent);
        }
    }

    public record SuppressionCommand(
            String conversationId,
            UUID checkpointId,
            CandidateScope scope,
            String reason,
            Instant expiresAt) {
        public SuppressionCommand {
            scope = scope == null ? CandidateScope.CONVERSATION : scope;
        }
    }

    public record OperationResult(
            String status,
            boolean successful,
            String message,
            UUID memoryId,
            UUID checkpointId,
            UUID artifactId,
            List<String> redactions,
            Map<String, Object> details) {
        public OperationResult {
            redactions = immutableList(redactions);
            details = immutableMap(details);
        }

        public static OperationResult accepted(String status, String message) {
            return new OperationResult(status, true, message, null, null, null, List.of(), Map.of());
        }
    }

    public record SearchRequest(
            String query,
            String project,
            UUID projectId,
            List<String> types,
            List<String> topics,
            List<String> tags,
            Instant from,
            Instant to,
            Integer limit,
            boolean includeRelated) {
        public SearchRequest {
            types = immutableList(types);
            topics = immutableList(topics);
            tags = immutableList(tags);
            limit = limit == null ? 10 : limit;
        }
    }

    public record SourceReference(
            String sourceClient,
            String conversationId,
            UUID checkpointId,
            Instant savedAt) {
    }

    public record RelatedReference(UUID id, String type, String title, String relationshipType) {
    }

    public record SearchHit(
            UUID memoryId,
            String title,
            String summary,
            String type,
            String project,
            BigDecimal relevance,
            SourceReference source,
            List<ArtifactView> relatedArtifacts,
            List<RelatedReference> relatedMemories) {
        public SearchHit {
            relatedArtifacts = immutableList(relatedArtifacts);
            relatedMemories = immutableList(relatedMemories);
        }
    }

    public record SearchResult(List<SearchHit> results, long total, String searchMode) {
        public SearchResult {
            results = immutableList(results);
        }
    }

    public record MemoryView(
            UUID id,
            String title,
            String summary,
            Map<String, Object> content,
            String type,
            String status,
            BigDecimal importance,
            BigDecimal confidence,
            String sensitivity,
            String explicitness,
            SourceReference source,
            List<ProjectSummary> projects,
            List<String> topics,
            List<String> tags,
            List<ArtifactView> artifacts,
            List<RelatedReference> relationships,
            Instant createdAt,
            Instant updatedAt,
            Instant lastRetrievedAt,
            long version) {
        public MemoryView {
            content = immutableMap(content);
            projects = immutableList(projects);
            topics = immutableList(topics);
            tags = immutableList(tags);
            artifacts = immutableList(artifacts);
            relationships = immutableList(relationships);
        }
    }

    public record ProjectListRequest(
            String query,
            boolean includeArchived,
            Integer limit) {
        public ProjectListRequest {
            limit = limit == null ? 50 : limit;
        }
    }

    public record ProjectSummary(
            UUID id,
            String name,
            String description,
            long memoryCount,
            long artifactCount,
            long openQuestionCount,
            Instant lastActivityAt,
            String status) {
    }

    public record ProjectUpsert(
            UUID id,
            String name,
            String description,
            UUID parentProjectId,
            List<String> tags,
            String status,
            Map<String, Object> metadata) {
        public ProjectUpsert {
            tags = immutableList(tags);
            metadata = immutableMap(metadata);
        }
    }

    public record ProjectView(
            UUID id,
            String name,
            String description,
            UUID parentProjectId,
            List<String> tags,
            String status,
            Map<String, Object> metadata,
            Instant createdAt,
            Instant updatedAt) {
        public ProjectView {
            tags = immutableList(tags);
            metadata = immutableMap(metadata);
        }
    }

    public record ArtifactLinkCommand(
            String path,
            UUID projectId,
            UUID topicId,
            UUID memoryId,
            UUID checkpointId,
            String kind,
            Map<String, Object> metadata) {
        public ArtifactLinkCommand {
            metadata = immutableMap(metadata);
        }
    }

    public record ArtifactView(
            UUID id,
            String filename,
            String displayPath,
            String contentType,
            String kind,
            Long sizeBytes,
            String sha256,
            String sensitivity,
            boolean existsOnDisk,
            Instant createdAt,
            Instant modifiedAt,
            Map<String, Object> generationMetadata) {
        public ArtifactView {
            generationMetadata = immutableMap(generationMetadata);
        }
    }

    public record OpenQuestionRequest(UUID projectId, String project, UUID topicId, String topic, Integer limit) {
        public OpenQuestionRequest {
            limit = limit == null ? 50 : limit;
        }
    }

    public record OpenQuestion(
            UUID memoryId,
            String question,
            String project,
            String topic,
            Instant createdAt,
            BigDecimal confidence) {
    }

    public record ConsolidationCommand(
            UUID projectId,
            UUID topicId,
            boolean confirmed,
            Integer limit) {
        public ConsolidationCommand {
            limit = limit == null ? 100 : limit;
        }
    }

    public record ConsolidationProposal(
            UUID id,
            List<UUID> sourceMemoryIds,
            String action,
            String rationale,
            String proposedSummary,
            BigDecimal confidence) {
        public ConsolidationProposal {
            sourceMemoryIds = immutableList(sourceMemoryIds);
        }
    }

    public record ConsolidationResult(
            boolean applied,
            List<ConsolidationProposal> proposals,
            int appliedCount,
            String message) {
        public ConsolidationResult {
            proposals = immutableList(proposals);
        }
    }

    public record ContextExportRequest(
            ExportFormat format,
            Set<UUID> memoryIds,
            UUID projectId,
            String conversationId,
            boolean includeArtifacts,
            boolean includeSensitiveContent) {
        public ContextExportRequest {
            format = format == null ? ExportFormat.JSON : format;
            memoryIds = immutableSet(memoryIds);
        }
    }

    public record ExportResult(
            String format,
            String content,
            String suggestedFilename,
            long recordCount,
            Instant generatedAt) {
    }

    public record DeletionCommand(
            String targetType,
            UUID id,
            String conversationId,
            DeletionMode mode,
            boolean confirmed) {
        public DeletionCommand {
            mode = mode == null ? DeletionMode.SOFT : mode;
        }
    }

    public record DeletionResult(
            boolean deleted,
            boolean confirmationRequired,
            String message,
            long affectedRecords) {
    }

    public record ConversationView(
            String externalConversationId,
            String sourceClient,
            String title,
            Instant lastCheckpointAt,
            long checkpointCount,
            List<MemoryView> memories) {
        public ConversationView {
            memories = immutableList(memories);
        }
    }

    public record HealthSnapshot(String status, Map<String, Object> components, Instant checkedAt) {
        public HealthSnapshot {
            components = immutableMap(components);
        }
    }

    public record PublicConfiguration(
            boolean localOnly,
            boolean checkpointingEnabled,
            int defaultTurnThreshold,
            boolean requireUserConfirmation,
            boolean rawTranscriptStorageEnabled,
            boolean semanticSearchEnabled,
            boolean keywordSearchEnabled,
            boolean artifactWatcherEnabled,
            List<String> approvedArtifactRoots) {
        public PublicConfiguration {
            approvedArtifactRoots = immutableList(approvedArtifactRoots);
        }
    }

    private static <T> List<T> immutableList(List<T> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private static <T> Set<T> immutableSet(Set<T> value) {
        return value == null ? Set.of() : Set.copyOf(value);
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> value) {
        return value == null ? Map.of() : Map.copyOf(value);
    }
}
