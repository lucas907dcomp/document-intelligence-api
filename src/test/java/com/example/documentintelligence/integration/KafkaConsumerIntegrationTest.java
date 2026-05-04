package com.example.documentintelligence.integration;

import com.example.documentintelligence.domain.Chunk;
import com.example.documentintelligence.domain.Document;
import com.example.documentintelligence.domain.DocumentStatus;
import com.example.documentintelligence.repository.ChunkRepository;
import com.example.documentintelligence.repository.DocumentRepository;
import com.example.documentintelligence.repository.EmbeddingRepository;
import com.example.documentintelligence.service.ai.EmbeddingProvider;
import com.example.documentintelligence.service.ai.LlmProvider;
import com.example.documentintelligence.service.kafka.DocumentEventProducer;
import com.example.documentintelligence.service.kafka.event.DocumentUploadedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class KafkaConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired DocumentRepository documentRepository;
    @Autowired ChunkRepository chunkRepository;
    @Autowired EmbeddingRepository embeddingRepository;
    @Autowired KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate;

    @MockBean EmbeddingProvider embeddingProvider;
    @MockBean LlmProvider llmProvider;

    @BeforeEach
    void configureMocks() {
        float[] vector = new float[1536];
        Arrays.fill(vector, 0.1f);
        when(embeddingProvider.embed(anyString())).thenReturn(vector);
    }

    @Test
    void consume_publishedEvent_persistsChunksAndEmbeddingsAndSetsReady() throws Exception {
        UUID documentId = UUID.randomUUID();
        documentRepository.save(Document.builder()
                .id(documentId)
                .filename("kafka-consumer-test.pdf")
                .status(DocumentStatus.PROCESSING)
                .build());

        Path pdfPath = Path.of(STORAGE_PATH, documentId.toString(), "kafka-consumer-test.pdf");
        Files.createDirectories(pdfPath.getParent());
        Files.write(pdfPath, DocumentIntegrationTest.buildPdf(
                "Kafka consumer integration test with enough content to produce at least one chunk."));

        DocumentUploadedEvent event = new DocumentUploadedEvent(
                documentId.toString(), pdfPath.toString(), Instant.now());
        kafkaTemplate.send(DocumentEventProducer.TOPIC, documentId.toString(), event);

        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> documentRepository.findById(documentId)
                        .map(d -> d.getStatus() == DocumentStatus.READY)
                        .orElse(false));

        List<Chunk> chunks = chunkRepository.findByDocumentId(documentId);
        assertThat(chunks).isNotEmpty();

        // Verify embeddings exist via similarity search (implicitly confirms all embeddings persisted)
        List<String> similarChunks = embeddingRepository.findTopKSimilarChunks(
                documentId, uniformVectorString(0.1f), chunks.size());
        assertThat(similarChunks).hasSize(chunks.size());

        // Consumer deletes the file after successful processing
        assertThat(pdfPath).doesNotExist();
    }

    private static String uniformVectorString(float value) {
        float[] v = new float[1536];
        Arrays.fill(v, value);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
