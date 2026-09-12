package com.context_mcp.context_mcp.mcp.tools;

import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.*;
import com.context_mcp.context_mcp.application.facade.ContextPlatformFacade;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP tools for the checkpoint evaluation and confirmation workflow.
 * These are the primary integration points for AI client adapters.
 */
@Component
public class ContextCheckpointTools {

    private final ContextPlatformFacade facade;

    public ContextCheckpointTools(ContextPlatformFacade facade) {
        this.facade = facade;
    }

    @Tool(name = "context_checkpoint_evaluate",
          description = "Evaluate a conversation checkpoint for persistence worthiness. " +
                        "Returns a recommendation (SAVE_RECOMMENDED, ASK_USER, DO_NOT_SAVE, EXPLICIT_ACTION_REQUIRED) " +
                        "with confidence and reasons. Does not persist unless the checkpoint contains an explicit save command.")
    public CheckpointEvaluation evaluateCheckpoint(
            @ToolParam(description = "External conversation ID from the AI client") String conversationId,
            @ToolParam(description = "Source client/platform (e.g. codex, chatgpt, claude, browser)") String sourceClient,
            @ToolParam(description = "Current user turn number in the conversation") Integer turnNumber,
            @ToolParam(description = "Optional title for this checkpoint") String title,
            @ToolParam(description = "Summary of the user's request/input") String userSummary,
            @ToolParam(description = "Summary of the assistant's response") String assistantSummary,
            @ToolParam(description = "Extracted candidate facts, decisions, or key information") List<String> candidateFacts,
            @ToolParam(description = "Project name hints extracted from the conversation") List<String> projectHints,
            @ToolParam(description = "Tags to associate with this checkpoint") List<String> tags,
            @ToolParam(description = "Open questions identified in the conversation") List<String> openQuestions,
            @ToolParam(description = "Explicit user action: REMEMBER, SAVE, DO_NOT_SAVE, or NONE") String explicitAction) {

        ExplicitAction action = parseExplicitAction(explicitAction);

        CheckpointCandidate candidate = new CheckpointCandidate(
                conversationId, sourceClient, turnNumber, null, title,
                userSummary, assistantSummary,
                candidateFacts != null ? candidateFacts : List.of(),
                List.of(),
                projectHints != null ? projectHints : List.of(),
                tags != null ? tags : List.of(),
                openQuestions != null ? openQuestions : List.of(),
                null, action, CandidateScope.CONVERSATION,
                Instant.now(), Map.of());

        return facade.evaluateCheckpoint(candidate);
    }

    @Tool(name = "context_checkpoint_confirm",
          description = "Confirm or decline persistence of a previously evaluated checkpoint. " +
                        "Call this after presenting the evaluation to the user and receiving their decision.")
    public OperationResult confirmCheckpoint(
            @ToolParam(description = "The checkpoint ID returned from context_checkpoint_evaluate") String checkpointId,
            @ToolParam(description = "true to confirm persistence, false to decline") boolean confirmed,
            @ToolParam(description = "Optional project ID to associate the memory with") String optionalProjectId,
            @ToolParam(description = "Optional user-edited title") String title,
            @ToolParam(description = "Optional user-edited summary") String summary) {

        CheckpointConfirmation confirmation = new CheckpointConfirmation(
                UUID.fromString(checkpointId), confirmed,
                optionalProjectId != null ? UUID.fromString(optionalProjectId) : null,
                title, summary, Map.of());

        return facade.confirmCheckpoint(confirmation);
    }

    private ExplicitAction parseExplicitAction(String action) {
        if (action == null || action.isBlank()) return ExplicitAction.NONE;
        try {
            return ExplicitAction.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ExplicitAction.NONE;
        }
    }
}
