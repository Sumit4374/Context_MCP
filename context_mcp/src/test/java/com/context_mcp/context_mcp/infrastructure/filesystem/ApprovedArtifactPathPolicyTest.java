package com.context_mcp.context_mcp.infrastructure.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApprovedArtifactPathPolicyTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsARegularFileInsideAnApprovedRoot() throws IOException {
        Path root = Files.createDirectory(temporaryDirectory.resolve("approved"));
        Path file = Files.writeString(root.resolve("design.md"), "design");

        var result = new ApprovedArtifactPathPolicy(java.util.List.of(root), java.util.List.of("**/.env")).validate(file);

        assertThat(result.accepted()).isTrue();
        assertThat(result.relativePath()).isEqualTo(Path.of("design.md"));
    }

    @Test
    void rejectsFilesOutsideTheApprovedRoot() throws IOException {
        Path root = Files.createDirectory(temporaryDirectory.resolve("approved"));
        Path file = Files.writeString(temporaryDirectory.resolve("outside.md"), "outside");

        var result = new ApprovedArtifactPathPolicy(java.util.List.of(root), java.util.List.of()).validate(file);

        assertThat(result.accepted()).isFalse();
    }

    @Test
    void rejectsExcludedFiles() throws IOException {
        Path root = Files.createDirectory(temporaryDirectory.resolve("approved"));
        Path file = Files.writeString(root.resolve(".env"), "TOKEN=do-not-index");

        var result = new ApprovedArtifactPathPolicy(java.util.List.of(root), java.util.List.of("**/.env", ".env")).validate(file);

        assertThat(result.accepted()).isFalse();
    }
}
