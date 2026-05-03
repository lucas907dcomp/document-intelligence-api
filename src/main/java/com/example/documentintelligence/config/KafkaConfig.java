package com.example.documentintelligence.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /**
     * Routes failed messages to {topic}.DLQ after 2 retries (3 total attempts, 1 s apart).
     * Spring Boot auto-configures ConcurrentKafkaListenerContainerFactory and picks up
     * a single CommonErrorHandler bean automatically.
     */
    @Bean
    @SuppressWarnings("rawtypes")
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLQ", 0));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }
}
