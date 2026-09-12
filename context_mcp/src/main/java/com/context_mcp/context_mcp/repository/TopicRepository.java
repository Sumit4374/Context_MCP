package com.context_mcp.context_mcp.repository;

import com.context_mcp.context_mcp.domain.model.TopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<TopicEntity, UUID> {
    Optional<TopicEntity> findByName(String name);
}
