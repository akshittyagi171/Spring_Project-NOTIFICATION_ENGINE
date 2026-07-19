package com.notificationengine.NotificationProcessor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.NotificationProcessor.models.db.DeliveryLog;
import com.notificationengine.NotificationProcessor.models.db.Notification;
import com.notificationengine.NotificationProcessor.models.db.User;
import com.notificationengine.NotificationProcessor.models.enums.Channel;
import com.notificationengine.NotificationProcessor.models.enums.Status;
import com.notificationengine.NotificationProcessor.models.requests.EmailRequest;
import com.notificationengine.NotificationProcessor.models.requests.PushNRequest;
import com.notificationengine.NotificationProcessor.models.requests.SmsRequest;
import com.notificationengine.NotificationProcessor.models.requests.WhatsAppRequest;
import com.notificationengine.NotificationProcessor.repo.DeliveryLogRepository;
import com.notificationengine.NotificationProcessor.repo.NotificationRepository;
import com.notificationengine.NotificationProcessor.service.exceptions.DuplicateNotificationFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;

import static com.notificationengine.NotificationProcessor.constants.Constants.*;

@Service
@Slf4j
public class SendNotificationService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final NotificationHelperService notificationHelperService;

    private final String priorityRoutingKey;

    public SendNotificationService(KafkaTemplate<String, String> kafkaTemplate,
                                   NotificationRepository notificationRepository,
                                   DeliveryLogRepository deliveryLogRepository,
                                   ObjectMapper objectMapper,
                                   NotificationHelperService notificationHelperService,
                                   @Value("${notification.processor.topic}") String priorityRoutingKey) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.notificationHelperService = notificationHelperService;
        this.priorityRoutingKey = priorityRoutingKey;
    }

    public void sendSmsRequest(SmsRequest smsRequest, User user) {
        Notification notification = null;
        try {
            notification = notificationRepository.save(new Notification(user, Channel.sms, smsRequest.getMessage(), objectMapper.writeValueAsString(smsRequest), notificationHelperService.getSmsHash(smsRequest, user.getId())));
            smsRequest.setNotificationId(notification.getId());
        } catch (JsonProcessingException e) {
            log.error("Exception parsing SMS requestContent to String: {}", e.toString());
        } catch (Exception e) {
            if (e.toString().contains("Duplicate entry")) {
                throw new DuplicateNotificationFoundException("Duplicate notification request. " + smsRequest.toString());
            } else {
                throw e;
            }
        }

        boolean isSmsAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.sms);
        if (isSmsAllowed) {
            try {
                log.info("Preference: SMS is allowed acc to preferences. UserId: {}, SmsRequest: {}", user.getId(), smsRequest);
                String notificationString = prepareMessage(smsRequest);
                kafkaTemplate.send(SMS_TOPIC, priorityRoutingKey, notificationString);
                deliveryLogRepository.save(new DeliveryLog(notification, Channel.sms, Status.pending, "Scheduled to kafka"));
                log.info("SMS sent to kafka. Delivery Log updated. UserId: {}, SmsRequest: {}", user.getId(), smsRequest);
            } catch (Exception e) {
                log.error("Failed to forward sms notification {} to Kafka: ", smsRequest, e);
            }
        } else {
            log.info("Preference: Not sending SMS as per user preferences. UserId: {}, SmsRequest: {}", user.getId(), smsRequest);
            deliveryLogRepository.save(new DeliveryLog(notification, Channel.sms, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences"));
        }
    }

    public void sendPushNRequest(PushNRequest pushNRequest, User user) {
        Notification notification = null;
        try {
            notification = notificationRepository.save(new Notification(user, Channel.push, pushNRequest.getTitle() + pushNRequest.getMessage(), objectMapper.writeValueAsString(pushNRequest), notificationHelperService.getPushNHash(pushNRequest, user.getId())));
            pushNRequest.setNotificationId(notification.getId());
        } catch (JsonProcessingException e) {
            log.error("Exception parsing Push requestContent to String: {}", e.toString());
        } catch (Exception e) {
            if (e.toString().contains("Duplicate entry")) {
                throw new DuplicateNotificationFoundException("Duplicate notification request. " + pushNRequest.toString());
            } else {
                throw e;
            }
        }

        boolean isPushNAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.push);
        if (isPushNAllowed) {
            try {
                log.info("Preference: PushN is allowed acc to preferences. UserId: {}, PushNRequest: {}", user.getId(), pushNRequest);
                String notificationString = prepareMessage(pushNRequest);
                kafkaTemplate.send(PUSH_N_TOPIC, priorityRoutingKey, notificationString);
                deliveryLogRepository.save(new DeliveryLog(notification, Channel.push, Status.pending, "Scheduled to kafka"));
                log.info("Push Notification sent to kafka. Delivery log updated. UserId: {}, PushNRequest: {}", user.getId(), pushNRequest);
            } catch (Exception e) {
                log.error("Failed to forward Push notification {} to Kafka: ", pushNRequest, e);
            }
        } else {
            log.info("Preference: Not sending Push Notification as per user preferences. UserId: {}, PushNRequest: {}", user.getId(), pushNRequest);
            deliveryLogRepository.save(new DeliveryLog(notification, Channel.push, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences"));
        }
    }

    public void sendEmailRequest(EmailRequest emailRequest, User user) {
        Notification notification = null;
        try {
            notification = notificationRepository.save(new Notification(user, Channel.email, "emailSubject: " + emailRequest.getEmailSubject() + " message: " + emailRequest.getMessage() + " attachments: " + Arrays.toString(emailRequest.getEmailAttachments()),
                    objectMapper.writeValueAsString(emailRequest), notificationHelperService.getEmailHash(emailRequest, user.getId())));
            emailRequest.setNotificationId(notification.getId());
        } catch (JsonProcessingException e) {
            log.error("Exception parsing Email requestContent to String: {}", e.toString());
        } catch (Exception e) {
            if (e.toString().contains("Duplicate entry")) {
                throw new DuplicateNotificationFoundException("Duplicate notification request. " + emailRequest.toString());
            } else {
                throw e;
            }
        }

        boolean isEmailAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.email);
        if (isEmailAllowed) {
            try {
                log.info("Preference: Email is allowed acc to preferences. UserId: {}, EmailRequest: {}", user.getId(), emailRequest);
                String notificationString = prepareMessage(emailRequest);
                kafkaTemplate.send(EMAIL_TOPIC, priorityRoutingKey, notificationString);
                deliveryLogRepository.save(new DeliveryLog(notification, Channel.email, Status.pending, "Scheduled to kafka"));
                log.info("Email is sent to kafka. Delivery Log updated. UserId: {}, EmailRequest: {}", user.getId(), emailRequest);
            } catch (Exception e) {
                log.error("Failed to forward Email notification {} to Kafka: ", emailRequest, e);
            }
        } else {
            log.info("Preference: Not sending Email Notification as per user preferences. UserId: {}, EmailRequest: {}", user.getId(), emailRequest);
            deliveryLogRepository.save(new DeliveryLog(notification, Channel.email, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences"));
        }
    }

    public void sendWhatsAppRequest(WhatsAppRequest whatsAppRequest, User user) {
        Notification notification = null;
        try {
            notification = notificationRepository.save(new Notification(user, Channel.whatsapp, whatsAppRequest.getMessage(), objectMapper.writeValueAsString(whatsAppRequest), notificationHelperService.getWhatsAppHash(whatsAppRequest, user.getId())));
            whatsAppRequest.setNotificationId(notification.getId());
        } catch (JsonProcessingException e) {
            log.error("Exception parsing WhatsApp requestContent to String: {}", e.toString());
        } catch (Exception e) {
            if (e.toString().contains("Duplicate entry")) {
                throw new DuplicateNotificationFoundException("Duplicate notification request. " + whatsAppRequest.toString());
            } else {
                throw e;
            }
        }

        boolean isWhatsAppAllowed = notificationHelperService.isNotificationAllowed_PreferenceCheck(user.getId(), Channel.whatsapp);
        if (isWhatsAppAllowed) {
            try {
                log.info("Preference: WhatsApp is allowed acc to preferences. UserId: {}, WhatsAppRequest: {}", user.getId(), whatsAppRequest);
                String notificationString = prepareMessage(whatsAppRequest);
                kafkaTemplate.send(WHATSAPP_TOPIC, priorityRoutingKey, notificationString);
                deliveryLogRepository.save(new DeliveryLog(notification, Channel.whatsapp, Status.pending, "Scheduled to kafka"));
                log.info("WhatsApp sent to kafka. Delivery Log updated. UserId: {}, WhatsAppRequest: {}", user.getId(), whatsAppRequest);
            } catch (Exception e) {
                log.error("Failed to forward WhatsApp notification {} to Kafka: ", whatsAppRequest, e);
            }
        } else {
            log.info("Preference: Not sending WhatsApp as per user preferences. UserId: {}, WhatsAppRequest: {}", user.getId(), whatsAppRequest);
            deliveryLogRepository.save(new DeliveryLog(notification, Channel.whatsapp, Status.failed, "Not sending notification as per user: " + user.getId() + " preferences"));
        }
    }

    private <T> String prepareMessage(T request) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(request);
    }
}