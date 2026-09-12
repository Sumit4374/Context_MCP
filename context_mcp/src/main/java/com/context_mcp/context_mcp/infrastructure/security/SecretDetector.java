package com.context_mcp.context_mcp.infrastructure.security;

import java.util.List;

public interface SecretDetector {

    /**
     * Detect secret-shaped material without returning the material itself.
     */
    List<SecretDetection> detect(String content);
}
