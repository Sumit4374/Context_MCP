package com.context_mcp.context_mcp.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "artifacts")
public class ArtifactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "canonical_path", nullable = false, columnDefinition = "TEXT")
    private String canonicalPath;

    @Column(name = "relative_path", columnDefinition = "TEXT")
    private String relativePath;

    @Column(length = 64)
    private String sha256;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "artifact_type", length = 100)
    private String artifactType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "generation_metadata", columnDefinition = "JSONB", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String generationMetadata = "{}";

    @Column(nullable = false, length = 50)
    private String sensitivity = "NONE";

    @Column(name = "exists_on_disk", nullable = false)
    private boolean existsOnDisk = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    @Column(name = "last_indexed_at")
    private Instant lastIndexedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        modifiedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = Instant.now();
    }

    // ---- Accessors ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getCanonicalPath() { return canonicalPath; }
    public void setCanonicalPath(String canonicalPath) { this.canonicalPath = canonicalPath; }

    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getArtifactType() { return artifactType; }
    public void setArtifactType(String artifactType) { this.artifactType = artifactType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getGenerationMetadata() { return generationMetadata; }
    public void setGenerationMetadata(String generationMetadata) { this.generationMetadata = generationMetadata; }

    public String getSensitivity() { return sensitivity; }
    public void setSensitivity(String sensitivity) { this.sensitivity = sensitivity; }

    public boolean isExistsOnDisk() { return existsOnDisk; }
    public void setExistsOnDisk(boolean existsOnDisk) { this.existsOnDisk = existsOnDisk; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getModifiedAt() { return modifiedAt; }

    public Instant getLastIndexedAt() { return lastIndexedAt; }
    public void setLastIndexedAt(Instant lastIndexedAt) { this.lastIndexedAt = lastIndexedAt; }
}
