package com.context_mcp.context_mcp.mcp.tools;

import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.*;
import com.context_mcp.context_mcp.application.facade.ContextPlatformFacade;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP tools for memory operations — search, remember, suppression, lookup.
 */
@Component
public class ContextMemoryTools {

    private final ContextPlatformFacade facade;

    public ContextMemoryTools(ContextPlatformFacade facade) {
        this.facade = facade;
    }

    @Tool(name = "context_remember",
          description = "Immediately persist an explicit user memory. Use this when the user says " +
                        "'remember this', 'save this', or similar. The content is persisted after secret redaction.")
    public OperationResult remember(
            @ToolParam(description = "The content to remember") String content,
            @ToolParam(description = "Memory type: PREFERENCE, ARCHITECTURE_DECISION, REQUIREMENT, etc.") String type,
            @ToolParam(description = "Scope: PROJECT, CONVERSATION, GLOBAL_PREFERENCE, etc.") String scope,
            @ToolParam(description = "Project name to associate with") String projectName,
            @ToolParam(description = "Importance score from 0.0 to 1.0") Double importance,
            @ToolParam(description = "Optional title for the memory") String title) {

        CandidateScope candidateScope = parseScope(scope);
        BigDecimal imp = importance != null ? BigDecimal.valueOf(importance) : new BigDecimal("0.7");

        ExplicitMemoryCommand command = new ExplicitMemoryCommand(
                content, type, candidateScope, projectName, null, imp,
                List.of(), title, Map.of());

        return facade.remember(command);
    }

    @Tool(name = "context_do_not_save",
          description = "Record a suppression decision — the user has explicitly declined to save a checkpoint or conversation.")
    public OperationResult doNotSave(
            @ToolParam(description = "External conversation ID") String conversationId,
            @ToolParam(description = "Checkpoint ID to suppress (from a previous evaluation)") String checkpointId,
            @ToolParam(description = "Scope: CHECKPOINT, CONVERSATION, TOPIC") String scope,
            @ToolParam(description = "Reason for suppression") String reason) {

        SuppressionCommand command = new SuppressionCommand(
                conversationId,
                checkpointId != null ? UUID.fromString(checkpointId) : null,
                parseScope(scope), reason, null);

        return facade.suppress(command);
    }

    @Tool(name = "context_search",
          description = "Search durable context using keywords, project filters, type filters, and time ranges. " +
                        "Returns concise, well-cited context records with source provenance.")
    public SearchResult search(
            @ToolParam(description = "Search query text") String query,
            @ToolParam(description = "Filter by project name") String project,
            @ToolParam(description = "Filter by memory types (e.g. ARCHITECTURE_DECISION, REQUIREMENT)") List<String> types,
            @ToolParam(description = "Maximum number of results") Integer limit,
            @ToolParam(description = "Include related memories and artifacts") Boolean includeRelated) {

        SearchRequest request = new SearchRequest(query, project, null,
                types != null ? types : List.of(),
                List.of(), List.of(), null, null,
                limit != null ? limit : 10,
                includeRelated != null && includeRelated);

        return facade.search(request);
    }

    @Tool(name = "context_get_memory",
          description = "Fetch a specific memory by ID, including structured fields, sources, artifacts, and relationships.")
    public MemoryView getMemory(
            @ToolParam(description = "Memory UUID") String memoryId) {
        return facade.findMemory(UUID.fromString(memoryId)).orElse(null);
    }

    private CandidateScope parseScope(String scope) {
        if (scope == null || scope.isBlank()) return CandidateScope.CONVERSATION;
        try {
            return CandidateScope.valueOf(scope.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CandidateScope.CONVERSATION;
        }
    }
}
