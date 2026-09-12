package com.context_mcp.context_mcp.repository;

import com.context_mcp.context_mcp.domain.model.RelationshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RelationshipRepository extends JpaRepository<RelationshipEntity, UUID> {
    List<RelationshipEntity> findBySourceIdAndSourceType(UUID sourceId, String sourceType);
    List<RelationshipEntity> findByTargetIdAndTargetType(UUID targetId, String targetType);
    List<RelationshipEntity> findBySourceIdOrTargetId(UUID sourceId, UUID targetId);
}
