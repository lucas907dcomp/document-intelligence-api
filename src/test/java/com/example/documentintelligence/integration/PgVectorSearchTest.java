package com.example.documentintelligence.integration;

import com.example.documentintelligence.domain.Chunk;
import com.example.documentintelligence.domain.Document;
import com.example.documentintelligence.domain.DocumentStatus;
import com.example.documentintelligence.domain.Embedding;
import com.example.documentintelligence.repository.ChunkRepository;
import com.example.documentintelligence.repository.DocumentRepository;
import com.example.documentintelligence.repository.EmbeddingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PgVectorSearchTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg15");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired DocumentRepository documentRepository;
    @Autowired ChunkRepository chunkRepository;
    @Autowired EmbeddingRepository embeddingRepository;

    @Test
    void findTopKSimilarChunks_returnsChunksOrderedByCosineSimilarity() {
        Document doc = documentRepository.save(Document.builder()
                .filename("pgvector-order-test.pdf")
                .status(DocumentStatus.READY)
                .build());

        // Distance 0.0: identical to query vector [1, 0, 0, ...]
        saveChunkWithEmbedding(doc, "most similar", 0, basisVector(0));
        // Distance ≈ 0.293: 45° from query (equal components on dims 0 and 1)
        saveChunkWithEmbedding(doc, "second similar", 1, mixedVector());
        // Distance 1.0: orthogonal to query
        saveChunkWithEmbedding(doc, "third similar", 2, basisVector(1));
        saveChunkWithEmbedding(doc, "fourth similar", 3, basisVector(2));

        List<String> result = embeddingRepository.findTopKSimilarChunks(
                doc.getId(), vectorString(basisVector(0)), 4);

        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isEqualTo("most similar");
        assertThat(result.get(1)).isEqualTo("second similar");
        assertThat(result.subList(2, 4))
                .containsExactlyInAnyOrder("third similar", "fourth similar");
    }

    @Test
    void findTopKSimilarChunks_respectsKLimit() {
        Document doc = documentRepository.save(Document.builder()
                .filename("pgvector-limit-test.pdf")
                .status(DocumentStatus.READY)
                .build());

        for (int i = 0; i < 5; i++) {
            saveChunkWithEmbedding(doc, "chunk-" + i, i, basisVector(i));
        }

        List<String> result = embeddingRepository.findTopKSimilarChunks(
                doc.getId(), vectorString(basisVector(0)), 2);

        assertThat(result).hasSize(2);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void saveChunkWithEmbedding(Document doc, String content, int index, float[] vector) {
        Chunk chunk = chunkRepository.save(
                Chunk.builder().document(doc).content(content).chunkIndex(index).build());
        embeddingRepository.save(Embedding.builder().chunk(chunk).vector(vector).build());
    }

    private static float[] basisVector(int dimension) {
        float[] v = new float[1536];
        v[dimension] = 1.0f;
        return v;
    }

    private static float[] mixedVector() {
        // Unit vector at 45° in dims 0+1: cosine distance from e0 ≈ 0.293
        float[] v = new float[1536];
        v[0] = 0.7071068f;
        v[1] = 0.7071068f;
        return v;
    }

    private static String vectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
