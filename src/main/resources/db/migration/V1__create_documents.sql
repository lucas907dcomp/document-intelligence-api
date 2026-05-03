CREATE TABLE documents (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    filename       VARCHAR(255) NOT NULL,
    storage_path   VARCHAR(512),
    status         VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    uploaded_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    file_size_bytes BIGINT
);
