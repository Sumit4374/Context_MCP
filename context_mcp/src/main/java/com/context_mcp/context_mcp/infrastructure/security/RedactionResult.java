package com.context_mcp.context_mcp.infrastructure.security;

import java.util.List;
import java.util.Objects;

public record RedactionResult(String content, List<SecretDetection> detections) {

    public RedactionResult {
        Objects.requireNonNull(content, "content must not be null");
        detections = List.copyOf(Objects.requireNonNull(detections, "detections must not be null"));
    }

    public boolean hadSensitiveContent() {
        return !detections.isEmpty();
    }
}
