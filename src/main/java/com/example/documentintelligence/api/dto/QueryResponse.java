package com.example.documentintelligence.api.dto;

import java.util.List;

public record QueryResponse(String answer, List<String> sourceChunks) {
}
