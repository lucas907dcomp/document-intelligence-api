package com.example.documentintelligence.config;

import com.example.documentintelligence.service.storage.FileSystemStorageService;
import com.example.documentintelligence.service.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Value("${app.storage.pdf-storage-path}")
    private String pdfStoragePath;

    @Bean
    public StorageService storageService() {
        return new FileSystemStorageService(pdfStoragePath);
    }
}
