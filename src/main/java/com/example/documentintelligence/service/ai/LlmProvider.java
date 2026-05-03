package com.example.documentintelligence.service.ai;

public interface LlmProvider {

    /** Sends a chat completion request with a system prompt and user message. */
    String complete(String systemPrompt, String userMessage);
}
