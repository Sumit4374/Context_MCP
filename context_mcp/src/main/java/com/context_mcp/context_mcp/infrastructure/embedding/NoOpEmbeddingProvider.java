package com.context_mcp.context_mcp.infrastructure.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


/**
 * No-op embedding provider used when no model provider is configured.
 * Keyword/metadata search must still work without embeddings.
 */
@Component
@ConditionalOnProperty(name = "context-platform.embeddings.provider", havingValue = "none", matchIfMissing = true)
public class NoOpEmbeddingProvider implements EmbeddingProvider {

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        return EmbeddingResult.empty();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public int dimensions() {
        return 768;
    }
}
