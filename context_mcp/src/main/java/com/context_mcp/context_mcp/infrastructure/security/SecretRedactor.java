package com.context_mcp.context_mcp.infrastructure.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Replaces secret-shaped values before any classification, embedding, logging,
 * or persistence work.  The returned findings include only type and offsets.
 */
public final class SecretRedactor {

    private final SecretDetector detector;

    public SecretRedactor(SecretDetector detector) {
        this.detector = Objects.requireNonNull(detector, "detector must not be null");
    }

    public RedactionResult redact(String content) {
        if (content == null || content.isEmpty()) {
            return new RedactionResult(content == null ? "" : content, List.of());
        }

        List<SecretDetection> detections = detector.detect(content);
        if (detections.isEmpty()) {
            return new RedactionResult(content, detections);
        }

        StringBuilder result = new StringBuilder(content.length());
        int cursor = 0;
        for (Range range : mergedRanges(detections)) {
            if (range.start() > cursor) {
                result.append(content, cursor, range.start());
            }
            result.append("[REDACTED_").append(range.type().name()).append(']');
            cursor = range.end();
        }
        if (cursor < content.length()) {
            result.append(content, cursor, content.length());
        }
        return new RedactionResult(result.toString(), detections);
    }

    private static List<Range> mergedRanges(List<SecretDetection> detections) {
        List<SecretDetection> ordered = new ArrayList<>(detections);
        ordered.sort(Comparator
                .comparingInt(SecretDetection::start)
                .thenComparing(Comparator.comparingInt(SecretDetection::end).reversed()));

        List<Range> ranges = new ArrayList<>();
        for (SecretDetection detection : ordered) {
            if (ranges.isEmpty()) {
                ranges.add(new Range(detection.start(), detection.end(), detection.type()));
                continue;
            }
            Range previous = ranges.getLast();
            if (detection.start() <= previous.end()) {
                ranges.set(ranges.size() - 1, new Range(
                        previous.start(),
                        Math.max(previous.end(), detection.end()),
                        moreSpecific(previous.type(), detection.type())));
            } else {
                ranges.add(new Range(detection.start(), detection.end(), detection.type()));
            }
        }
        return ranges;
    }

    private static SecretType moreSpecific(SecretType first, SecretType second) {
        return priority(second) > priority(first) ? second : first;
    }

    private static int priority(SecretType type) {
        return switch (type) {
            case PRIVATE_KEY -> 7;
            case CONNECTION_STRING -> 6;
            case BEARER_TOKEN -> 5;
            case ACCESS_TOKEN -> 4;
            case API_KEY -> 3;
            case PASSWORD -> 2;
            case ENVIRONMENT_VALUE -> 1;
        };
    }

    private record Range(int start, int end, SecretType type) {
    }
}
