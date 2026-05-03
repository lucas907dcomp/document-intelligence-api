package com.example.documentintelligence.service.ai;

public interface EmbeddingProvider {

    /** Embeds the given text into a float vector. Retryable for transient API errors. */
    float[] embed(String text);
}
