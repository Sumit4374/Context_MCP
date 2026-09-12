package com.context_mcp.context_mcp.infrastructure.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative local-only secret detector.  It is intentionally a safety
 * filter, not a credential validator: a false positive is preferable to
 * retaining a likely credential in durable context.
 */
public final class RegexSecretDetector implements SecretDetector {

    private static final List<Rule> RULES = List.of(
            new Rule(SecretType.PRIVATE_KEY,
                    Pattern.compile("-----BEGIN (?:[A-Z0-9 ]+ )?PRIVATE KEY-----[\\s\\S]*?-----END (?:[A-Z0-9 ]+ )?PRIVATE KEY-----")),
            new Rule(SecretType.CONNECTION_STRING,
                    Pattern.compile("(?i)\\b(?:postgres(?:ql)?|mysql|mariadb|mongodb(?:\\+srv)?|redis)://[^\\s/@:]+:[^\\s/@]+@[^\\s/]+(?:/[^\\s]*)?")),
            new Rule(SecretType.BEARER_TOKEN,
                    Pattern.compile("(?i)\\bbearer[ \\t]+[A-Za-z0-9._~+/=-]{12,}")),
            new Rule(SecretType.API_KEY,
                    Pattern.compile("\\b(?:AKIA|ASIA)[A-Z0-9]{16}\\b")),
            new Rule(SecretType.ACCESS_TOKEN,
                    Pattern.compile("\\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|sk-(?:proj-)?[A-Za-z0-9_-]{20,})\\b")),
            new Rule(SecretType.PASSWORD,
                    Pattern.compile("(?i)\\b(?:password|passwd|pwd)[ \\t]*[=:][ \\t]*[^\\s'\"`;,]{4,}")),
            new Rule(SecretType.API_KEY,
                    Pattern.compile("(?i)\\b(?:api[_-]?key|access[_-]?token|auth[_-]?token|client[_-]?secret)[ \\t]*[=:][ \\t]*[^\\s'\"`;,]{8,}")),
            new Rule(SecretType.ENVIRONMENT_VALUE,
                    Pattern.compile("(?m)^[ \\t]*[A-Za-z_][A-Za-z0-9_]*(?:KEY|TOKEN|SECRET|PASSWORD|PASSWD|PWD)[ \\t]*=[ \\t]*[^\\s#]{4,}"))
    );

    @Override
    public List<SecretDetection> detect(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<SecretDetection> detections = new ArrayList<>();
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(content);
            while (matcher.find()) {
                detections.add(new SecretDetection(rule.type(), matcher.start(), matcher.end()));
            }
        }
        detections.sort(Comparator
                .comparingInt(SecretDetection::start)
                .thenComparing(Comparator.comparingInt(SecretDetection::length).reversed())
                .thenComparing(detection -> detection.type().name()));
        return List.copyOf(detections);
    }

    private record Rule(SecretType type, Pattern pattern) {
    }
}
