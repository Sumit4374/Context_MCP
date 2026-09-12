package com.context_mcp.context_mcp.repository;

import com.context_mcp.context_mcp.domain.model.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
    Optional<ConversationEntity> findByExternalConversationIdAndSourceClient(String externalConversationId, String sourceClient);
    Optional<ConversationEntity> findByExternalConversationId(String externalConversationId);
}
