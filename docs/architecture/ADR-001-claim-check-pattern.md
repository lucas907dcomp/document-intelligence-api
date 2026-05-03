# ADR-001 — PDF Storage Strategy: Claim Check Pattern

| Field | Value |
|-------|-------|
| **Status** | Accepted |
| **Date** | 2026-05-02 |
| **Deciders** | @architect (Aria) |
| **Story** | DIS-001 |
| **Resolves** | OQ-1 |

---

## Context

The Document Intelligence Service receives PDF uploads (max 10MB) and must process them asynchronously via Apache Kafka. The core question was: **how should the PDF payload be transported from the upload API to the Kafka consumer?**

Three options were evaluated:

| Option | Description | Problem |
|--------|-------------|---------|
| **A** — Kafka payload | Embed PDF bytes directly in the Kafka event | Kafka default max message size is 1MB; pushing to 10MB requires broker reconfiguration, increases broker memory pressure, and couples message bus to payload size |
| **B** — Database BYTEA | Store PDF in PostgreSQL as BYTEA, Kafka carries only documentId | Database becomes a blob store — wrong tool; adds I/O load to the transactional DB and couples processing to DB availability |
| **C** — Shared volume (Claim Check) | Store PDF on a shared Docker volume, Kafka carries documentId + filePath | Decouples broker from payload size; file system is the right tool for binary blobs |

---

## Decision

**Option C — Claim Check Pattern via shared Docker named volume.**

The upload API writes the PDF to a shared volume at a deterministic path: `{PDF_STORAGE_PATH}/{documentId}/{filename}`. The Kafka producer publishes a lightweight `DocumentUploadedEvent` containing only `{ documentId, filePath }`. The Kafka consumer reads the `filePath` from the event, accesses the file on disk, processes it, and **deletes the file after successful processing**.

### Event Schema

```json
{
  "eventType": "DocumentUploaded",
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filePath": "/app/pdf-storage/550e8400.../report.pdf",
  "uploadedAt": "2026-05-02T14:30:00Z"
}
```

### docker-compose Volume Configuration

```yaml
volumes:
  pdf-storage:
    driver: local

services:
  app:
    volumes:
      - pdf-storage:/app/pdf-storage

  # If consumer is a separate service:
  consumer:
    volumes:
      - pdf-storage:/app/pdf-storage
```

### File Path Convention

```
{PDF_STORAGE_PATH}/{documentId}/{originalFilename}

Example:
/app/pdf-storage/550e8400-e29b-41d4-a716-446655440000/quarterly-report.pdf
```

Using `documentId` as a subdirectory prevents filename collisions and enables per-document cleanup.

---

## Consequences

### Positive

- **Kafka broker stability:** No oversized messages. Broker operates with default configuration.
- **Decoupled concerns:** Message bus carries coordination data; file system carries binary data. Each tool used appropriately.
- **Simple implementation:** Standard file I/O in Spring Boot. No additional infrastructure.
- **Volume cleanup:** Consumer deletes the file post-processing, preventing unbounded disk growth.
- **Failure inspection:** On consumer failure, the file is **not deleted**, enabling manual PDF inspection alongside the DLQ event.

### Negative / Limitations

- **Single-host constraint:** The shared volume approach ties consumer horizontal scaling to a single Docker host. Multiple consumer instances on different machines cannot access the same named volume.
- **Mitigation path:** For multi-host scale-out, the volume is replaced with an object store (MinIO locally, AWS S3 in production). The consumer code only changes the storage read/delete calls — the Claim Check Pattern and event schema remain identical.
- **MVP scope:** Single-host docker-compose deployment is explicitly within MVP scope (NFR-4, CON-7). This limitation is documented and the migration path is clear.

---

## New Constraints Introduced

| ID | Description |
|----|-------------|
| CON-7 | Both API and consumer containers must mount the same Docker named volume |
| CON-7 | `PDF_STORAGE_PATH` must be externalized as an environment variable — never hardcoded |

---

## New Edge Cases Introduced

| ID | Scenario | Handling |
|----|----------|----------|
| EC-7 | File missing from volume when consumer processes event | Consumer catches `FileNotFoundException` → Document status FAILED → DLQ |

---

## Resolved Open Questions

| OQ | Resolution |
|----|------------|
| OQ-1 | Claim Check Pattern — shared volume + lightweight Kafka event |
| OQ-2 | 1000 char chunks, 200 char overlap — configurable via `app.rag.chunkSize` / `app.rag.chunkOverlap` |
| OQ-3 | K=5 top-K similarity results — configurable via `app.rag.topK` |

---

## Future Migration Path (Post-MVP)

Replace shared volume with MinIO (local) or S3 (cloud):

```
Current:  FileSystemStorageService implements StorageService
Future:   S3StorageService implements StorageService
```

The `StorageService` abstraction should be introduced from day one so the consumer code is storage-provider-agnostic. This mirrors the Strategy Pattern already applied to LLM and embedding providers.
