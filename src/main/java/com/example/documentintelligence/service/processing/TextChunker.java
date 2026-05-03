package com.example.documentintelligence.service.processing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TextChunker {

    private final int chunkSize;
    private final int chunkOverlap;

    public TextChunker(
            @Value("${app.rag.chunk-size:1000}") int chunkSize,
            @Value("${app.rag.chunk-overlap:200}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * Splits text into overlapping chunks using a sliding window.
     * step = chunkSize - chunkOverlap ensures semantic continuity across boundaries.
     */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - chunkOverlap;
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + chunkSize, text.length());
            String chunk = text.substring(i, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            i += step;
        }
        return chunks;
    }
}
