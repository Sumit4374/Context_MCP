package com.context_mcp.context_mcp.repository;

import com.context_mcp.context_mcp.domain.model.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    Optional<ProjectEntity> findByName(String name);

    @Query("SELECT p FROM ProjectEntity p WHERE p.status <> 'DELETED' ORDER BY p.updatedAt DESC")
    List<ProjectEntity> findAllActive();

    @Query("SELECT p FROM ProjectEntity p WHERE p.status <> 'DELETED' AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.updatedAt DESC")
    List<ProjectEntity> searchByQuery(String query);

    List<ProjectEntity> findByParentProjectId(UUID parentProjectId);
}
