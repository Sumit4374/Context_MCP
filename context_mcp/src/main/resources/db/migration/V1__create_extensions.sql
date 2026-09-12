-- Enable required PostgreSQL extensions for vector search and trigram similarity
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
