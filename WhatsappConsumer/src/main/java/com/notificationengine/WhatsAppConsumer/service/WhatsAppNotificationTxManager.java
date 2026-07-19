package com.notificationengine.WhatsAppConsumer.service;

import com.notificationengine.WhatsAppConsumer.models.db.DeliveryLog;
import com.notificationengine.WhatsAppConsumer.models.db.Notification;
import com.notificationengine.WhatsAppConsumer.models.enums.Channel;
import com.notificationengine.WhatsAppConsumer.models.enums.Status;
import com.notificationengine.WhatsAppConsumer.repo.DeliveryLogRepository;
import com.notificationengine.WhatsAppConsumer.repo.NotificationRepository;
import com.notificationengine.WhatsAppConsumer.service.exceptions.NotificationNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationTxManager {

    private final NotificationRepository notificationRepository;
    private final DeliveryLogRepository deliveryLogRepository;

    @Transactional
    public void updateNotificationStateAndLog(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification resource entity not found for ID: " + notificationId
                ));

        notification.setStatus(Status.sent);
        notificationRepository.save(notification);
        log.info("Notification status tracking updated to SENT for ID: {}", notificationId);

        DeliveryLog logEntry = new DeliveryLog(notification, Channel.whatsapp, Status.sent, "Dispatched successfully via Twilio channels.");
        deliveryLogRepository.save(logEntry);
    }

    @Transactional
    public void markNotificationAsFailed(Long notificationId, String failureReason) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification resource entity not found for ID: " + notificationId));

        notification.setStatus(Status.failed);
        notificationRepository.save(notification);
        log.warn("Notification status tracking updated to FAILED for ID: {}", notificationId);

        String cleanReason = failureReason != null && failureReason.length() > 250
                ? failureReason.substring(0, 250) + "..."
                : failureReason;

        DeliveryLog logEntry = new DeliveryLog(
                notification,
                Channel.whatsapp,
                Status.failed,
                "Delivery failed after exhausting all Kafka retries. Reason: " + cleanReason
        );
        deliveryLogRepository.save(logEntry);
    }
}