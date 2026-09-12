package com.context_mcp.context_mcp.api.rest;

import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.*;
import com.context_mcp.context_mcp.application.facade.ContextPlatformFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for local administration and future browser-extension integration.
 * All endpoints mirror the MCP tools but are exposed over HTTP.
 */
@RestController
@RequestMapping("/api/v1")
public class ContextApiController {

    private final ContextPlatformFacade facade;

    public ContextApiController(ContextPlatformFacade facade) {
        this.facade = facade;
    }

    // ---- Health ----

    @GetMapping("/health")
    public HealthSnapshot health() {
        return facade.health();
    }

    @GetMapping("/configuration")
    public PublicConfiguration configuration() {
        return facade.publicConfiguration();
    }

    // ---- Checkpoint ----

    @PostMapping("/checkpoints/evaluate")
    public CheckpointEvaluation evaluateCheckpoint(@RequestBody CheckpointCandidate candidate) {
        return facade.evaluateCheckpoint(candidate);
    }

    @PostMapping("/checkpoints/confirm")
    public OperationResult confirmCheckpoint(@RequestBody CheckpointConfirmation confirmation) {
        return facade.confirmCheckpoint(confirmation);
    }

    // ---- Memory ----

    @PostMapping("/memories/remember")
    public OperationResult remember(@RequestBody ExplicitMemoryCommand command) {
        return facade.remember(command);
    }

    @GetMapping("/memories/{id}")
    public ResponseEntity<MemoryView> getMemory(@PathVariable UUID id) {
        return facade.findMemory(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- Search ----

    @PostMapping("/search")
    public SearchResult search(@RequestBody SearchRequest request) {
        return facade.search(request);
    }

    // ---- Projects ----

    @GetMapping("/projects")
    public List<ProjectSummary> listProjects(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        return facade.listProjects(new ProjectListRequest(query, includeArchived, 50));
    }

    @PostMapping("/projects")
    public ProjectView createOrUpdateProject(@RequestBody ProjectUpsert command) {
        return facade.createOrUpdateProject(command);
    }

    // ---- Artifacts ----

    @GetMapping("/artifacts/{id}")
    public ResponseEntity<ArtifactView> getArtifact(@PathVariable UUID id) {
        return facade.findArtifact(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/artifacts/link")
    public OperationResult linkArtifact(@RequestBody ArtifactLinkCommand command) {
        return facade.linkArtifact(command);
    }

    // ---- Open Questions ----

    @GetMapping("/open-questions")
    public List<OpenQuestion> listOpenQuestions(
            @RequestParam(required = false) String project,
            @RequestParam(required = false) UUID projectId) {
        return facade.listOpenQuestions(new OpenQuestionRequest(projectId, project, null, null, 50));
    }

    // ---- Suppression ----

    @PostMapping("/suppressions")
    public OperationResult suppress(@RequestBody SuppressionCommand command) {
        return facade.suppress(command);
    }

    // ---- Consolidation ----

    @PostMapping("/admin/consolidate")
    public ConsolidationResult consolidate(@RequestBody ConsolidationCommand command) {
        return facade.consolidate(command);
    }

    // ---- Export ----

    @PostMapping("/exports")
    public ExportResult export(@RequestBody ContextExportRequest request) {
        return facade.exportContext(request);
    }

    // ---- Deletion ----

    @PostMapping("/delete")
    public DeletionResult delete(@RequestBody DeletionCommand command) {
        return facade.delete(command);
    }

    // ---- Conversations ----

    @GetMapping("/conversations/{externalId}")
    public ResponseEntity<ConversationView> getConversation(@PathVariable String externalId) {
        return facade.findConversation(externalId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
