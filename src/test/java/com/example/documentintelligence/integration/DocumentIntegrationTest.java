package com.example.documentintelligence.integration;

import com.example.documentintelligence.api.dto.QueryResponse;
import com.example.documentintelligence.api.dto.UploadResponse;
import com.example.documentintelligence.domain.DocumentStatus;
import com.example.documentintelligence.repository.DocumentRepository;
import com.example.documentintelligence.service.ai.EmbeddingProvider;
import com.example.documentintelligence.service.ai.LlmProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DocumentIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired DocumentRepository documentRepository;

    @MockBean EmbeddingProvider embeddingProvider;
    @MockBean LlmProvider llmProvider;

    @BeforeEach
    void configureMocks() {
        float[] vector = new float[1536];
        Arrays.fill(vector, 0.1f);
        when(embeddingProvider.embed(anyString())).thenReturn(vector);
        when(llmProvider.complete(anyString(), anyString())).thenReturn("Integration test answer.");
    }

    @Test
    void uploadAndQuery_fullPipeline_returnsAnswer() throws Exception {
        byte[] pdfBytes = buildPdf("Integration test document content for retrieval augmented generation.");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(pdfBytes) {
            @Override public String getFilename() { return "integration-test.pdf"; }
        };
        body.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<UploadResponse> uploadResponse = restTemplate.postForEntity(
                "/documents",
                new HttpEntity<>(body, headers),
                UploadResponse.class);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(uploadResponse.getBody()).isNotNull();
        UUID documentId = uploadResponse.getBody().documentId();
        assertThat(documentId).isNotNull();

        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> documentRepository.findById(documentId)
                        .map(d -> d.getStatus() == DocumentStatus.READY)
                        .orElse(false));

        HttpHeaders queryHeaders = new HttpHeaders();
        queryHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<QueryResponse> queryResponse = restTemplate.postForEntity(
                "/documents/" + documentId + "/query",
                new HttpEntity<>(Map.of("question", "What is this document about?"), queryHeaders),
                QueryResponse.class);

        assertThat(queryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(queryResponse.getBody().answer()).isEqualTo("Integration test answer.");
        assertThat(queryResponse.getBody().sourceChunks()).isNotEmpty();
    }

    static byte[] buildPdf(String text) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
