package com.example.documentintelligence.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemStorageServiceTest {

    @TempDir
    Path tempDir;

    FileSystemStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileSystemStorageService(tempDir.toString());
    }

    @Test
    void store_createsFileAtExpectedPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                "pdf content".getBytes());

        String filePath = service.store("doc-123", file);

        assertThat(Path.of(filePath)).exists();
        assertThat(filePath).endsWith("doc.pdf");
        assertThat(filePath).contains("doc-123");
        assertThat(Files.readAllBytes(Path.of(filePath))).isEqualTo("pdf content".getBytes());
    }

    @Test
    void retrieve_returnsStoredBytes() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                "hello pdf".getBytes());
        String filePath = service.store("doc-456", file);

        byte[] result = service.retrieve(filePath);

        assertThat(result).isEqualTo("hello pdf".getBytes());
    }

    @Test
    void delete_removesFileAndEmptyDirectory() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                new byte[]{1, 2, 3});
        String filePath = service.store("doc-789", file);
        Path fileAsPath = Path.of(filePath);
        Path parentDir = fileAsPath.getParent();

        service.delete(filePath);

        assertThat(fileAsPath).doesNotExist();
        assertThat(parentDir).doesNotExist(); // empty parent dir is also removed
    }

    @Test
    void delete_isNoOpWhenFileAbsent() {
        // Should not throw
        service.delete("/nonexistent/path/file.pdf");
    }

    @Test
    void retrieve_missingPath_throwsFileNotFoundException() {
        assertThatThrownBy(() -> service.retrieve("/nonexistent/path/file.pdf"))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
