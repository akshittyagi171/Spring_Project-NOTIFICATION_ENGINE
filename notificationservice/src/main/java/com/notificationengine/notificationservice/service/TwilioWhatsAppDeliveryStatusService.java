package com.notificationengine.notificationservice.service;

import com.notificationengine.common.enums.Channel;
import com.notificationengine.common.enums.Status;
import com.notificationengine.common.model.DeliveryLog;
import com.notificationengine.common.model.Notification;
import com.notificationengine.common.repo.DeliveryLogRepository;
import com.notificationengine.common.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioWhatsAppDeliveryStatusService {

    private final NotificationRepository notificationRepository;
    private final DeliveryLogRepository deliveryLogRepository;

    private static final Map<String, Status> TWILIO_STATUS_TO_OUR_STATUS = Map.of(
            "sent", Status.sent,
            "delivered", Status.delivered,
            "read", Status.read,
            "undelivered", Status.undelivered,
            "failed", Status.failed
    );

    private static final List<Status> STATUS_PRECEDENCE = List.of(
            Status.pending, Status.sent, Status.delivered, Status.read
    );

    @Transactional
    public void applyStatusCallback(String messageSid, String twilioMessageStatus, String errorCode, String errorMessage) {
        if (messageSid == null || messageSid.isBlank()) {
            log.warn("Twilio status callback received with no MessageSid - ignoring. Status: {}", twilioMessageStatus);
            return;
        }

        Optional<Notification> maybeNotification = notificationRepository.findByProviderMessageSid(messageSid);
        if (maybeNotification.isEmpty()) {
            // Expected for the secondary SIDs of a multi-media WhatsApp send (see WhatsAppSender)
            // and for any callback that arrives after the notification row has been purged.
            log.warn("Twilio status callback for unknown/untracked MessageSid: {} (status: {})", messageSid, twilioMessageStatus);
            return;
        }

        Status newStatus = TWILIO_STATUS_TO_OUR_STATUS.get(
                twilioMessageStatus == null ? "" : twilioMessageStatus.toLowerCase());
        if (newStatus == null) {
            log.info("Ignoring non-actionable Twilio status '{}' for MessageSid {}", twilioMessageStatus, messageSid);
            return;
        }

        Notification notification = maybeNotification.get();

        if (isRegressive(notification.getStatus(), newStatus)) {
            log.warn("Ignoring out-of-order Twilio callback for notification {}: current status {} is already " +
                    "at or past incoming status {}", notification.getId(), notification.getStatus(), newStatus);
            return;
        }

        notification.setStatus(newStatus);
        notificationRepository.save(notification);

        String logMessage = "Twilio delivery-status callback: " + twilioMessageStatus;
        if (newStatus == Status.undelivered || newStatus == Status.failed) {
            logMessage += String.format(" (errorCode=%s, errorMessage=%s)", errorCode, errorMessage);
        }

        deliveryLogRepository.save(new DeliveryLog(notification, Channel.whatsapp, newStatus, logMessage));

        log.info("Notification {} moved to {} via Twilio webhook (SID: {})",
                notification.getId(), newStatus, messageSid);
    }

    private boolean isRegressive(Status current, Status incoming) {
        if (incoming == Status.undelivered || incoming == Status.failed) {
            return false;
        }
        int currentRank = STATUS_PRECEDENCE.indexOf(current);
        int incomingRank = STATUS_PRECEDENCE.indexOf(incoming);
        if (currentRank == -1 || incomingRank == -1) {
            return false;
        }
        return incomingRank <= currentRank;
    }
}