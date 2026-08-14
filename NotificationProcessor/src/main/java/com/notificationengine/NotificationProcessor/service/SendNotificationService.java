package com.notificationengine.NotificationProcessor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.common.enums.Channel;
import com.notificationengine.common.enums.Status;
import com.notificationengine.common.dto.content.EmailContent;
import com.notificationengine.common.dto.content.PushContent;
import com.notificationengine.common.dto.content.SmsContent;
import com.notificationengine.common.dto.content.WhatsAppContent;
import com.notificationengine.NotificationProcessor.service.exceptions.DuplicateNotificationFoundException;
import com.notificationengine.common.model.Notification;
import com.notificationengine.common.model.User;
import com.notificationengine.common.repo.NotificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import static com.notificationengine.NotificationProcessor.constants.Constants.NOTIFICATION_UNIQUE_CONSTRAINT_NAME;

@Service
@Slf4j
public class SendNotificationService {

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final DeliveryLogAsyncService deliveryLogAsyncService;
    private final NotificationHelperService notificationHelperService;
    private final MeterRegistry meterRegistry;

    public SendNotificationService(NotificationRepository notificationRepository,
                                   DeliveryLogAsyncService deliveryLogAsyncService,
                                   ObjectMapper objectMapper,
                                   NotificationHelperService notificationHelperService,
                                   MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.deliveryLogAsyncService = deliveryLogAsyncService;
        this.notificationHelperService = notificationHelperService;
        this.meterRegistry = meterRegistry;
    }

    public void sendSmsRequest(SmsContent smsContent, User user, String idempotencyKey, String priority) {
        boolean isSmsAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.sms);
        Notification notification = buildAndSave(user, Channel.sms, smsContent.getMessage(), idempotencyKey, isSmsAllowed, smsContent, priority);

        if (isSmsAllowed) {
            log.info("Preference: SMS is allowed acc to preferences. UserId: {}, SmsRequest: {}", user.getId(), smsContent);
            meterRegistry.counter("notifications_sent_total", "channel", "sms").increment();
            deliveryLogAsyncService.saveAsync(notification, Channel.sms, Status.pending, "Scheduled to kafka");
        } else {
            log.info("Preference: Not sending SMS as per user preferences. UserId: {}, SmsRequest: {}", user.getId(), smsContent);
            deliveryLogAsyncService.saveAsync(notification, Channel.sms, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences");
        }
    }

    public void sendPushNRequest(PushContent pushNRequest, User user, String idempotencyKey, String priority) {
        boolean isPushNAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.push);
        Notification notification = buildAndSave(user, Channel.push, pushNRequest.getTitle() + pushNRequest.getMessage(), idempotencyKey, isPushNAllowed, pushNRequest, priority);

        if (isPushNAllowed) {
            log.info("Preference: PushN is allowed acc to preferences. UserId: {}, PushNRequest: {}", user.getId(), pushNRequest);
            meterRegistry.counter("notifications_sent_total", "channel", "push").increment();
            deliveryLogAsyncService.saveAsync(notification, Channel.push, Status.pending, "Scheduled to kafka");
        } else {
            log.info("Preference: Not sending Push Notification as per user preferences. UserId: {}, PushNRequest: {}", user.getId(), pushNRequest);
            deliveryLogAsyncService.saveAsync(notification, Channel.push, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences");
        }
    }

    public void sendEmailRequest(EmailContent emailContent, User user, String idempotencyKey, String priority) {
        boolean isEmailAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.email);
        Notification notification = buildAndSave(user, Channel.email,
                "emailSubject: " + emailContent.getSubject() + " message: " + emailContent.getMessage(), idempotencyKey, isEmailAllowed, emailContent, priority);

        if (isEmailAllowed) {
            log.info("Preference: Email is allowed acc to preferences. UserId: {}, EmailRequest: {}", user.getId(), emailContent);
            meterRegistry.counter("notifications_sent_total", "channel", "email").increment();
            deliveryLogAsyncService.saveAsync(notification, Channel.email, Status.pending, "Scheduled to kafka");
        } else {
            log.info("Preference: Not sending Email Notification as per user preferences. UserId: {}, EmailRequest: {}", user.getId(), emailContent);
            deliveryLogAsyncService.saveAsync(notification, Channel.email, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences");
        }
    }

    public void sendWhatsAppRequest(WhatsAppContent whatsAppContent, User user, String idempotencyKey, String priority) {
        boolean isWhatsAppAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.whatsapp);
        Notification notification = buildAndSave(user, Channel.whatsapp, whatsAppContent.getMessage(), idempotencyKey, isWhatsAppAllowed, whatsAppContent, priority);

        if (isWhatsAppAllowed) {
            log.info("Preference: WhatsApp is allowed acc to preferences. UserId: {}, WhatsAppRequest: {}", user.getId(), whatsAppContent);
            meterRegistry.counter("notifications_sent_total", "channel", "whatsapp").increment();
            deliveryLogAsyncService.saveAsync(notification, Channel.whatsapp, Status.pending, "Scheduled to kafka");
        } else {
            log.info("Preference: Not sending WhatsApp as per user preferences. UserId: {}, WhatsAppRequest: {}", user.getId(), whatsAppContent);
            deliveryLogAsyncService.saveAsync(notification, Channel.whatsapp, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences");
        }
    }

    private <T> Notification buildAndSave(User user, Channel channel, String message, String idempotencyKey, boolean allowed, T content, String priority) {
        String requestContent = null;
        if (allowed) {
            try {
                requestContent = objectMapper.writeValueAsString(content);
            } catch (JsonProcessingException e) {
                log.error("Exception parsing {} requestContent to String: {}", channel, e.toString());
            }
        }

        Notification notification = new Notification(
                user, channel, message, requestContent,
                notificationHelperService.getNotificationHash(idempotencyKey, user.getId(), channel));
        notification.setCorrelationId(MDC.get("correlationId"));
        notification.setPriority(priority);

        try {
            return notificationRepository.save(notification);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateNotificationConstraint(e)) {
                throw new DuplicateNotificationFoundException("Duplicate notification request for userId " + user.getId() + ", channel " + channel);
            }
            log.error("Unexpected data integrity violation saving {} notification for userId {}: {}", channel, user.getId(), e.getMostSpecificCause().getMessage(), e);
            throw e;
        }
    }

    private boolean isDuplicateNotificationConstraint(DataIntegrityViolationException e) {
        Throwable rootCause = e.getMostSpecificCause();
        String rootMessage = rootCause.getMessage();
        return rootMessage != null && rootMessage.toLowerCase().contains(NOTIFICATION_UNIQUE_CONSTRAINT_NAME.toLowerCase());
    }
}