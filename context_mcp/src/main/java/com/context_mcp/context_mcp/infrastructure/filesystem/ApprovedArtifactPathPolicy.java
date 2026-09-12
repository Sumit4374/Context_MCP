package com.context_mcp.context_mcp.infrastructure.filesystem;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Resolves artifact paths against an explicit allow-list.  The check is made
 * using real paths, rather than string prefixes, so a symlink cannot escape an
 * approved directory.
 */
public final class ApprovedArtifactPathPolicy {

    private final List<Path> approvedRoots;
    private final List<String> excludedGlobs;
    private final FileSystem fileSystem;

    public ApprovedArtifactPathPolicy(Collection<Path> approvedRoots, Collection<String> excludedGlobs) {
        this(approvedRoots, excludedGlobs, FileSystems.getDefault());
    }

    ApprovedArtifactPathPolicy(Collection<Path> approvedRoots, Collection<String> excludedGlobs, FileSystem fileSystem) {
        this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem");
        this.approvedRoots = approvedRoots == null ? List.of() : approvedRoots.stream()
                .filter(Objects::nonNull)
                .map(path -> path.toAbsolutePath().normalize())
                .toList();
        this.excludedGlobs = excludedGlobs == null ? List.of() : excludedGlobs.stream()
                .filter(glob -> glob != null && !glob.isBlank())
                .toList();
    }

    public ValidationResult validate(Path candidate) {
        if (candidate == null) {
            return ValidationResult.rejected("Artifact path is required");
        }
        if (approvedRoots.isEmpty()) {
            return ValidationResult.rejected("No approved artifact roots are configured");
        }
        if (!Files.isRegularFile(candidate)) {
            return ValidationResult.rejected("Artifact path must identify a regular existing file");
        }

        try {
            Path realCandidate = candidate.toRealPath();
            for (Path configuredRoot : approvedRoots) {
                if (!Files.isDirectory(configuredRoot)) {
                    continue;
                }
                Path realRoot = configuredRoot.toRealPath();
                if (!realCandidate.startsWith(realRoot)) {
                    continue;
                }
                Path relativePath = realRoot.relativize(realCandidate);
                if (isExcluded(relativePath)) {
                    return ValidationResult.rejected("Artifact matches an excluded path pattern");
                }
                return ValidationResult.accepted(realCandidate, relativePath, realRoot);
            }
            return ValidationResult.rejected("Artifact is outside the approved roots");
        } catch (IOException exception) {
            return ValidationResult.rejected("Artifact path could not be resolved safely");
        }
    }

    private boolean isExcluded(Path relativePath) {
        List<Path> candidates = new ArrayList<>();
        candidates.add(relativePath);
        candidates.add(Path.of("/").resolve(relativePath));
        return excludedGlobs.stream()
                .map(glob -> fileSystem.getPathMatcher("glob:" + glob))
                .anyMatch(matcher -> candidates.stream().anyMatch(matcher::matches));
    }

    public record ValidationResult(boolean accepted, Path canonicalPath, Path relativePath, Path approvedRoot, String reason) {
        static ValidationResult accepted(Path canonicalPath, Path relativePath, Path approvedRoot) {
            return new ValidationResult(true, canonicalPath, relativePath, approvedRoot, null);
        }

        static ValidationResult rejected(String reason) {
            return new ValidationResult(false, null, null, null, reason);
        }
    }
}
