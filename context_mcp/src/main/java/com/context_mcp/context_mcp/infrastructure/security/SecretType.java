package com.context_mcp.context_mcp.infrastructure.security;

/**
 * Broad classes of credentials that must never be retained in checkpoint or
 * memory content.  The values are deliberately safe to expose in audit data;
 * they never contain the matched value.
 */
public enum SecretType {
    API_KEY,
    ACCESS_TOKEN,
    BEARER_TOKEN,
    PASSWORD,
    PRIVATE_KEY,
    CONNECTION_STRING,
    ENVIRONMENT_VALUE
}
