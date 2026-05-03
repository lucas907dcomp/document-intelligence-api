package com.example.documentintelligence.service.kafka.event;

import java.time.Instant;

public record DocumentUploadedEvent(String documentId, String filePath, Instant uploadedAt) {
}
