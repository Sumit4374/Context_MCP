-- Memory embeddings with pgvector
-- Default dimension is 768; a model change requires a new migration that
-- drops and recreates this table with the new dimension.
CREATE TABLE memory_embeddings (
    memory_id    UUID PRIMARY KEY REFERENCES memories(id) ON DELETE CASCADE,
    embedding    vector(768) NOT NULL,
    model_name   VARCHAR(255),
    content_hash VARCHAR(64),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- HNSW index for fast approximate nearest-neighbor search (cosine distance)
CREATE INDEX idx_memory_embeddings_hnsw ON memory_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
