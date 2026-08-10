package com.notificationengine.SMSConsumer.service;

import com.notificationengine.SMSConsumer.models.db.DeliveryLog;
import com.notificationengine.SMSConsumer.models.db.Notification;
import com.notificationengine.common.enums.Channel;
import com.notificationengine.common.enums.Status;
import com.notificationengine.SMSConsumer.repo.DeliveryLogRepository;
import com.notificationengine.SMSConsumer.repo.NotificationRepository;
import com.notificationengine.SMSConsumer.service.exceptions.NotificationNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationTxManager {

    private final NotificationRepository notificationRepository;
    private final DeliveryLogRepository deliveryLogRepository;

    @Transactional
    public void updateNotificationStateAndLog(Long notificationId, String message) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification resource entity not found for ID: " + notificationId
                ));

        notification.setStatus(Status.sent);
        notificationRepository.save(notification);
        log.info("Notification status tracking updated to SENT for ID: {}", notificationId);

        DeliveryLog logEntry = new DeliveryLog(notification, Channel.sms, Status.sent, message);
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
                Channel.sms,
                Status.failed,
                "Delivery failed after exhausting all Kafka retries. Reason: " + cleanReason
        );
        deliveryLogRepository.save(logEntry);
    }

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notif -> Status.sent.equals(notif.getStatus()))
                .orElse(false);
    }
}