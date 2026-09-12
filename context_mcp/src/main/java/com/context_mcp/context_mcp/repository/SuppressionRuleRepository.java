package com.context_mcp.context_mcp.repository;

import com.context_mcp.context_mcp.domain.model.SuppressionRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SuppressionRuleRepository extends JpaRepository<SuppressionRuleEntity, UUID> {

    @Query("SELECT s FROM SuppressionRuleEntity s WHERE s.active = true AND " +
           "(s.conversationId = :conversationId OR s.checkpointId = :checkpointId) AND " +
           "(s.expiresAt IS NULL OR s.expiresAt > :now)")
    List<SuppressionRuleEntity> findActiveSuppression(@Param("conversationId") String conversationId,
                                                       @Param("checkpointId") UUID checkpointId,
                                                       @Param("now") Instant now);

    @Query("SELECT s FROM SuppressionRuleEntity s WHERE s.active = true AND " +
           "s.conversationId = :conversationId AND " +
           "(s.expiresAt IS NULL OR s.expiresAt > :now)")
    List<SuppressionRuleEntity> findActiveByConversation(@Param("conversationId") String conversationId,
                                                          @Param("now") Instant now);
}
