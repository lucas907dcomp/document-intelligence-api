package com.example.documentintelligence.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Document Intelligence API")
                        .version("1.0.0")
                        .description("RAG pipeline: upload PDFs, process with embeddings, query with natural language"));
    }
}
