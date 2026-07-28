package com.notificationengine.NotificationProcessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.NotificationProcessor.models.dtos.request.NotificationRequest;
import com.notificationengine.NotificationProcessor.models.dtos.content.NotificationContent;
import com.notificationengine.NotificationProcessor.service.NotificationProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaConsumer {

    private final NotificationProcessingService notificationProcessingService;
    private final ObjectMapper mapper;

    public KafkaConsumer(NotificationProcessingService notificationProcessingService, ObjectMapper mapper){
        this.notificationProcessingService = notificationProcessingService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "#{'${notification.processor.topic}'}")
    public void consumeNotificationRequest(String notificationRequestString){

        try{
            JsonNode notificationRequestJson = mapper.readTree(notificationRequestString);
            NotificationRequest notificationRequest = mapper.treeToValue(notificationRequestJson, NotificationRequest.class);
            log.debug("Successfully parsed Consumed Notification Request: {}", notificationRequest.toString());
            try{
                notificationProcessingService.processNotification(notificationRequest);
            } catch (Exception exception){
                log.error("Unexpected Exception in NotificationProcessingService while processing Notification Request: {}", notificationRequest);
                log.error("Exception: {}", exception.toString());
            }
        } catch (JsonProcessingException jsonProcessingException){
            log.error("Error parsing kafka consumed message to JSON. Exception: \n {}", jsonProcessingException.toString());
        }
    }
}
