package com.example.documentintelligence.service;

import com.example.documentintelligence.domain.Document;
import com.example.documentintelligence.domain.DocumentStatus;
import com.example.documentintelligence.exception.DocumentNotReadyException;
import com.example.documentintelligence.exception.FileTooLargeException;
import com.example.documentintelligence.exception.InvalidFileTypeException;
import com.example.documentintelligence.exception.ResourceNotFoundException;
import com.example.documentintelligence.exception.ServiceUnavailableException;
import com.example.documentintelligence.repository.DocumentRepository;
import com.example.documentintelligence.repository.EmbeddingRepository;
import com.example.documentintelligence.service.ai.EmbeddingProvider;
import com.example.documentintelligence.service.ai.LlmProvider;
import com.example.documentintelligence.service.kafka.DocumentEventProducer;
import com.example.documentintelligence.service.kafka.event.DocumentUploadedEvent;
import com.example.documentintelligence.service.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String SYSTEM_PROMPT_PREFIX =
            "Answer the user's question based exclusively on the context provided below. " +
            "If the answer is not in the context, say you do not know.\n\nContext:\n";

    private final StorageService storageService;
    private final DocumentEventProducer eventProducer;
    private final EmbeddingProvider embeddingProvider;
    private final LlmProvider llmProvider;
    private final DocumentRepository documentRepository;
    private final EmbeddingRepository embeddingRepository;

    @Value("${app.rag.top-k:5}")
    private int topK;

    public DocumentService(
            StorageService storageService,
            DocumentEventProducer eventProducer,
            EmbeddingProvider embeddingProvider,
            LlmProvider llmProvider,
            DocumentRepository documentRepository,
            EmbeddingRepository embeddingRepository) {
        this.storageService = storageService;
        this.eventProducer = eventProducer;
        this.embeddingProvider = embeddingProvider;
        this.llmProvider = llmProvider;
        this.documentRepository = documentRepository;
        this.embeddingRepository = embeddingRepository;
    }

    /**
     * CRIT-1 order: store → publish → persist → return documentId.
     * On Kafka failure: delete stored file, throw 503.
     */
    public UUID upload(MultipartFile file) {
        if (!PDF_CONTENT_TYPE.equals(file.getContentType())) {
            throw new InvalidFileTypeException("Only PDF files are accepted");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileTooLargeException("File exceeds maximum allowed size of 10MB");
        }

        UUID documentId = UUID.randomUUID();
        String filePath;
        try {
            filePath = storageService.store(documentId.toString(), file);
        } catch (Exception e) {
            throw new ServiceUnavailableException("Storage unavailable: " + e.getMessage());
        }

        try {
            eventProducer.publish(new DocumentUploadedEvent(
                    documentId.toString(), filePath, Instant.now()));
        } catch (Exception e) {
            storageService.delete(filePath);
            throw new ServiceUnavailableException("Message broker unavailable: " + e.getMessage());
        }

        Document document = Document.builder()
                .id(documentId)
                .filename(file.getOriginalFilename())
                .storagePath(filePath)
                .fileSizeBytes(file.getSize())
                .status(DocumentStatus.PROCESSING)
                .build();
        documentRepository.save(document);

        log.info("Document uploaded documentId={} filename={}", documentId, file.getOriginalFilename());
        return documentId;
    }

    public Document getStatus(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    public String query(UUID documentId, String question) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (document.getStatus() != DocumentStatus.READY) {
            throw new DocumentNotReadyException(
                    "Document is not ready for querying. Current status: " + document.getStatus());
        }

        float[] questionEmbedding = embeddingProvider.embed(question);
        String vectorStr = toVectorString(questionEmbedding);
        List<String> contextChunks = embeddingRepository.findTopKSimilarChunks(documentId, vectorStr, topK);

        String context = String.join("\n\n", contextChunks);
        return llmProvider.complete(SYSTEM_PROMPT_PREFIX + context, question);
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
