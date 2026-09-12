package com.context_mcp.context_mcp.infrastructure.embedding;

import java.util.List;

/**
 * Provider-neutral embedding interface.  The application must not fail
 * if embeddings are unavailable — keyword/metadata search must still work.
 */
public interface EmbeddingProvider {

    EmbeddingResult embed(EmbeddingRequest request);

    boolean isAvailable();

    int dimensions();

    record EmbeddingRequest(String content, String model) {}

    record EmbeddingResult(List<Float> embedding, String model, boolean success, String error) {
        public static EmbeddingResult empty() {
            return new EmbeddingResult(List.of(), "", false, "No embedding provider configured");
        }
    }
}
