package com.example.documentintelligence.api.dto;

import java.util.UUID;

public record StatusResponse(UUID documentId, String status, String filename) {
}
