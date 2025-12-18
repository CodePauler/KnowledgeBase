-- Enable pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Single table to store chunks + embeddings (Spring AI PgVectorStore compatible)
CREATE TABLE IF NOT EXISTS document_chunk (
    id           BIGSERIAL PRIMARY KEY,
    content      TEXT NOT NULL,
    metadata     JSONB,
    embedding    vector(1024), -- adjust dimension to your embedding model
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_chunk_metadata ON document_chunk USING gin (metadata);
