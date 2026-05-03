package com.example.documentintelligence.service.kafka;

import com.example.documentintelligence.service.kafka.event.DocumentUploadedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DocumentEventProducer {

    static final String TOPIC = "document-uploaded";

    private final KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate;

    public DocumentEventProducer(KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DocumentUploadedEvent event) {
        kafkaTemplate.send(TOPIC, event.documentId(), event);
    }
}
