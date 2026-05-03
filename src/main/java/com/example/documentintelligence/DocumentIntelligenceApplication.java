package com.example.documentintelligence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class DocumentIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentIntelligenceApplication.class, args);
    }
}
