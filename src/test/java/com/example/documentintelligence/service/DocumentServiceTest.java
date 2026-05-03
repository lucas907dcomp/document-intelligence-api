package com.example.documentintelligence.service;

import com.example.documentintelligence.domain.Document;
import com.example.documentintelligence.domain.DocumentStatus;
import com.example.documentintelligence.exception.DocumentNotReadyException;
import com.example.documentintelligence.exception.FileTooLargeException;
import com.example.documentintelligence.exception.InvalidFileTypeException;
import com.example.documentintelligence.exception.LlmUnavailableException;
import com.example.documentintelligence.exception.ResourceNotFoundException;
import com.example.documentintelligence.exception.ServiceUnavailableException;
import com.example.documentintelligence.repository.DocumentRepository;
import com.example.documentintelligence.repository.EmbeddingRepository;
import com.example.documentintelligence.service.ai.EmbeddingProvider;
import com.example.documentintelligence.service.ai.LlmProvider;
import com.example.documentintelligence.service.kafka.DocumentEventProducer;
import com.example.documentintelligence.service.kafka.event.DocumentUploadedEvent;
import com.example.documentintelligence.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock StorageService storageService;
    @Mock DocumentEventProducer eventProducer;
    @Mock EmbeddingProvider embeddingProvider;
    @Mock LlmProvider llmProvider;
    @Mock DocumentRepository documentRepository;
    @Mock EmbeddingRepository embeddingRepository;
    @Mock MultipartFile file;

    DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                storageService, eventProducer, embeddingProvider,
                llmProvider, documentRepository, embeddingRepository);
        ReflectionTestUtils.setField(documentService, "topK", 5);
    }

    // ── upload ────────────────────────────────────────────────────────────────

    @Test
    void upload_happyPath_returnsDocumentId() throws Exception {
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(storageService.store(anyString(), any())).thenReturn("/tmp/doc/test.pdf");

        UUID result = documentService.upload(file);

        assertThat(result).isNotNull();

        // Verify CRIT-1: same UUID in Kafka event and DB record
        ArgumentCaptor<DocumentUploadedEvent> eventCaptor = ArgumentCaptor.forClass(DocumentUploadedEvent.class);
        verify(eventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().documentId()).isEqualTo(result.toString());

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        Document saved = docCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(result);
        assertThat(saved.getFilename()).isEqualTo("test.pdf");
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
    }

    @Test
    void upload_kafkaFailure_deletesFileAndThrowsServiceUnavailable() throws Exception {
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L);
        String filePath = "/tmp/doc/test.pdf";
        when(storageService.store(anyString(), any())).thenReturn(filePath);
        doThrow(new RuntimeException("broker unreachable")).when(eventProducer).publish(any());

        assertThatThrownBy(() -> documentService.upload(file))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("broker unavailable");

        verify(storageService).delete(filePath);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void upload_invalidContentType_throwsInvalidFileType() {
        when(file.getContentType()).thenReturn("image/png");

        assertThatThrownBy(() -> documentService.upload(file))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void upload_fileTooLarge_throwsFileTooLarge() {
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(11L * 1024 * 1024); // 11 MB

        assertThatThrownBy(() -> documentService.upload(file))
                .isInstanceOf(FileTooLargeException.class);
    }

    // ── getStatus ─────────────────────────────────────────────────────────────

    @Test
    void getStatus_found_returnsDocument() {
        UUID id = UUID.randomUUID();
        Document doc = Document.builder().id(id).status(DocumentStatus.READY).build();
        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));

        Document result = documentService.getStatus(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.READY);
    }

    @Test
    void getStatus_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getStatus(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── query ─────────────────────────────────────────────────────────────────

    @Test
    void query_readyDocument_returnsAnswerWithSourceChunks() {
        UUID id = UUID.randomUUID();
        Document doc = Document.builder().id(id).status(DocumentStatus.READY).build();
        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));

        float[] embedding = {0.1f, 0.2f, 0.3f};
        when(embeddingProvider.embed("What is this?")).thenReturn(embedding);
        List<String> chunks = List.of("Relevant chunk 1", "Relevant chunk 2");
        when(embeddingRepository.findTopKSimilarChunks(eq(id), anyString(), eq(5)))
                .thenReturn(chunks);
        when(llmProvider.complete(anyString(), eq("What is this?"))).thenReturn("The answer.");

        DocumentService.QueryResult result = documentService.query(id, "What is this?");

        assertThat(result.answer()).isEqualTo("The answer.");
        assertThat(result.sourceChunks()).containsExactlyElementsOf(chunks);
    }

    @Test
    void query_processingDocument_throwsDocumentNotReady() {
        UUID id = UUID.randomUUID();
        Document doc = Document.builder().id(id).status(DocumentStatus.PROCESSING).build();
        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.query(id, "question"))
                .isInstanceOf(DocumentNotReadyException.class)
                .hasMessageContaining("PROCESSING");
    }

    @Test
    void query_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.query(id, "question"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void query_llmFailure_throwsLlmUnavailable() {
        UUID id = UUID.randomUUID();
        Document doc = Document.builder().id(id).status(DocumentStatus.READY).build();
        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));
        when(embeddingProvider.embed(anyString())).thenReturn(new float[]{0.1f});
        when(embeddingRepository.findTopKSimilarChunks(any(), anyString(), anyInt()))
                .thenReturn(List.of("ctx"));
        when(llmProvider.complete(anyString(), anyString())).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> documentService.query(id, "question"))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("LLM provider unavailable");
    }
}
