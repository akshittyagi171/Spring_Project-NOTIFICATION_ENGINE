package com.notificationengine.NotificationProcessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.NotificationProcessor.models.dtos.content.*;
import com.notificationengine.NotificationProcessor.models.dtos.request.*;
import com.notificationengine.common.enums.Channel;
import com.notificationengine.NotificationProcessor.models.db.Template;
import com.notificationengine.NotificationProcessor.models.db.User;
import com.notificationengine.NotificationProcessor.repo.TemplateRepository;
import com.notificationengine.NotificationProcessor.service.exceptions.DuplicateNotificationFoundException;
import com.notificationengine.NotificationProcessor.service.exceptions.PlaceholderNotFoundInRequestException;
import com.notificationengine.NotificationProcessor.service.exceptions.TemplateNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NotificationProcessingService {

    private final ObjectMapper objectMapper;
    private final TemplateRepository templateRepository;
    private final UserCacheService userCacheService;
    private final SendNotificationService sendNotificationService;

    public NotificationProcessingService(ObjectMapper objectMapper, TemplateRepository templateRepository, UserCacheService userCacheService, SendNotificationService sendNotificationService) {
        this.objectMapper = objectMapper;
        this.templateRepository = templateRepository;
        this.userCacheService = userCacheService;
        this.sendNotificationService = sendNotificationService;
    }

    public void processNotification(NotificationRequest notificationRequest) {
        ContentRequest contentRequest = notificationRequest.getContent();

        // 1. Resolve templates for each channel independently before looping users
        resolveTemplatesForChannels(contentRequest);

        List<Channel> channels = getChannels(notificationRequest.getChannels());

        // 2. Loop through all recipients in the batch
        for (RecipientRequest recipient : notificationRequest.getRecipients()) {
            if (recipient.getUserId() == null || recipient.getUserId().isBlank()) {
                log.warn("Skipping recipient with no valid userId.");
                continue;
            }

            Long userId = Long.parseLong(recipient.getUserId());
            String idempotencyKey = notificationRequest.getIdempotencyKey();
            try {
                User user = userCacheService.findById(userId)
                        .orElseThrow(() -> new UserPrincipalNotFoundException("User with userId: " + userId + " Not found"));

                // 3. Dispatch to requested and present channels
                if (channels.contains(Channel.email) && contentRequest.getEmail() != null) {
                    safeExecute(() -> prepareAndSendEmailNotification(contentRequest.getEmail(), user, idempotencyKey), "Email");
                }
                if (channels.contains(Channel.sms) && contentRequest.getSms() != null) {
                    safeExecute(() -> prepareAndSendSMSNotification(contentRequest.getSms(), user, idempotencyKey), "SMS");
                }
                if (channels.contains(Channel.whatsapp) && contentRequest.getWhatsapp() != null) {
                    safeExecute(() -> prepareAndSendWhatsAppNotification(contentRequest.getWhatsapp(), user, idempotencyKey), "WhatsApp");
                }
                if (channels.contains(Channel.push) && contentRequest.getPush() != null) {
                    safeExecute(() -> prepareAndSendPushNotification(contentRequest.getPush(), user, idempotencyKey), "Push");
                }

            } catch (UserPrincipalNotFoundException e) {
                log.error("User with userId: {} Not found for Notification Request", userId);
            }
        }
    }

    private void resolveTemplatesForChannels(ContentRequest contentRequest) {
        if (contentRequest.getEmail() != null && contentRequest.getEmail().getTemplateName() != null) {
            contentRequest.getEmail().setMessage(resolveSingleTemplate(contentRequest.getEmail().getTemplateName(), contentRequest.getEmail().getPlaceholders()));
        }
        if (contentRequest.getWhatsapp() != null && contentRequest.getWhatsapp().getTemplateName() != null) {
            contentRequest.getWhatsapp().setMessage(resolveSingleTemplate(contentRequest.getWhatsapp().getTemplateName(), contentRequest.getWhatsapp().getPlaceholders()));
        }
        if (contentRequest.getPush() != null && contentRequest.getPush().getTemplateName() != null) {
            contentRequest.getPush().setMessage(resolveSingleTemplate(contentRequest.getPush().getTemplateName(), contentRequest.getPush().getPlaceholders()));
        }
        if (contentRequest.getSms() != null && contentRequest.getSms().getTemplateName() != null) {
            contentRequest.getSms().setMessage(resolveSingleTemplate(contentRequest.getSms().getTemplateName(), contentRequest.getSms().getPlaceholders()));
        }
    }

    private String resolveSingleTemplate(String templateName, Map<String, String> placeholders) {
        try {
            Template usedTemplate = templateRepository.findByName(templateName)
                    .orElseThrow(() -> new TemplateNotFoundException("Template: " + templateName + " Not found"));

            String[] requiredPlaceholders = objectMapper.readValue(usedTemplate.getPlaceholders(), String[].class);
            return replacePlaceholdersInMessageContent(usedTemplate.getContent(), placeholders, requiredPlaceholders);

        } catch (Exception e) {
            log.error("Failed to resolve template: {}", templateName, e);
            return ""; // Fallback or throw based on your business requirement
        }
    }

    private String replacePlaceholdersInMessageContent(String content, Map<String, String> placeholdersInRequest, String[] requiredPlaceholders) throws PlaceholderNotFoundInRequestException {
        if (placeholdersInRequest == null) placeholdersInRequest = Map.of();

        for (String s : requiredPlaceholders) {
            if (!placeholdersInRequest.containsKey(s)) {
                throw new PlaceholderNotFoundInRequestException("Value for " + s + " not found in the request for using content template");
            }
            content = content.replace("{" + s + "}", placeholdersInRequest.get(s));
        }
        return content;
    }

    // --- Dispatchers mapped to new Content Objects ---

    private void prepareAndSendEmailNotification(EmailRequest inboundEmail, User user, String idempotencyKey) {
        EmailContent emailContent = EmailContent.builder()
                .emailId(user.getEmail())
                .templateName(inboundEmail.getTemplateName())
                .placeholders(inboundEmail.getPlaceholders())
                .message(inboundEmail.getMessage())
                .subject(inboundEmail.getSubject())
                .attachments(mapEmailAttachments(inboundEmail.getAttachments()))
                .build();

        try {
            sendNotificationService.sendEmailRequest(emailContent, user, idempotencyKey);
        } catch (DuplicateNotificationFoundException e) {
            log.error("Duplicate Email Request: {}", e.getMessage());
        }
    }

    private void prepareAndSendWhatsAppNotification(WhatsappRequest inboundWhatsapp, User user, String idempotencyKey) {
        WhatsappContent whatsAppContent = WhatsappContent.builder()
                .mobileNumber(user.getPhone())
                .templateName(inboundWhatsapp.getTemplateName())
                .placeholders(inboundWhatsapp.getPlaceholders())
                .message(inboundWhatsapp.getMessage())
                .attachments(mapWhatsappAttachments(inboundWhatsapp.getAttachments()))
                .build();

        try {
            sendNotificationService.sendWhatsAppRequest(whatsAppContent, user, idempotencyKey);
        } catch (DuplicateNotificationFoundException e) {
            log.error("Duplicate WhatsApp Request: {}", e.getMessage());
        }
    }

    private void prepareAndSendPushNotification(PushRequest inboundPush, User user, String idempotencyKey) {
        // Mapping Push Action explicitly to decouple the nested action object
        PushContent.PushAction mappedAction = null;
        if (inboundPush.getAction() != null) {
            mappedAction = new PushContent.PushAction(
                    inboundPush.getAction().getType(),
                    inboundPush.getAction().getUrl()
            );
        }

        PushContent pushNContent = PushContent.builder()
                .fcmToken(user.getFcmToken())
                .templateName(inboundPush.getTemplateName())
                .placeholders(inboundPush.getPlaceholders())
                .message(inboundPush.getMessage())
                .title(inboundPush.getTitle())
                .action(mappedAction)
                .mediaUrl(inboundPush.getMediaUrl())
                .build();

        try {
            sendNotificationService.sendPushNRequest(pushNContent, user, idempotencyKey);
        } catch (DuplicateNotificationFoundException e) {
            log.error("Duplicate Push Request: {}", e.getMessage());
        }
    }

    private void prepareAndSendSMSNotification(SmsRequest inboundSms, User user, String idempotencyKey) {
        SmsContent smsContent = SmsContent.builder()
                .mobileNumber(user.getPhone())
                .templateName(inboundSms.getTemplateName())
                .placeholders(inboundSms.getPlaceholders())
                .message(inboundSms.getMessage())
                .build();

        try {
            sendNotificationService.sendSmsRequest(smsContent, user, idempotencyKey);
        } catch (DuplicateNotificationFoundException e) {
            log.error("Duplicate SMS Request: {}", e.getMessage());
        }
    }

    private List<EmailContent.EmailAttachment> mapEmailAttachments(
            List<EmailRequest.EmailAttachment> inbound) {
        if (inbound == null) return null;
        return inbound.stream()
                .map(a -> new EmailContent.EmailAttachment(
                        a.getType(), a.getUrl(), a.getFilename()))
                .toList();
    }

    private List<WhatsappContent.WhatsappAttachment> mapWhatsappAttachments(
            List<WhatsappRequest.WhatsappAttachment> inbound) {
        if (inbound == null) return null;
        return inbound.stream()
                .map(a -> new WhatsappContent.WhatsappAttachment(
                        a.getType(), a.getUrl(), a.getCaption()))
                .toList();
    }

    private List<Channel> getChannels(List<String> channelsStr) {
        List<Channel> channelList = new ArrayList<>();
        if (channelsStr == null) return channelList;

        for (String s : channelsStr) {
            try {
                channelList.add(Channel.valueOf(s.toLowerCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return channelList;
    }

    private void safeExecute(Runnable runnable, String channelName) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("Unexpected Exception while processing {} Notification: {}", channelName, e.getMessage(), e);
        }
    }
}