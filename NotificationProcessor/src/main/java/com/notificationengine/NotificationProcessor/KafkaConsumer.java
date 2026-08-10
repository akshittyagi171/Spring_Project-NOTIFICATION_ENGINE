package com.notificationengine.NotificationProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.common.dto.request.NotificationRequest;
import com.notificationengine.NotificationProcessor.service.NotificationProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class KafkaConsumer {

    private final NotificationProcessingService notificationProcessingService;
    private final ObjectMapper mapper;

    public KafkaConsumer(NotificationProcessingService notificationProcessingService, ObjectMapper mapper) {
        this.notificationProcessingService = notificationProcessingService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "#{'${notification.processor.topic}'}",
            concurrency = "${notification.kafka.consumer.concurrency}")
    public void consumeNotificationRequest(ConsumerRecord<String, String> record,
                                           @Header(value = "correlationId", required = false) byte[] correlationIdBytes) {

        String correlationId = correlationIdBytes != null
                ? new String(correlationIdBytes, StandardCharsets.UTF_8)
                : UUID.randomUUID().toString();

        MDC.put("correlationId", correlationId);

        try {
            JsonNode notificationRequestJson = mapper.readTree(record.value());
            NotificationRequest notificationRequest = mapper.treeToValue(notificationRequestJson, NotificationRequest.class);
            notificationProcessingService.processNotification(notificationRequest);

        } catch (Exception e) {
            log.error("Unexpected Exception in NotificationProcessingService: {}", e.getMessage(), e);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
