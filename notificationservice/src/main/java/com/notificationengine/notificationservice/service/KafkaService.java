package com.notificationengine.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.notificationservice.models.dtos.NotificationSendRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.notificationengine.notificationservice.constants.Constants.*;

@Service
@Slf4j
public class KafkaService {

    KafkaTemplate<String, String> kafkaTemplate;

    public KafkaService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotification(NotificationSendRequest notificationSendRequest){

        try {
            String notification = prepareMessage(notificationSendRequest);
            if (notificationSendRequest.getNotificationPriority() == 1) {
                this.kafkaTemplate.send(TOPIC_PRIORITY_1, notification);
            } else if (notificationSendRequest.getNotificationPriority() == 2) {
                this.kafkaTemplate.send(TOPIC_PRIORITY_2, notification);
            } else {
                this.kafkaTemplate.send(TOPIC_PRIORITY_3, notification);
            }
            log.info("Notification Successfully forwarded to Kafka with priority: {}", notificationSendRequest.getNotificationPriority());
        } catch (Exception e){
            throw new KafkaException("Failed to send notification", e);
        }
    }

    private String prepareMessage(NotificationSendRequest notificationSendRequest) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(notificationSendRequest);
    }
}
