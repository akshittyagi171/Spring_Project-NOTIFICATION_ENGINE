package com.notificationengine.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.notificationservice.dto.request.NotificationRequest;
import com.notificationengine.notificationservice.dto.request.RecipientRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.notificationengine.notificationservice.constants.Constants.*;

@Service
@Slf4j
public class KafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;

    public KafkaService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
    }

    public void sendNotification(NotificationRequest request) {
        try {
            String correlationId = MDC.get("correlationId");

            String topic = switch (request.getNotificationPriority()) {
                case 1 -> TOPIC_PRIORITY_1;
                case 2 -> TOPIC_PRIORITY_2;
                default -> TOPIC_PRIORITY_3;
            };

            for (RecipientRequest recipient : request.getRecipients()) {
                NotificationRequest singleRecipientRequest = NotificationRequest.builder()
                        .notificationPriority(request.getNotificationPriority())
                        .channels(request.getChannels())
                        .content(request.getContent())
                        .idempotencyKey(request.getIdempotencyKey())
                        .recipients(List.of(recipient))
                        .build();

                String notification = mapper.writeValueAsString(singleRecipientRequest);
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, notification);
                if (correlationId != null) {
                    record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
                }

                this.kafkaTemplate.send(record);
            }

            log.info("Notification fanned out to Kafka: {} message(s) at priority: {}", request.getRecipients().size(), request.getNotificationPriority());
        } catch (JsonProcessingException e) {
            throw new KafkaException("Failed to serialize notification payload", e);
        } catch (Exception e) {
            throw new KafkaException("Failed to send notification to broker", e);
        }
    }
}