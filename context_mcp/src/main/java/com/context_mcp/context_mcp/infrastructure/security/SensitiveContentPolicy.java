package com.context_mcp.context_mcp.infrastructure.security;

/**
 * Local policy applied after secrets have been removed from content.  Encrypted
 * storage is an extension point; callers must still redact before forwarding
 * content to classifiers or embedding providers.
 */
public enum SensitiveContentPolicy {
    REDACT,
    REJECT,
    REQUIRE_CONFIRMATION,
    ENCRYPTED_STORAGE
}
