CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE embeddings (
    id       UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_id UUID           NOT NULL REFERENCES chunks(id) ON DELETE CASCADE,
    vector   VECTOR(1536)   NOT NULL
);

-- IVFFlat index for cosine similarity search (lists=100 appropriate for MVP scale)
CREATE INDEX idx_embeddings_vector_cosine
    ON embeddings
    USING ivfflat (vector vector_cosine_ops)
    WITH (lists = 100);
