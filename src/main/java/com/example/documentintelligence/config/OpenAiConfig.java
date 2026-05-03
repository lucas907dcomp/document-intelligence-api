package com.example.documentintelligence.config;

import com.example.documentintelligence.service.ai.EmbeddingProvider;
import com.example.documentintelligence.service.ai.LlmProvider;
import com.example.documentintelligence.service.ai.openai.OpenAiEmbeddingProvider;
import com.example.documentintelligence.service.ai.openai.OpenAiLlmProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Bean
    public EmbeddingProvider embeddingProvider(EmbeddingModel embeddingModel) {
        return new OpenAiEmbeddingProvider(embeddingModel);
    }

    @Bean
    public LlmProvider llmProvider(ChatClient.Builder chatClientBuilder) {
        return new OpenAiLlmProvider(chatClientBuilder);
    }
}
