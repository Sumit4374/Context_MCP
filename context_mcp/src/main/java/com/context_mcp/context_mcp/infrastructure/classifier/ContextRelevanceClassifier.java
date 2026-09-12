package com.context_mcp.context_mcp.infrastructure.classifier;

import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.CheckpointCandidate;

import java.math.BigDecimal;
import java.util.List;

/**
 * Provider-neutral interface for evaluating whether a checkpoint candidate
 * contains durable, reusable context that merits persistence.
 */
public interface ContextRelevanceClassifier {

    ClassificationResult classify(CheckpointCandidate candidate);

    record ClassificationResult(
            String suggestedMemoryType,
            BigDecimal durabilityScore,
            BigDecimal reuseScore,
            BigDecimal projectAffinityScore,
            BigDecimal sensitivityRisk,
            BigDecimal confidence,
            List<String> reasons,
            boolean sensitiveContentDetected,
            String suggestedProject,
            String suggestedSummary) {
        public ClassificationResult {
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
        }
    }
}
