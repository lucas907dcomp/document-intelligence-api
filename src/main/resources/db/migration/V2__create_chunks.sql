CREATE TABLE chunks (
    id            UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id   UUID    NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    content       TEXT    NOT NULL,
    chunk_index   INTEGER NOT NULL
);

CREATE INDEX idx_chunks_document_id ON chunks(document_id);
