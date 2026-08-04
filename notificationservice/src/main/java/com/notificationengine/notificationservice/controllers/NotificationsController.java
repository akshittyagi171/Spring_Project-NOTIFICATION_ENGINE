package com.notificationengine.notificationservice.controllers;

import com.notificationengine.notificationservice.models.dtos.APIResponse;
import com.notificationengine.notificationservice.models.dtos.request.NotificationRequest;
import com.notificationengine.notificationservice.models.dtos.response.NotificationResponse;
import com.notificationengine.notificationservice.models.dtos.response.RecipientResponse;
import com.notificationengine.notificationservice.service.KafkaService;
import com.notificationengine.notificationservice.service.NotificationProcessingService;
import com.notificationengine.notificationservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class NotificationsController {

    private final KafkaService kafkaService;
    private final NotificationProcessingService notificationProcessingService;
    private final UserService userService;

    public NotificationsController(KafkaService kafkaService,
                                   NotificationProcessingService notificationProcessingService,
                                   UserService userService) {
        this.kafkaService = kafkaService;
        this.notificationProcessingService = notificationProcessingService;
        this.userService = userService;
    }

    @GetMapping("/health")
    public ResponseEntity<APIResponse<String>> getHealth() {
        return ResponseEntity.ok(APIResponse.success(200, "System Operational", "Running"));
    }

    @PostMapping("/send-notification")
    public ResponseEntity<APIResponse<NotificationResponse>> sendNotification(@RequestBody NotificationRequest request) {
        try {
            // 1. Validate the payload structure
            notificationProcessingService.validateRequest(request);

            // 1b. Ensure every request carries a stable idempotency key before it
            // touches Kafka, so retries of this exact message dedupe correctly downstream.
            notificationProcessingService.assignIdempotencyKey(request);

            // 2. Resolve or provision recipient user profiles
            List<RecipientResponse> processedRecipients = userService.processRecipients(request.getRecipients());

            // Map generated/resolved user IDs back to the notification payload
            for (int i = 0; i < request.getRecipients().size(); i++) {
                request.getRecipients().get(i).setUserId(processedRecipients.get(i).getUserId());
            }

            // 3. Evaluate channel priority rules
            if (request.getNotificationPriority() == null || request.getNotificationPriority() == -1) {
                notificationProcessingService.assignPriority(request);
            }

            // 4. Dispatch payload to Kafka pipeline
            kafkaService.sendNotification(request);
            log.info("Notification forwarded to Kafka Service with priority: {}", request.getNotificationPriority());

            // 5. Build standard response payload
            NotificationResponse sendData = NotificationResponse.builder()
                    .status("QUEUED_IN_KAFKA")
                    .message("Notification request processed and enqueued successfully.")
                    .processedRecipients(processedRecipients)
                    .contentSent(request.getContent())
                    .build();

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(APIResponse.success(202, "Notification accepted for processing.", sendData));

        } catch (ResponseStatusException e) {
            log.error("Validation constraint violation: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                    .body(APIResponse.failure(e.getStatusCode().value(), "Bad Request Validation Failed", e.getReason()));

        } catch (KafkaException e) {
            log.error("Failed to forward notification to Kafka: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.failure(500, "Messaging Queue Pipeline Error", e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected pipeline anomaly: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.failure(500, "An unexpected anomaly occurred.", e.getMessage()));
        }
    }
}