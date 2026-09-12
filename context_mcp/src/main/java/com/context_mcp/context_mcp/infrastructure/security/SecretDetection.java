package com.context_mcp.context_mcp.infrastructure.security;

import java.util.Objects;

/**
 * A location-only secret finding.  Keeping offsets instead of the matched
 * value makes it safe to pass this object to logs and audit records.
 */
public record SecretDetection(SecretType type, int start, int end) {

    public SecretDetection {
        Objects.requireNonNull(type, "type must not be null");
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("secret detection range must be non-empty");
        }
    }

    public int length() {
        return end - start;
    }
}
