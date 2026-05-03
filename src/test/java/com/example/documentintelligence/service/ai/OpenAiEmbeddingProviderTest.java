package com.example.documentintelligence.service.ai;

import com.example.documentintelligence.service.ai.openai.OpenAiEmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiEmbeddingProviderTest {

    private final EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
    // Instantiate directly — @Retryable AOP is NOT active in plain unit test (by design)
    private final OpenAiEmbeddingProvider provider = new OpenAiEmbeddingProvider(embeddingModel);

    @Test
    void embed_returnsVectorFromModel() {
        float[] expected = {0.1f, 0.2f, 0.3f};
        when(embeddingModel.embed("hello world")).thenReturn(expected);

        float[] result = provider.embed("hello world");

        assertThat(result).isEqualTo(expected);
        verify(embeddingModel).embed("hello world");
    }

    @Test
    void embed_propagatesModelException() {
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("API error"));

        assertThatThrownBy(() -> provider.embed("test input"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API error");
    }
}
