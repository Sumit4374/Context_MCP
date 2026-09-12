package com.context_mcp.context_mcp.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The core durable context record. Each memory represents a piece of valuable
 * context extracted from conversations, explicit user saves, or imports.
 */
@Entity
@Table(name = "memories")
public class MemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "JSONB", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String content = "{}";

    @Column(name = "memory_type", nullable = false, length = 50)
    private String memoryType;

    @Column(nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal importance = new BigDecimal("0.5000");

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence = new BigDecimal("0.5000");

    @Column(nullable = false, length = 50)
    private String sensitivity = "NONE";

    @Column(nullable = false, length = 50)
    private String explicitness = "CONFIRMED_RECOMMENDATION";

    @Column(name = "source_client", length = 100)
    private String sourceClient;

    @Column(name = "source_conversation_id", length = 512)
    private String sourceConversationId;

    @Column(name = "source_checkpoint_id")
    private UUID sourceCheckpointId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_retrieved_at")
    private Instant lastRetrievedAt;

    @Column(name = "reinforcement_count", nullable = false)
    private int reinforcementCount = 0;

    @Version
    @Column(nullable = false)
    private long version = 0;

    // ---- Relationships ----

    @ManyToMany
    @JoinTable(name = "memory_projects",
            joinColumns = @JoinColumn(name = "memory_id"),
            inverseJoinColumns = @JoinColumn(name = "project_id"))
    private Set<ProjectEntity> projects = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "memory_topics",
            joinColumns = @JoinColumn(name = "memory_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"))
    private Set<TopicEntity> topics = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "memory_tags",
            joinColumns = @JoinColumn(name = "memory_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<TagEntity> tags = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "memory_artifacts",
            joinColumns = @JoinColumn(name = "memory_id"),
            inverseJoinColumns = @JoinColumn(name = "artifact_id"))
    private Set<ArtifactEntity> artifacts = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // ---- Accessors ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMemoryType() { return memoryType; }
    public void setMemoryType(String memoryType) { this.memoryType = memoryType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getImportance() { return importance; }
    public void setImportance(BigDecimal importance) { this.importance = importance; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public String getSensitivity() { return sensitivity; }
    public void setSensitivity(String sensitivity) { this.sensitivity = sensitivity; }

    public String getExplicitness() { return explicitness; }
    public void setExplicitness(String explicitness) { this.explicitness = explicitness; }

    public String getSourceClient() { return sourceClient; }
    public void setSourceClient(String sourceClient) { this.sourceClient = sourceClient; }

    public String getSourceConversationId() { return sourceConversationId; }
    public void setSourceConversationId(String sourceConversationId) { this.sourceConversationId = sourceConversationId; }

    public UUID getSourceCheckpointId() { return sourceCheckpointId; }
    public void setSourceCheckpointId(UUID sourceCheckpointId) { this.sourceCheckpointId = sourceCheckpointId; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getLastRetrievedAt() { return lastRetrievedAt; }
    public void setLastRetrievedAt(Instant lastRetrievedAt) { this.lastRetrievedAt = lastRetrievedAt; }

    public int getReinforcementCount() { return reinforcementCount; }
    public void setReinforcementCount(int reinforcementCount) { this.reinforcementCount = reinforcementCount; }

    public long getVersion() { return version; }

    public Set<ProjectEntity> getProjects() { return projects; }
    public void setProjects(Set<ProjectEntity> projects) { this.projects = projects; }

    public Set<TopicEntity> getTopics() { return topics; }
    public void setTopics(Set<TopicEntity> topics) { this.topics = topics; }

    public Set<TagEntity> getTags() { return tags; }
    public void setTags(Set<TagEntity> tags) { this.tags = tags; }

    public Set<ArtifactEntity> getArtifacts() { return artifacts; }
    public void setArtifacts(Set<ArtifactEntity> artifacts) { this.artifacts = artifacts; }

    /** Record a retrieval event for reinforcement tracking. */
    public void recordRetrieval() {
        this.lastRetrievedAt = Instant.now();
        this.reinforcementCount++;
    }
}
