package com.example.documentintelligence.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;

public class FileSystemStorageService implements StorageService {

    private final Path baseStoragePath;

    public FileSystemStorageService(String pdfStoragePath) {
        this.baseStoragePath = Paths.get(pdfStoragePath);
    }

    @Override
    public String store(String documentId, MultipartFile file) throws IOException {
        Path docDir = baseStoragePath.resolve(documentId);
        Files.createDirectories(docDir);
        String safeFilename = Path.of(file.getOriginalFilename()).getFileName().toString();
        Path destination = docDir.resolve(safeFilename);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toAbsolutePath().toString();
    }

    @Override
    public byte[] retrieve(String filePath) throws FileNotFoundException, IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found at path: " + filePath);
        }
        return Files.readAllBytes(path);
    }

    @Override
    public void delete(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
            Path parent = path.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
                    if (!stream.iterator().hasNext()) {
                        Files.deleteIfExists(parent);
                    }
                }
            }
        } catch (IOException ignored) {
            // Silent per spec — delete is best-effort
        }
    }
}
