package com.context_mcp.context_mcp.repository;

import com.context_mcp.context_mcp.domain.model.AuditEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
    List<AuditEventEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
    List<AuditEventEntity> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
    List<AuditEventEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
