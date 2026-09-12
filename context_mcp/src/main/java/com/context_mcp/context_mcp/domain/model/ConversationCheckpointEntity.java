package com.context_mcp.context_mcp.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_checkpoints")
public class ConversationCheckpointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_conversation_id", nullable = false, length = 512)
    private String externalConversationId;

    @Column(name = "source_client", length = 100)
    private String sourceClient;

    @Column(name = "turn_number")
    private Integer turnNumber;

    @Column(name = "checkpoint_number")
    private Integer checkpointNumber;

    @Column(name = "request_payload", columnDefinition = "JSONB", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String requestPayload = "{}";

    @Column(columnDefinition = "JSONB", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String evaluation = "{}";

    @Column(length = 50)
    private String decision;

    @Column(name = "user_confirmation", length = 50)
    private String userConfirmation;

    @Column(nullable = false)
    private boolean suppressed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getExternalConversationId() { return externalConversationId; }
    public void setExternalConversationId(String externalConversationId) { this.externalConversationId = externalConversationId; }

    public String getSourceClient() { return sourceClient; }
    public void setSourceClient(String sourceClient) { this.sourceClient = sourceClient; }

    public Integer getTurnNumber() { return turnNumber; }
    public void setTurnNumber(Integer turnNumber) { this.turnNumber = turnNumber; }

    public Integer getCheckpointNumber() { return checkpointNumber; }
    public void setCheckpointNumber(Integer checkpointNumber) { this.checkpointNumber = checkpointNumber; }

    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }

    public String getEvaluation() { return evaluation; }
    public void setEvaluation(String evaluation) { this.evaluation = evaluation; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getUserConfirmation() { return userConfirmation; }
    public void setUserConfirmation(String userConfirmation) { this.userConfirmation = userConfirmation; }

    public boolean isSuppressed() { return suppressed; }
    public void setSuppressed(boolean suppressed) { this.suppressed = suppressed; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
}
