package com.context_mcp.context_mcp.infrastructure.classifier;

import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.CheckpointCandidate;
import com.context_mcp.context_mcp.application.facade.ContextFacadeTypes.ExplicitAction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Deterministic, rules-only relevance classifier.  No remote calls.
 * Uses keyword scoring, structural pattern detection, and explicit command
 * recognition to categorise checkpoint candidates.
 */
@Component
public class RulesBasedClassifier implements ContextRelevanceClassifier {

    // ---- keyword → category mappings, ranked by specificity ----

    private static final Map<String, String> CATEGORY_KEYWORDS = Map.ofEntries(
        Map.entry("architecture", "ARCHITECTURE_DECISION"),
        Map.entry("design decision", "ARCHITECTURE_DECISION"),
        Map.entry("tech stack", "ARCHITECTURE_DECISION"),
        Map.entry("database design", "ARCHITECTURE_DECISION"),
        Map.entry("api design", "ARCHITECTURE_DECISION"),
        Map.entry("implementation", "IMPLEMENTATION_PLAN"),
        Map.entry("implementation plan", "IMPLEMENTATION_PLAN"),
        Map.entry("sprint plan", "IMPLEMENTATION_PLAN"),
        Map.entry("debug", "DEBUGGING_KNOWLEDGE"),
        Map.entry("bug", "DEBUGGING_KNOWLEDGE"),
        Map.entry("stack trace", "DEBUGGING_KNOWLEDGE"),
        Map.entry("error handling", "DEBUGGING_KNOWLEDGE"),
        Map.entry("troubleshoot", "DEBUGGING_KNOWLEDGE"),
        Map.entry("prd", "DOCUMENT_WORK"),
        Map.entry("specification", "DOCUMENT_WORK"),
        Map.entry("proposal", "DOCUMENT_WORK"),
        Map.entry("report", "DOCUMENT_WORK"),
        Map.entry("resume", "DOCUMENT_WORK"),
        Map.entry("documentation", "DOCUMENT_WORK"),
        Map.entry("image prompt", "IMAGE_GENERATION"),
        Map.entry("generate image", "IMAGE_GENERATION"),
        Map.entry("dall-e", "IMAGE_GENERATION"),
        Map.entry("midjourney", "IMAGE_GENERATION"),
        Map.entry("stable diffusion", "IMAGE_GENERATION"),
        Map.entry("video script", "VIDEO_GENERATION"),
        Map.entry("storyboard", "VIDEO_GENERATION"),
        Map.entry("video generation", "VIDEO_GENERATION"),
        Map.entry("social post", "AI_CONTENT_CREATION"),
        Map.entry("blog post", "AI_CONTENT_CREATION"),
        Map.entry("content calendar", "AI_CONTENT_CREATION"),
        Map.entry("campaign", "AI_CONTENT_CREATION"),
        Map.entry("brand guide", "AI_CONTENT_CREATION"),
        Map.entry("research", "RESEARCH"),
        Map.entry("findings", "RESEARCH"),
        Map.entry("literature review", "RESEARCH"),
        Map.entry("requirement", "REQUIREMENT"),
        Map.entry("user story", "REQUIREMENT"),
        Map.entry("acceptance criteria", "REQUIREMENT"),
        Map.entry("roadmap", "ROADMAP"),
        Map.entry("milestone", "ROADMAP"),
        Map.entry("product idea", "PRODUCT_IDEA"),
        Map.entry("feature idea", "PRODUCT_IDEA"),
        Map.entry("preference", "PREFERENCE"),
        Map.entry("open question", "OPEN_QUESTION"),
        Map.entry("to be decided", "OPEN_QUESTION"),
        Map.entry("tbd", "OPEN_QUESTION"),
        Map.entry("todo", "OPEN_QUESTION")
    );

    private static final Pattern CODE_BLOCK = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern FILE_PATH = Pattern.compile("(?:/|\\w:\\\\)[\\w/\\\\.-]+\\.\\w{1,10}");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+");
    private static final Pattern SCHEMA_PATTERN = Pattern.compile("(?i)\\b(?:CREATE TABLE|ALTER TABLE|schema|migration|entity|column|foreign key)\\b");

    // Terms that signal the content is transient / casual
    private static final Pattern TRANSIENT_SIGNALS = Pattern.compile(
            "(?i)\\b(?:what time|weather|joke|hello|hi there|thanks|thank you|good morning|good night|how are you)\\b"
    );

