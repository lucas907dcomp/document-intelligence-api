package com.example.documentintelligence.service.kafka;

import com.example.documentintelligence.domain.Chunk;
import com.example.documentintelligence.domain.Document;
import com.example.documentintelligence.domain.DocumentStatus;
import com.example.documentintelligence.repository.ChunkRepository;
import com.example.documentintelligence.repository.DocumentRepository;
import com.example.documentintelligence.repository.EmbeddingRepository;
import com.example.documentintelligence.service.ai.EmbeddingProvider;
import com.example.documentintelligence.service.kafka.event.DocumentUploadedEvent;
import com.example.documentintelligence.service.processing.PdfExtractionException;
import com.example.documentintelligence.service.processing.PdfExtractor;
import com.example.documentintelligence.service.processing.TextChunker;
import com.example.documentintelligence.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentEventConsumerTest {

    @Mock StorageService storageService;
    @Mock PdfExtractor pdfExtractor;
    @Mock TextChunker textChunker;
    @Mock EmbeddingProvider embeddingProvider;
    @Mock ChunkRepository chunkRepository;
    @Mock EmbeddingRepository embeddingRepository;
    @Mock DocumentRepository documentRepository;
    @Mock PlatformTransactionManager txManager;
    @Mock Acknowledgment ack;

    DocumentEventConsumer consumer;
    final UUID documentId = UUID.randomUUID();
    final String filePath = "/tmp/" + documentId + "/file.pdf";
    final byte[] pdfBytes = new byte[]{1, 2, 3};
    DocumentUploadedEvent event;

    @BeforeEach
    void setUp() {
        consumer = new DocumentEventConsumer(
                storageService, pdfExtractor, textChunker,
                embeddingProvider, chunkRepository, embeddingRepository,
                documentRepository, txManager);

        event = new DocumentUploadedEvent(documentId.toString(), filePath, Instant.now());
    }

    /** Stubs txManager so TransactionTemplate executes the callback without a real DB. */
    private void stubTransactionManager() {
        TransactionStatus txStatus = mock(TransactionStatus.class);
        when(txManager.getTransaction(any())).thenReturn(txStatus);
    }

    @Test
    void consume_happyPath_setsReadyAcksAndDeletesFile() throws Exception {
        stubTransactionManager();
        // Arrange
        when(storageService.retrieve(filePath)).thenReturn(pdfBytes);
        when(pdfExtractor.extract(pdfBytes)).thenReturn("some extracted text");
        when(textChunker.chunk("some extracted text")).thenReturn(List.of("chunk1", "chunk2"));
        Document docRef = Document.builder().id(documentId).build();
        when(documentRepository.getReferenceById(documentId)).thenReturn(docRef);
        Chunk savedChunk = Chunk.builder().id(UUID.randomUUID()).build();
        when(chunkRepository.save(any(Chunk.class))).thenReturn(savedChunk);
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});

        // Act
        consumer.consume(event, ack);

        // Assert
        verify(documentRepository).updateStatusById(documentId, DocumentStatus.READY);
        verify(storageService).delete(filePath);
        verify(ack).acknowledge();
        verify(documentRepository, never()).updateStatusById(documentId, DocumentStatus.FAILED);
    }

    @Test
    void consume_pdfExtractionFails_setsFailedNoDeleteNoAck() throws Exception {
        when(storageService.retrieve(filePath)).thenReturn(pdfBytes);
        when(pdfExtractor.extract(pdfBytes))
                .thenThrow(new PdfExtractionException("image-only PDF"));

        consumer.consume(event, ack);

        verify(documentRepository).updateStatusById(documentId, DocumentStatus.FAILED);
        verify(storageService, never()).delete(anyString());
        verify(ack, never()).acknowledge();
    }

    @Test
    void consume_storageRetrieveFails_setsFailedNoAck() throws Exception {
        when(storageService.retrieve(filePath))
                .thenThrow(new FileNotFoundException("not found"));

        consumer.consume(event, ack);

        verify(documentRepository).updateStatusById(documentId, DocumentStatus.FAILED);
        verify(ack, never()).acknowledge();
    }

    @Test
    void consume_embeddingFailsMidLoop_setsFailedNoAck() throws Exception {
        stubTransactionManager();
        when(storageService.retrieve(filePath)).thenReturn(pdfBytes);
        when(pdfExtractor.extract(pdfBytes)).thenReturn("text");
        when(textChunker.chunk("text")).thenReturn(List.of("chunk1", "chunk2"));
        Document docRef = Document.builder().id(documentId).build();
        when(documentRepository.getReferenceById(documentId)).thenReturn(docRef);
        Chunk savedChunk = Chunk.builder().id(UUID.randomUUID()).build();
        when(chunkRepository.save(any(Chunk.class))).thenReturn(savedChunk);
        // First embed succeeds, second fails
        when(embeddingProvider.embed("chunk1")).thenReturn(new float[]{0.1f});
        when(embeddingProvider.embed("chunk2")).thenThrow(new RuntimeException("rate limit"));

        consumer.consume(event, ack);

        verify(documentRepository).updateStatusById(documentId, DocumentStatus.FAILED);
        verify(ack, never()).acknowledge();
    }
}
