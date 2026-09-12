package com.context_mcp.context_mcp.repository;

import com.context_mcp.context_mcp.domain.model.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MemoryRepository extends JpaRepository<MemoryEntity, UUID> {

    @Query("SELECT m FROM MemoryEntity m WHERE m.status = 'ACTIVE' AND m.memoryType = :type ORDER BY m.createdAt DESC")
    List<MemoryEntity> findActiveByType(@Param("type") String type);

    @Query("SELECT m FROM MemoryEntity m WHERE m.status = 'ACTIVE' AND m.sourceConversationId = :conversationId ORDER BY m.createdAt DESC")
    List<MemoryEntity> findByConversationId(@Param("conversationId") String conversationId);

    @Query("SELECT m FROM MemoryEntity m JOIN m.projects p WHERE m.status = 'ACTIVE' AND p.id = :projectId ORDER BY m.createdAt DESC")
    List<MemoryEntity> findActiveByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT m FROM MemoryEntity m WHERE m.status = 'ACTIVE' AND m.memoryType = 'OPEN_QUESTION' ORDER BY m.createdAt DESC")
    List<MemoryEntity> findActiveOpenQuestions();

    @Query("SELECT m FROM MemoryEntity m JOIN m.projects p WHERE m.status = 'ACTIVE' AND m.memoryType = 'OPEN_QUESTION' AND p.id = :projectId ORDER BY m.createdAt DESC")
    List<MemoryEntity> findOpenQuestionsByProject(@Param("projectId") UUID projectId);

    /** Full-text search using PostgreSQL tsvector */
    @Query(value = "SELECT m.* FROM memories m WHERE m.status = 'ACTIVE' AND " +
                   "m.search_vector @@ plainto_tsquery('english', :query) " +
                   "ORDER BY ts_rank(m.search_vector, plainto_tsquery('english', :query)) DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<MemoryEntity> fullTextSearch(@Param("query") String query, @Param("limit") int limit);

    /** Full-text search filtered by project */
    @Query(value = "SELECT m.* FROM memories m " +
                   "JOIN memory_projects mp ON m.id = mp.memory_id " +
                   "WHERE m.status = 'ACTIVE' AND mp.project_id = :projectId AND " +
                   "m.search_vector @@ plainto_tsquery('english', :query) " +
                   "ORDER BY ts_rank(m.search_vector, plainto_tsquery('english', :query)) DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<MemoryEntity> fullTextSearchByProject(@Param("query") String query,
                                                @Param("projectId") UUID projectId,
                                                @Param("limit") int limit);

    /** Find memories with similar embeddings using pgvector cosine distance */
    @Query(value = "SELECT m.* FROM memories m " +
                   "JOIN memory_embeddings me ON m.id = me.memory_id " +
                   "WHERE m.status = 'ACTIVE' " +
                   "ORDER BY me.embedding <=> cast(:embedding AS vector) " +
                   "LIMIT :limit", nativeQuery = true)
    List<MemoryEntity> semanticSearch(@Param("embedding") String embedding, @Param("limit") int limit);

    /** Semantic search filtered by project */
    @Query(value = "SELECT m.* FROM memories m " +
                   "JOIN memory_embeddings me ON m.id = me.memory_id " +
                   "JOIN memory_projects mp ON m.id = mp.memory_id " +
                   "WHERE m.status = 'ACTIVE' AND mp.project_id = :projectId " +
                   "ORDER BY me.embedding <=> cast(:embedding AS vector) " +
                   "LIMIT :limit", nativeQuery = true)
    List<MemoryEntity> semanticSearchByProject(@Param("embedding") String embedding,
                                                @Param("projectId") UUID projectId,
                                                @Param("limit") int limit);

    @Query("SELECT COUNT(m) FROM MemoryEntity m JOIN m.projects p WHERE m.status = 'ACTIVE' AND p.id = :projectId")
    long countActiveByProject(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(m) FROM MemoryEntity m JOIN m.projects p WHERE m.status = 'ACTIVE' AND m.memoryType = 'OPEN_QUESTION' AND p.id = :projectId")
    long countOpenQuestionsByProject(@Param("projectId") UUID projectId);
}
