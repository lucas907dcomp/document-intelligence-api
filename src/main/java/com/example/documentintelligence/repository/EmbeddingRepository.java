package com.example.documentintelligence.repository;

import com.example.documentintelligence.domain.Embedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmbeddingRepository extends JpaRepository<Embedding, UUID> {

    /**
     * Returns the top-K most similar chunk contents to the query vector
     * using pgvector cosine distance operator (<=>) on the embeddings table.
     */
    @Query(value = """
            SELECT c.content
            FROM chunks c
            JOIN embeddings e ON e.chunk_id = c.id
            WHERE c.document_id = :docId
            ORDER BY e.vector <=> CAST(:queryVector AS vector)
            LIMIT :k
            """, nativeQuery = true)
    List<String> findTopKSimilarChunks(
            @Param("docId") UUID docId,
            @Param("queryVector") String queryVector,
            @Param("k") int k
    );
}
