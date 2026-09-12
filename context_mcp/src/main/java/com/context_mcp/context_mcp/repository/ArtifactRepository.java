package com.context_mcp.context_mcp.repository;

import com.context_mcp.context_mcp.domain.model.ArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtifactRepository extends JpaRepository<ArtifactEntity, UUID> {
    Optional<ArtifactEntity> findByCanonicalPath(String canonicalPath);
    Optional<ArtifactEntity> findBySha256(String sha256);
}
