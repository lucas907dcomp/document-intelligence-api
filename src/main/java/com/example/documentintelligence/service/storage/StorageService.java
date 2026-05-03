package com.example.documentintelligence.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface StorageService {

    /** Stores file under {PDF_STORAGE_PATH}/{documentId}/{filename} and returns the absolute path. */
    String store(String documentId, MultipartFile file) throws IOException;

    /** Retrieves the raw bytes of the file at filePath. Throws FileNotFoundException if absent (EC-7). */
    byte[] retrieve(String filePath) throws FileNotFoundException, IOException;

    /** Deletes the file (and empty parent dir) silently — no-op if already absent. */
    void delete(String filePath);
}
