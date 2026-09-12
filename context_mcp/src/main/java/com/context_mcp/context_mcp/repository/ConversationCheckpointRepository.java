package com.context_mcp.context_mcp.repository;

import com.context_mcp.context_mcp.domain.model.ConversationCheckpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationCheckpointRepository extends JpaRepository<ConversationCheckpointEntity, UUID> {
    List<ConversationCheckpointEntity> findByExternalConversationIdOrderByCheckpointNumberDesc(String externalConversationId);
    long countByExternalConversationId(String externalConversationId);
}
