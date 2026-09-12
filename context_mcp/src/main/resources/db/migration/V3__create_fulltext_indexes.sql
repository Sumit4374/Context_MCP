-- Full-text search support using tsvector generated columns
ALTER TABLE memories ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(summary, '')), 'B')
    ) STORED;

CREATE INDEX idx_memories_fts ON memories USING GIN (search_vector);

-- Trigram index for fuzzy matching on titles
CREATE INDEX idx_memories_title_trgm ON memories USING GIN (title gin_trgm_ops);
