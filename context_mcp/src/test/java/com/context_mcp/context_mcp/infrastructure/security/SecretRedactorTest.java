package com.context_mcp.context_mcp.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretRedactorTest {

    private final SecretRedactor redactor = new SecretRedactor(new RegexSecretDetector());

    @Test
    void redactsMultipleCredentialShapesWithoutReturningTheirValues() {
        String content = "Deploy with Authorization: Bearer abcdefghijklmnopqrstuvwxyz123456 and "
                + "DATABASE_URL=postgresql://context_user:superSecretPassword@localhost/context";

        RedactionResult result = redactor.redact(content);

        assertThat(result.hadSensitiveContent()).isTrue();
        assertThat(result.content())
                .contains("[REDACTED_BEARER_TOKEN]")
                .contains("[REDACTED_CONNECTION_STRING]")
                .doesNotContain("abcdefghijklmnopqrstuvwxyz123456")
                .doesNotContain("superSecretPassword");
        assertThat(result.detections()).extracting(SecretDetection::type)
                .contains(SecretType.BEARER_TOKEN, SecretType.CONNECTION_STRING);
    }

    @Test
    void redactsPrivateKeyBlocksAsOneSafePlaceholder() {
        String content = "Keep this out of durable context:\n"
                + "-----BEGIN PRIVATE KEY-----\n"
                + "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMockKeyData\n"
                + "-----END PRIVATE KEY-----";

        RedactionResult result = redactor.redact(content);

        assertThat(result.content()).contains("[REDACTED_PRIVATE_KEY]")
                .doesNotContain("MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMockKeyData");
        assertThat(result.detections()).extracting(SecretDetection::type).contains(SecretType.PRIVATE_KEY);
    }

    @Test
    void leavesOrdinaryTechnicalTextUntouched() {
        String content = "Use PostgreSQL with pgvector and store a summary, not the raw transcript.";

        RedactionResult result = redactor.redact(content);

        assertThat(result.content()).isEqualTo(content);
        assertThat(result.detections()).isEmpty();
    }
}
