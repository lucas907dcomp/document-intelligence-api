package com.example.documentintelligence.service.kafka;

import com.example.documentintelligence.domain.Chunk;
import com.example.documentintelligence.domain.DocumentStatus;
import com.example.documentintelligence.repository.ChunkRepository;
import com.example.documentintelligence.repository.DocumentRepository;
import com.example.documentintelligence.repository.EmbeddingRepository;
import com.example.documentintelligence.service.ai.EmbeddingProvider;
import com.example.documentintelligence.service.kafka.event.DocumentUploadedEvent;
import com.example.documentintelligence.service.processing.PdfExtractor;
import com.example.documentintelligence.service.storage.StorageService;
import com.example.documentintelligence.service.processing.TextChunker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class DocumentEventConsumer {

    private final StorageService storageService;
    private final PdfExtractor pdfExtractor;
    private final TextChunker textChunker;
    private final EmbeddingProvider embeddingProvider;
    private final ChunkRepository chunkRepository;
    private final EmbeddingRepository embeddingRepository;
    private final DocumentRepository documentRepository;
    private final TransactionTemplate transactionTemplate;

    public DocumentEventConsumer(
            StorageService storageService,
            PdfExtractor pdfExtractor,
            TextChunker textChunker,
            EmbeddingProvider embeddingProvider,
            ChunkRepository chunkRepository,
            EmbeddingRepository embeddingRepository,
            DocumentRepository documentRepository,
            PlatformTransactionManager transactionManager) {
        this.storageService = storageService;
        this.pdfExtractor = pdfExtractor;
        this.textChunker = textChunker;
        this.embeddingProvider = embeddingProvider;
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.documentRepository = documentRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @KafkaListener(topics = DocumentEventProducer.TOPIC)
    public void consume(DocumentUploadedEvent event, Acknowledgment ack) {
        UUID documentId = UUID.fromString(event.documentId());
        log.info("Received document event documentId={} filePath={}", documentId, event.filePath());

        try {
            byte[] pdfBytes = storageService.retrieve(event.filePath());
            String text = pdfExtractor.extract(pdfBytes);
            List<String> chunkTexts = textChunker.chunk(text);

            // Persist all chunks and their embeddings atomically
            transactionTemplate.execute(status -> {
                var docRef = documentRepository.getReferenceById(documentId);
                for (int i = 0; i < chunkTexts.size(); i++) {
                    String chunkText = chunkTexts.get(i);
                    Chunk chunk = chunkRepository.save(Chunk.builder()
                            .document(docRef)
                            .content(chunkText)
                            .chunkIndex(i)
                            .build());
                    float[] vector = embeddingProvider.embed(chunkText);
                    embeddingRepository.insertEmbedding(chunk.getId(), toVectorString(vector));
                }
                return null;
            });

            documentRepository.updateStatusById(documentId, DocumentStatus.READY);

            try {
                storageService.delete(event.filePath());
            } catch (Exception deleteEx) {
                log.warn("Could not delete processed file filePath={}: {}",
                        event.filePath(), deleteEx.getMessage());
            }

            ack.acknowledge();
            log.info("Document processed successfully documentId={} chunks={}", documentId, chunkTexts.size());

        } catch (Exception e) {
            log.error("Processing failed documentId={} filePath={} error={} message={}",
                    documentId, event.filePath(), e.getClass().getSimpleName(), e.getMessage());
            documentRepository.updateStatusById(documentId, DocumentStatus.FAILED);
            // Do NOT acknowledge — DefaultErrorHandler will retry then route to DLQ
        }
    }

    private static String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