    @Override
    public ClassificationResult classify(CheckpointCandidate candidate) {
        String combined = combinedText(candidate);
        String lower = combined.toLowerCase(Locale.ROOT);

        List<String> reasons = new ArrayList<>();
        BigDecimal durability = BigDecimal.ZERO;
        BigDecimal reuse = BigDecimal.ZERO;
        BigDecimal projectAffinity = BigDecimal.ZERO;
        String suggestedType = "GENERAL_NOTE";
        String suggestedProject = null;

        // 1. Explicit action always wins
        if (candidate.explicitAction() == ExplicitAction.REMEMBER ||
            candidate.explicitAction() == ExplicitAction.SAVE) {
            reasons.add("Explicit user save/remember command detected");
            return new ClassificationResult(
                    detectBestType(lower, reasons),
                    new BigDecimal("1.0"), new BigDecimal("1.0"), BigDecimal.ZERO,
                    BigDecimal.ZERO, new BigDecimal("1.0"), reasons,
                    false, firstProjectHint(candidate), candidate.userSummary());
        }

        // 2. Transient detection
        if (TRANSIENT_SIGNALS.matcher(lower).find() && !hasStructuralContent(lower)) {
            reasons.add("Appears to be casual/transient conversation");
            return new ClassificationResult("TRANSIENT",
                    new BigDecimal("0.1"), new BigDecimal("0.1"), BigDecimal.ZERO,
                    BigDecimal.ZERO, new BigDecimal("0.85"), reasons,
                    false, null, null);
        }

        // 3. Category keyword scoring
        String detectedType = detectBestType(lower, reasons);
        if (!detectedType.equals("GENERAL_NOTE")) {
            suggestedType = detectedType;
            durability = durability.add(new BigDecimal("0.4"));
            reuse = reuse.add(new BigDecimal("0.3"));
        }

        // 4. Structural content scoring
        if (CODE_BLOCK.matcher(combined).find()) {
            durability = durability.add(new BigDecimal("0.2"));
            reasons.add("Contains code blocks");
        }
        if (FILE_PATH.matcher(combined).find()) {
            durability = durability.add(new BigDecimal("0.1"));
            reasons.add("Contains file paths");
        }
        if (URL_PATTERN.matcher(combined).find()) {
            reuse = reuse.add(new BigDecimal("0.1"));
            reasons.add("Contains URLs/references");
        }
        if (SCHEMA_PATTERN.matcher(combined).find()) {
            durability = durability.add(new BigDecimal("0.2"));
            reasons.add("Contains schema/database design patterns");
            if (suggestedType.equals("GENERAL_NOTE")) {
                suggestedType = "TECHNICAL_PROJECT";
            }
        }

        // 5. Candidate facts and artifacts boost
        if (!candidate.candidateFacts().isEmpty()) {
            durability = durability.add(new BigDecimal("0.15"));
            reasons.add("Contains " + candidate.candidateFacts().size() + " candidate facts");
        }
        if (!candidate.candidateArtifacts().isEmpty()) {
            durability = durability.add(new BigDecimal("0.15"));
            reasons.add("References " + candidate.candidateArtifacts().size() + " artifacts");
        }
        if (!candidate.openQuestions().isEmpty()) {
            reuse = reuse.add(new BigDecimal("0.15"));
            reasons.add("Contains " + candidate.openQuestions().size() + " open questions");
        }

        // 6. Project hints
        if (!candidate.projectHints().isEmpty()) {
            projectAffinity = new BigDecimal("0.6");
            suggestedProject = candidate.projectHints().getFirst();
            reasons.add("References project: " + suggestedProject);
        }

        // Cap scores at 1.0
        durability = durability.min(BigDecimal.ONE);
        reuse = reuse.min(BigDecimal.ONE);

        // Overall confidence
        BigDecimal confidence = durability.add(reuse).divide(new BigDecimal("2"), 4, java.math.RoundingMode.HALF_UP);

        String summary = candidate.assistantSummary() != null ? candidate.assistantSummary() : candidate.userSummary();

        return new ClassificationResult(suggestedType, durability, reuse, projectAffinity,
                BigDecimal.ZERO, confidence, reasons, false, suggestedProject, summary);
    }

    private String detectBestType(String lowerText, List<String> reasons) {
        String bestType = "GENERAL_NOTE";
        int bestScore = 0;

        for (var entry : CATEGORY_KEYWORDS.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                int score = entry.getKey().length(); // longer match = more specific
                if (score > bestScore) {
                    bestScore = score;
                    bestType = entry.getValue();
                }
            }
        }
        if (!bestType.equals("GENERAL_NOTE")) {
            reasons.add("Matched category keyword for " + bestType);
        }
        return bestType;
    }

    private boolean hasStructuralContent(String lower) {
        return CODE_BLOCK.matcher(lower).find() ||
               SCHEMA_PATTERN.matcher(lower).find() ||
               FILE_PATH.matcher(lower).find();
    }

    private String combinedText(CheckpointCandidate c) {
        StringBuilder sb = new StringBuilder();
        if (c.title() != null) sb.append(c.title()).append(" ");
        if (c.userSummary() != null) sb.append(c.userSummary()).append(" ");
        if (c.assistantSummary() != null) sb.append(c.assistantSummary()).append(" ");
        c.candidateFacts().forEach(f -> sb.append(f).append(" "));
        c.openQuestions().forEach(q -> sb.append(q).append(" "));
        return sb.toString();
    }

    private String firstProjectHint(CheckpointCandidate c) {
        return c.projectHints().isEmpty() ? null : c.projectHints().getFirst();
    }
}
