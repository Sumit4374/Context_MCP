package com.context_mcp.context_mcp.repository;

import com.context_mcp.context_mcp.domain.model.EmbeddingJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJobEntity, UUID> {
    List<EmbeddingJobEntity> findByStatusOrderByCreatedAtAsc(String status);
    List<EmbeddingJobEntity> findByMemoryId(UUID memoryId);
}
