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
public class SendGridEmailDeliveryStatusService {

    private final NotificationRepository notificationRepository;
    private final DeliveryLogRepository deliveryLogRepository;

    private static final Map<String, Status> SENDGRID_EVENT_TO_STATUS = Map.of(
            "delivered", Status.delivered,
            "open", Status.read,
            "bounce", Status.undelivered,
            "dropped", Status.failed
    );

    private static final List<Status> STATUS_PRECEDENCE = List.of(
            Status.pending, Status.sent, Status.delivered, Status.read
    );

    @Transactional
    public void applyEvent(Map<String, Object> event) {
        Object notificationIdRaw = event.get("notificationId");
        String sgMessageId = asString(event.get("sg_message_id"));
        String eventType = asString(event.get("event"));

        if (notificationIdRaw == null) {
            log.warn("SendGrid event '{}' (sg_message_id={}) has no notificationId custom arg - ignoring",
                    eventType, sgMessageId);
            return;
        }

        Long notificationId;
        try {
            notificationId = Long.valueOf(notificationIdRaw.toString());
        } catch (NumberFormatException e) {
            log.warn("SendGrid event '{}' has a non-numeric notificationId custom arg: {} - ignoring",
                    eventType, notificationIdRaw);
            return;
        }

        Status newStatus = SENDGRID_EVENT_TO_STATUS.get(eventType == null ? "" : eventType.toLowerCase());
        if (newStatus == null) {
            log.info("Ignoring non-actionable SendGrid event '{}' for notification {}", eventType, notificationId);
            return;
        }

        Optional<Notification> maybeNotification = notificationRepository.findById(notificationId);
        if (maybeNotification.isEmpty()) {
            log.warn("SendGrid event '{}' references unknown notification ID {}", eventType, notificationId);
            return;
        }

        Notification notification = maybeNotification.get();

        if (sgMessageId != null && notification.getProviderMessageSid() != null
                && !sgMessageId.startsWith(notification.getProviderMessageSid())) {
            log.warn("SendGrid event sg_message_id {} does not match stored provider message id {} for " +
                            "notification {} - proceeding anyway since notificationId is the correlation key",
                    sgMessageId, notification.getProviderMessageSid(), notificationId);
        }

        if (isRegressive(notification.getStatus(), newStatus)) {
            log.warn("Ignoring out-of-order SendGrid event for notification {}: current status {} is already " +
                    "at or past incoming status {}", notificationId, notification.getStatus(), newStatus);
            return;
        }

        notification.setStatus(newStatus);
        notificationRepository.save(notification);

        String logMessage = "SendGrid delivery-status event: " + eventType;
        if (newStatus == Status.undelivered || newStatus == Status.failed) {
            Object reason = event.getOrDefault("reason", event.get("type"));
            logMessage += " (reason=" + reason + ")";
        }

        deliveryLogRepository.save(new DeliveryLog(notification, Channel.email, newStatus, logMessage));

        log.info("Notification {} moved to {} via SendGrid webhook (event: {})", notificationId, newStatus, eventType);
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

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }
}