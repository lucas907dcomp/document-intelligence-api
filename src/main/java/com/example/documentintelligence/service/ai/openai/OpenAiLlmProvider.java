package com.example.documentintelligence.service.ai.openai;

import com.example.documentintelligence.service.ai.LlmProvider;
import org.springframework.ai.chat.client.ChatClient;

public class OpenAiLlmProvider implements LlmProvider {

    private final ChatClient chatClient;

    public OpenAiLlmProvider(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String complete(String systemPrompt, String userMessage) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }
}
