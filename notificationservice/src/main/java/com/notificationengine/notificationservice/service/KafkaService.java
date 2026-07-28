package com.notificationengine.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.notificationservice.models.dtos.request.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
            String notification = mapper.writeValueAsString(request);

            if (request.getNotificationPriority() == 1) {
                this.kafkaTemplate.send(TOPIC_PRIORITY_1, notification);
            } else if (request.getNotificationPriority() == 2) {
                this.kafkaTemplate.send(TOPIC_PRIORITY_2, notification);
            } else {
                this.kafkaTemplate.send(TOPIC_PRIORITY_3, notification);
            }

            log.info("Notification Successfully forwarded to Kafka with priority: {}", request.getNotificationPriority());
        } catch (JsonProcessingException e) {
            throw new KafkaException("Failed to serialize notification payload", e);
        } catch (Exception e) {
            throw new KafkaException("Failed to send notification to broker", e);
        }
    }
}