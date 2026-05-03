package com.example.documentintelligence.api.dto;

import jakarta.validation.constraints.NotBlank;

public record QueryRequest(@NotBlank String question) {
}
