package com.context_mcp.context_mcp.application.facade;

import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ArtifactLinkCommand;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ArtifactView;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.CheckpointCandidate;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.CheckpointConfirmation;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.CheckpointEvaluation;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ConsolidationCommand;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ConsolidationResult;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ConversationView;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ContextExportRequest;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.DeletionCommand;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.DeletionResult;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ExplicitMemoryCommand;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ExportResult;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.HealthSnapshot;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.MemoryView;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.OpenQuestion;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.OpenQuestionRequest;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.OperationResult;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ProjectListRequest;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ProjectSummary;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ProjectUpsert;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ProjectView;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.PublicConfiguration;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.SearchRequest;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.SearchResult;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.SuppressionCommand;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transport-neutral application boundary for REST, MCP, and future client adapters.
 *
 * <p>Implementations own transactions, persistence, classification, redaction, and audit
 * recording. Transport adapters must not reach into repositories or domain entities directly.
 */
public interface ContextPlatformFacade {

    CheckpointEvaluation evaluateCheckpoint(CheckpointCandidate candidate);

    OperationResult confirmCheckpoint(CheckpointConfirmation confirmation);

    OperationResult remember(ExplicitMemoryCommand command);

    OperationResult suppress(SuppressionCommand command);

    SearchResult search(SearchRequest request);

    Optional<MemoryView> findMemory(UUID memoryId);

    List<ProjectSummary> listProjects(ProjectListRequest request);

    ProjectView createOrUpdateProject(ProjectUpsert command);

    OperationResult linkArtifact(ArtifactLinkCommand command);

    List<OpenQuestion> listOpenQuestions(OpenQuestionRequest request);

    ConsolidationResult consolidate(ConsolidationCommand command);

    ExportResult exportContext(ContextExportRequest request);

    DeletionResult delete(DeletionCommand command);

    Optional<ConversationView> findConversation(String externalConversationId);

    Optional<ArtifactView> findArtifact(UUID artifactId);

    HealthSnapshot health();

    PublicConfiguration publicConfiguration();
}
