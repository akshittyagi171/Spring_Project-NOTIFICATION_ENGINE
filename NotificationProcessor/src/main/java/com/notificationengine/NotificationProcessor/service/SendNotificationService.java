package com.notificationengine.NotificationProcessor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.NotificationProcessor.models.db.Notification;
import com.notificationengine.NotificationProcessor.models.db.User;
import com.notificationengine.NotificationProcessor.models.enums.Channel;
import com.notificationengine.NotificationProcessor.models.enums.Status;
import com.notificationengine.NotificationProcessor.models.dtos.content.EmailContent;
import com.notificationengine.NotificationProcessor.models.dtos.content.PushContent;
import com.notificationengine.NotificationProcessor.models.dtos.content.SmsContent;
import com.notificationengine.NotificationProcessor.models.dtos.content.WhatsappContent;
import com.notificationengine.NotificationProcessor.repo.NotificationRepository;
import com.notificationengine.NotificationProcessor.service.exceptions.DuplicateNotificationFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


import java.nio.charset.StandardCharsets;

import static com.notificationengine.NotificationProcessor.constants.Constants.*;

@Service
@Slf4j
public class SendNotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final DeliveryLogAsyncService deliveryLogAsyncService;
    private final NotificationHelperService notificationHelperService;
    private final MeterRegistry meterRegistry;

    private final String priorityRoutingKey;

    public SendNotificationService(KafkaTemplate<String, String> kafkaTemplate,
                                   NotificationRepository notificationRepository,
                                   DeliveryLogAsyncService deliveryLogAsyncService,
                                   ObjectMapper objectMapper,
                                   NotificationHelperService notificationHelperService,
                                   MeterRegistry meterRegistry,
                                   @Value("${notification.processor.topic}") String priorityRoutingKey) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.deliveryLogAsyncService = deliveryLogAsyncService;
        this.notificationHelperService = notificationHelperService;
        this.meterRegistry = meterRegistry;
        this.priorityRoutingKey = priorityRoutingKey;
    }

    public void sendSmsRequest(SmsContent smsContent, User user, String idempotencyKey) {
        Notification notification = null;
        try {
            notification = notificationRepository.save(new Notification(user, Channel.sms, smsContent.getMessage(), objectMapper.writeValueAsString(smsContent), notificationHelperService.getNotificationHash(idempotencyKey, user.getId(), Channel.sms)));
            smsContent.setNotificationId(notification.getId());
        } catch (JsonProcessingException e) {
            log.error("Exception parsing SMS requestContent to String: {}", e.toString());
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateNotificationConstraint(e)) {
                throw new DuplicateNotificationFoundException("Duplicate notification request. " + smsContent);
            }
            log.error("Unexpected data integrity violation saving SMS notification for userId {}: {}", user.getId(), e.getMostSpecificCause().getMessage(), e);
            throw e;
        }

        boolean isSmsAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.sms);
        if (isSmsAllowed) {
            try {
                log.info("Preference: SMS is allowed acc to preferences. UserId: {}, SmsRequest: {}", user.getId(), smsContent);
                String notificationString = prepareMessage(smsContent);
                dispatchToKafkaWithTracing(SMS_TOPIC, priorityRoutingKey, notificationString);
                meterRegistry.counter("notifications_sent_total", "channel", "sms").increment();
                deliveryLogAsyncService.saveAsync(notification, Channel.sms, Status.pending, "Scheduled to kafka");
                log.info("SMS sent to kafka. Delivery Log update queued. UserId: {}, SmsRequest: {}", user.getId(), smsContent);
            } catch (Exception e) {
                log.error("Failed to forward sms notification {} to Kafka: ", smsContent, e);
            }
        } else {
            log.info("Preference: Not sending SMS as per user preferences. UserId: {}, SmsRequest: {}", user.getId(), smsContent);
            deliveryLogAsyncService.saveAsync(notification, Channel.sms, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences");
        }
    }

    public void sendPushNRequest(PushContent pushNRequest, User user, String idempotencyKey) {
        Notification notification = null;
        try {
            notification = notificationRepository.save(new Notification(user, Channel.push, pushNRequest.getTitle() + pushNRequest.getMessage(), objectMapper.writeValueAsString(pushNRequest), notificationHelperService.getNotificationHash(idempotencyKey, user.getId(), Channel.push)));
            pushNRequest.setNotificationId(notification.getId());
        } catch (JsonProcessingException e) {
            log.error("Exception parsing Push requestContent to String: {}", e.toString());
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateNotificationConstraint(e)) {
                throw new DuplicateNotificationFoundException("Duplicate notification request. " + pushNRequest);
            }
            log.error("Unexpected data integrity violation saving Push notification for userId {}: {}", user.getId(), e.getMostSpecificCause().getMessage(), e);
            throw e;
        }

        boolean isPushNAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.push);
        if (isPushNAllowed) {
            try {
                log.info("Preference: PushN is allowed acc to preferences. UserId: {}, PushNRequest: {}", user.getId(), pushNRequest);
                String notificationString = prepareMessage(pushNRequest);
                dispatchToKafkaWithTracing(PUSH_N_TOPIC, priorityRoutingKey, notificationString);
                meterRegistry.counter("notifications_sent_total", "channel", "push").increment();
                deliveryLogAsyncService.saveAsync(notification, Channel.push, Status.pending, "Scheduled to kafka");
                log.info("Push Notification sent to kafka. Delivery log update queued. UserId: {}, PushNRequest: {}", user.getId(), pushNRequest);
            } catch (Exception e) {
                log.error("Failed to forward Push notification {} to Kafka: ", pushNRequest, e);
            }
        } else {
            log.info("Preference: Not sending Push Notification as per user preferences. UserId: {}, PushNRequest: {}", user.getId(), pushNRequest);
            deliveryLogAsyncService.saveAsync(notification, Channel.push, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences");
        }
    }

    public void sendEmailRequest(EmailContent emailContent, User user, String idempotencyKey) {
        Notification notification = null;
        try {
            notification = notificationRepository.save(new Notification(user, Channel.email, "emailSubject: " + emailContent.getSubject() + " message: " + emailContent.getMessage(),
                    objectMapper.writeValueAsString(emailContent), notificationHelperService.getNotificationHash(idempotencyKey, user.getId(), Channel.email)));
            emailContent.setNotificationId(notification.getId());
        } catch (JsonProcessingException e) {
            log.error("Exception parsing Email requestContent to String: {}", e.toString());
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateNotificationConstraint(e)) {
                throw new DuplicateNotificationFoundException("Duplicate notification request. " + emailContent);
            }
            log.error("Unexpected data integrity violation saving Email notification for userId {}: {}", user.getId(), e.getMostSpecificCause().getMessage(), e);
            throw e;
        }

        boolean isEmailAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.email);
        if (isEmailAllowed) {
            try {
                log.info("Preference: Email is allowed acc to preferences. UserId: {}, EmailRequest: {}", user.getId(), emailContent);
                String notificationString = prepareMessage(emailContent);
                dispatchToKafkaWithTracing(EMAIL_TOPIC, priorityRoutingKey, notificationString);
                meterRegistry.counter("notifications_sent_total", "channel", "email").increment();
                deliveryLogAsyncService.saveAsync(notification, Channel.email, Status.pending, "Scheduled to kafka");
                log.info("Email is sent to kafka. Delivery Log update queued. UserId: {}, EmailRequest: {}", user.getId(), emailContent);
            } catch (Exception e) {
                log.error("Failed to forward Email notification {} to Kafka: ", emailContent, e);
            }
        } else {
            log.info("Preference: Not sending Email Notification as per user preferences. UserId: {}, EmailRequest: {}", user.getId(), emailContent);
            deliveryLogAsyncService.saveAsync(notification, Channel.email, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences");
        }
    }

    public void sendWhatsAppRequest(WhatsappContent whatsAppContent, User user, String idempotencyKey) {
        Notification notification = null;
        try {
            notification = notificationRepository.save(new Notification(user, Channel.whatsapp, whatsAppContent.getMessage(), objectMapper.writeValueAsString(whatsAppContent), notificationHelperService.getNotificationHash(idempotencyKey, user.getId(), Channel.whatsapp)));
            whatsAppContent.setNotificationId(notification.getId());
        } catch (JsonProcessingException e) {
            log.error("Exception parsing WhatsApp requestContent to String: {}", e.toString());
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateNotificationConstraint(e)) {
                throw new DuplicateNotificationFoundException("Duplicate notification request. " + whatsAppContent);
            }
            log.error("Unexpected data integrity violation saving WhatsApp notification for userId {}: {}", user.getId(), e.getMostSpecificCause().getMessage(), e);
            throw e;
        }

        boolean isWhatsAppAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.whatsapp);
        if (isWhatsAppAllowed) {
            try {
                log.info("Preference: WhatsApp is allowed acc to preferences. UserId: {}, WhatsAppRequest: {}", user.getId(), whatsAppContent);
                String notificationString = prepareMessage(whatsAppContent);
                dispatchToKafkaWithTracing(WHATSAPP_TOPIC, priorityRoutingKey, notificationString);
                meterRegistry.counter("notifications_sent_total", "channel", "whatsapp").increment();
                deliveryLogAsyncService.saveAsync(notification, Channel.whatsapp, Status.pending, "Scheduled to kafka");
                log.info("WhatsApp sent to kafka. Delivery Log update queued. UserId: {}, WhatsAppRequest: {}", user.getId(), whatsAppContent);
            } catch (Exception e) {
                log.error("Failed to forward WhatsApp notification {} to Kafka: ", whatsAppContent, e);
            }
        } else {
            log.info("Preference: Not sending WhatsApp as per user preferences. UserId: {}, WhatsAppRequest: {}", user.getId(), whatsAppContent);
            deliveryLogAsyncService.saveAsync(notification, Channel.whatsapp, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences");
        }
    }

    private <T> String prepareMessage(T request) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(request);
    }

    private boolean isDuplicateNotificationConstraint(DataIntegrityViolationException e) {
        Throwable rootCause = e.getMostSpecificCause();
        String rootMessage = rootCause.getMessage();
        return rootMessage != null && rootMessage.toLowerCase().contains(NOTIFICATION_UNIQUE_CONSTRAINT_NAME.toLowerCase());
    }

    private void dispatchToKafkaWithTracing(String topic, String routingKey, String payload) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, routingKey, payload);

        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
        }

        kafkaTemplate.send(record);
    }
}