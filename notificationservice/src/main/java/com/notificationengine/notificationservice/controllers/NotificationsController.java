package com.notificationengine.notificationservice.controllers;

import com.notificationengine.notificationservice.models.dtos.*;
import com.notificationengine.notificationservice.service.KafkaService;
import com.notificationengine.notificationservice.service.NotificationProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api")
public class NotificationsController {

    private final KafkaService kafkaService;
    private final NotificationProcessingService notificationProcessingService;

    public NotificationsController(KafkaService kafkaService, NotificationProcessingService notificationProcessingService) {
        this.kafkaService = kafkaService;
        this.notificationProcessingService = notificationProcessingService;
    }

    @GetMapping("/health")
    public String getHealth() {
        return "Running";
    }

    @PostMapping("/send-notification")
    public ResponseEntity<APIResponse<NotificationSendResponse>> sendNotification(@RequestBody NotificationSendRequest notificationSendRequest) {
        try {
            notificationProcessingService.validateRequest(notificationSendRequest);
            if (notificationSendRequest.getNotificationPriority() == -1) {
                notificationProcessingService.assignPriority(notificationSendRequest);
            }

            kafkaService.sendNotification(notificationSendRequest);

            log.info("Notification forwarded to Kafka Service with priority: {}", notificationSendRequest.getNotificationPriority());
            NotificationSendResponse sendResponse = new NotificationSendResponse(notificationSendRequest, "QUEUED_IN_KAFKA");

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(APIResponse.success(202, "Notification accepted for processing.", sendResponse));
        } catch (InvalidRequestException | ResponseStatusException e){
            log.error("Bad request constraint violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.failure(400, "Bad Request Validation Failed", e.getMessage()));

        } catch (KafkaException e) {
            log.error("Failed to forward notification to Kafka: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.failure(500, "Messaging Queue Pipeline Error", e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.failure(500, "An unexpected anomaly occurred.", e.getMessage()));
        }
    }
}
