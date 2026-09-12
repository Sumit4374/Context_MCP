package com.context_mcp.context_mcp.infrastructure.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Small streaming helpers used by artifact ingestion; no artifact contents are retained in memory. */
public final class FileHashing {

    private static final int BUFFER_SIZE = 64 * 1024;

    private FileHashing() {
    }

    public static String sha256(Path path) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available in the JRE", exception);
        }
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder output = new StringBuilder(64);
        for (byte value : digest.digest()) {
            output.append(String.format("%02x", value));
        }
        return output.toString();
    }
}
