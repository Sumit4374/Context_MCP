package com.context_mcp.context_mcp.mcp.tools;

import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.*;
import com.context_mcp.context_mcp.application.facade.ContextPlatformFacade;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP tools for project management, artifact linking, open questions,
 * consolidation, export, and deletion.
 */
@Component
public class ContextManagementTools {

    private final ContextPlatformFacade facade;

    public ContextManagementTools(ContextPlatformFacade facade) {
        this.facade = facade;
    }

    @Tool(name = "context_list_projects",
          description = "List projects with memory counts, recent activity, and open question counts.")
    public List<ProjectSummary> listProjects(
            @ToolParam(description = "Optional search query to filter projects") String query,
            @ToolParam(description = "Include archived projects") Boolean includeArchived) {
        ProjectListRequest request = new ProjectListRequest(query,
                includeArchived != null && includeArchived, 50);
        return facade.listProjects(request);
    }

    @Tool(name = "context_create_or_update_project",
          description = "Create a new project or update an existing one.")
    public ProjectView createOrUpdateProject(
            @ToolParam(description = "Project ID (null to create new)") String id,
            @ToolParam(description = "Project name") String name,
            @ToolParam(description = "Project description") String description,
            @ToolParam(description = "Parent project ID") String parentProjectId) {

        ProjectUpsert upsert = new ProjectUpsert(
                id != null ? UUID.fromString(id) : null,
                name, description,
                parentProjectId != null ? UUID.fromString(parentProjectId) : null,
                List.of(), "ACTIVE", Map.of());

        return facade.createOrUpdateProject(upsert);
    }

    @Tool(name = "context_link_artifact",
          description = "Link a local file artifact to a project, topic, memory, or checkpoint.")
    public OperationResult linkArtifact(
            @ToolParam(description = "Absolute local file path") String path,
            @ToolParam(description = "Project ID to link to") String projectId,
            @ToolParam(description = "Memory ID to link to") String memoryId,
            @ToolParam(description = "Artifact kind (e.g. document, image, code)") String kind) {

        ArtifactLinkCommand command = new ArtifactLinkCommand(
                path,
                projectId != null ? UUID.fromString(projectId) : null,
                null,
                memoryId != null ? UUID.fromString(memoryId) : null,
                null, kind, Map.of());

        return facade.linkArtifact(command);
    }

    @Tool(name = "context_list_open_questions",
          description = "Retrieve unresolved questions by project or topic.")
    public List<OpenQuestion> listOpenQuestions(
            @ToolParam(description = "Project name to filter by") String project,
            @ToolParam(description = "Project ID to filter by") String projectId) {

        OpenQuestionRequest request = new OpenQuestionRequest(
                projectId != null ? UUID.fromString(projectId) : null,
                project, null, null, 50);

        return facade.listOpenQuestions(request);
    }

    @Tool(name = "context_consolidate",
          description = "Run dry-run or confirmed memory consolidation for a project/topic. " +
                        "Finds duplicates and proposes merges without silently deleting anything.")
    public ConsolidationResult consolidate(
            @ToolParam(description = "Project ID to consolidate") String projectId,
            @ToolParam(description = "Apply proposals if true, dry-run if false") Boolean confirmed) {

        ConsolidationCommand command = new ConsolidationCommand(
                projectId != null ? UUID.fromString(projectId) : null,
                null,
                confirmed != null && confirmed,
                100);

        return facade.consolidate(command);
    }

    @Tool(name = "context_export",
          description = "Export selected context in JSON or Markdown format.")
    public ExportResult export(
            @ToolParam(description = "Format: JSON or MARKDOWN") String format,
            @ToolParam(description = "Project ID to export") String projectId) {

        ExportFormat exportFormat = "MARKDOWN".equalsIgnoreCase(format)
                ? ExportFormat.MARKDOWN : ExportFormat.JSON;

        ContextExportRequest request = new ContextExportRequest(
                exportFormat, java.util.Set.of(),
                projectId != null ? UUID.fromString(projectId) : null,
                null, false, false);

        return facade.exportContext(request);
    }

    @Tool(name = "context_delete",
          description = "Soft-delete or permanently delete context. Permanent deletion requires explicit confirmation.")
    public DeletionResult delete(
            @ToolParam(description = "Target type: MEMORY, PROJECT") String targetType,
            @ToolParam(description = "Target entity ID") String id,
            @ToolParam(description = "Deletion mode: SOFT or PERMANENT") String mode,
            @ToolParam(description = "Explicit confirmation for permanent deletes") Boolean confirmed) {

        DeletionMode deletionMode = "PERMANENT".equalsIgnoreCase(mode)
                ? DeletionMode.PERMANENT : DeletionMode.SOFT;

        DeletionCommand command = new DeletionCommand(
                targetType,
                id != null ? UUID.fromString(id) : null,
                null, deletionMode,
                confirmed != null && confirmed);

        return facade.delete(command);
    }
}
