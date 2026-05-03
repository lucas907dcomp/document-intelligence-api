package com.example.documentintelligence.service.ai.openai;

import com.example.documentintelligence.service.ai.EmbeddingProvider;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;

    public OpenAiEmbeddingProvider(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }
}
