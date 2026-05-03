package com.example.documentintelligence.api;

import com.example.documentintelligence.api.dto.QueryRequest;
import com.example.documentintelligence.api.dto.QueryResponse;
import com.example.documentintelligence.api.dto.StatusResponse;
import com.example.documentintelligence.api.dto.UploadResponse;
import com.example.documentintelligence.domain.Document;
import com.example.documentintelligence.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents", description = "PDF upload, status polling, and RAG query")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Upload a PDF document for RAG processing")
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        UUID documentId = documentService.upload(file);
        return new UploadResponse(documentId);
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get the processing status of a document")
    public StatusResponse getStatus(@PathVariable UUID id) {
        Document document = documentService.getStatus(id);
        return new StatusResponse(document.getId(), document.getStatus().name(), document.getFilename());
    }

    @PostMapping("/{id}/query")
    @Operation(summary = "Query a processed document using natural language")
    public QueryResponse query(@PathVariable UUID id,
                               @Valid @RequestBody QueryRequest request) {
        DocumentService.QueryResult result = documentService.query(id, request.question());
        return new QueryResponse(result.answer(), result.sourceChunks());
    }
}
