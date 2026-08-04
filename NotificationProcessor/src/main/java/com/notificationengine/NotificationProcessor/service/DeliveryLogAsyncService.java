package com.notificationengine.NotificationProcessor.service;

import com.notificationengine.NotificationProcessor.models.db.DeliveryLog;
import com.notificationengine.NotificationProcessor.models.db.Notification;
import com.notificationengine.NotificationProcessor.models.enums.Channel;
import com.notificationengine.NotificationProcessor.models.enums.Status;
import com.notificationengine.NotificationProcessor.repo.DeliveryLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeliveryLogAsyncService {

    private final DeliveryLogRepository deliveryLogRepository;

    public DeliveryLogAsyncService(DeliveryLogRepository deliveryLogRepository) {
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @Async("deliveryLogExecutor")
    public void saveAsync(Notification notification, Channel channel, Status status, String message) {
        if (notification == null) {
            log.warn("Skipping delivery log write: notification is null (channel={}, status={})", channel, status);
            return;
        }
        try {
            deliveryLogRepository.save(new DeliveryLog(notification, channel, status, message));
        } catch (Exception e) {
            log.error("Failed to persist delivery log for notificationId={}, channel={}: {}",
                    notification.getId(), channel, e.getMessage(), e);
        }
    }
}