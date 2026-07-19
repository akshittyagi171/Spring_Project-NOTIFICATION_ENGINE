package com.notificationengine.NotificationProcessor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.NotificationProcessor.models.dtos.Content;
import com.notificationengine.NotificationProcessor.models.dtos.NotificationRequest;
import com.notificationengine.NotificationProcessor.models.dtos.PushNotification;
import com.notificationengine.NotificationProcessor.models.enums.Channel;
import com.notificationengine.NotificationProcessor.models.requests.EmailRequest;
import com.notificationengine.NotificationProcessor.models.requests.PushNRequest;
import com.notificationengine.NotificationProcessor.models.requests.SmsRequest;
import com.notificationengine.NotificationProcessor.models.db.Template;
import com.notificationengine.NotificationProcessor.models.db.User;
import com.notificationengine.NotificationProcessor.models.requests.WhatsAppRequest;
import com.notificationengine.NotificationProcessor.repo.TemplateRepository;
import com.notificationengine.NotificationProcessor.repo.UserRepository;
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
    ObjectMapper objectMapper;
    TemplateRepository templateRepository;
    UserRepository userRepository;
    SendNotificationService sendNotificationService;

    public NotificationProcessingService(ObjectMapper objectMapper,TemplateRepository templateRepository, UserRepository userRepository, SendNotificationService sendNotificationService){
        this.objectMapper = objectMapper;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.sendNotificationService = sendNotificationService;
    }

    public void processNotification(NotificationRequest notificationRequest) {

        if (notificationRequest.getContent().isUsingTemplates()){
            prepareMessageFromTemplate(notificationRequest);
        }

        //Channel validation done at Notification Service
        ArrayList<Channel> channels = getChannels(notificationRequest.getChannels());
        Long userId = Long.parseLong(notificationRequest.getRecipient().getUserId());
        try {
            //Get user from DB
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User with userId: {} Not found", userId);
                        return new UserPrincipalNotFoundException("User with userId: " + userId + " Not found");
                    });

            if(channels.contains(Channel.email)){
                try {
                    prepareAndSendEmailNotification(notificationRequest, user.getEmail(), user);
                } catch (Exception exception){
                    log.error("Unexpected Exception while processing Email Notification Request: {}", notificationRequest);
                    log.error("Exception at Email: {}", exception.toString());
                }
            }
            if(channels.contains(Channel.sms)){
                try{
                    prepareAndSendSMSNotification(notificationRequest.getContent().getMessage(),user.getPhone(),user);
                }catch (Exception exception){
                    log.error("Unexpected Exception while processing SMS Notification Request: {}", notificationRequest);
                    log.error("Exception at SMS: {}", exception.toString());
                }

            }
            if(channels.contains(Channel.whatsapp)){
                try{
                    prepareAndSendWhatsAppNotification(notificationRequest, user.getPhone(), user);
                }catch (Exception exception){
                    log.error("Unexpected Exception while processing WhatsApp Notification Request: {}", notificationRequest);
                    log.error("Exception at WhatsApp: {}", exception.toString());
                }

            }
            if(channels.contains(Channel.push)){
                try{
                    prepareAndSendPushNotification(notificationRequest,user);
                }catch (Exception exception){
                    log.error("Unexpected Exception while processing Push Notification Request: {}", notificationRequest);
                    log.error("Exception at Push: {}", exception.toString());
                }
            }
        } catch (UserPrincipalNotFoundException e) {
            log.error("User with userId: {} Not found for Notification Request: {}", userId, notificationRequest);
        }
    }

    private void prepareMessageFromTemplate(NotificationRequest notificationRequest) {
        String templateName = notificationRequest.getContent().getTemplateName();
        try{
            Template usedTemplate = templateRepository.findByName(templateName)
                    .orElseThrow(() -> {
                        log.error("Template with name: {} Not found", templateName);
                        return new TemplateNotFoundException("Template with name: " + templateName + " Not found");
                    });
            log.info("Used Template: {}", usedTemplate.toString());
            Map<String,String> placeholdersInRequest = notificationRequest.getContent().getPlaceholders();
            String[] requiredPlaceholders = objectMapper.readValue(usedTemplate.getPlaceholders(),String[].class);

            String updatedMessage = replacePlaceholdersInMessageContent(usedTemplate.getContent(),placeholdersInRequest,requiredPlaceholders);
            notificationRequest.getContent().setMessage(updatedMessage);
            log.info("Updated message: {}", updatedMessage);
        } catch (TemplateNotFoundException | PlaceholderNotFoundInRequestException e){
            log.error("{} For notificationRequest: {}", e, notificationRequest);
        } catch (JsonProcessingException e) {
            log.error("Error parsing String placeholders to Json from usedTemplate. {} For notificationRequest: {}", e, notificationRequest);
        }
    }

    private String replacePlaceholdersInMessageContent(String content, Map<String, String> placeholdersInRequest, String[] requiredPlaceholders) throws PlaceholderNotFoundInRequestException {
        for(String s: requiredPlaceholders){ //check all required placeholders exist in request
            if(!placeholdersInRequest.containsKey(s)){
                throw new PlaceholderNotFoundInRequestException("Value for "+s+" not found in the request for using content template");
            }
        }
        for(String s: requiredPlaceholders){
            content = content.replace("{"+s+"}",placeholdersInRequest.get(s));
        }
        return content;
    }

    private void prepareAndSendPushNotification(NotificationRequest notificationRequest, User user) {
        Content content = notificationRequest.getContent();
        PushNotification pushNotification = content.getPushNotification();
        PushNRequest pushNRequest = new PushNRequest(pushNotification.getTitle(),content.getMessage(),pushNotification.getAction().getUrl(), user.getFcmToken());
        try{
            sendNotificationService.sendPushNRequest(pushNRequest, user);
        } catch (DuplicateNotificationFoundException duplicateNotificationFoundException){
            log.error("Duplicate Push Notification Request. {}", duplicateNotificationFoundException.toString());
        }
    }

    private void prepareAndSendSMSNotification(String message, String phone, User user) {
        SmsRequest smsRequest = new SmsRequest(phone,message);
        try{
            sendNotificationService.sendSmsRequest(smsRequest, user);
        }catch (DuplicateNotificationFoundException duplicateNotificationFoundException){
            log.error("Duplicate SMS Request. {}", duplicateNotificationFoundException.toString());
        }
    }

    private void prepareAndSendWhatsAppNotification(NotificationRequest notificationRequest, String phone, User user) {
        Content content = notificationRequest.getContent();

        WhatsAppRequest whatsAppRequest = new WhatsAppRequest();
        whatsAppRequest.setMobileNumber(phone);
        whatsAppRequest.setMessage(content.getMessage());

        whatsAppRequest.setTemplateName(content.getTemplateName());
        whatsAppRequest.setPlaceholders(content.getPlaceholders());

        if (content.getEmailAttachments() != null && content.getEmailAttachments().length > 0) {
            whatsAppRequest.setMediaUrls(List.of(content.getEmailAttachments()));
        }

        try {
            sendNotificationService.sendWhatsAppRequest(whatsAppRequest, user);
        } catch (DuplicateNotificationFoundException duplicateNotificationFoundException) {
            log.error("Duplicate WhatsApp Request caught. Execution skipped: {}", duplicateNotificationFoundException.toString());
        }
    }

    private void prepareAndSendEmailNotification(NotificationRequest notificationRequest, String email, User user) {
        Content content = notificationRequest.getContent();

        EmailRequest emailRequest = new EmailRequest(email,content.getMessage(),content.getEmailSubject(),content.getEmailAttachments());
        try{
            sendNotificationService.sendEmailRequest(emailRequest,user);
        } catch (DuplicateNotificationFoundException duplicateNotificationFoundException){
            log.error("Duplicate Email Request. {}", duplicateNotificationFoundException.toString());
        }
    }

    private ArrayList<Channel> getChannels(String[] channels) {
        ArrayList<Channel> channelList = new ArrayList<>();
        for (String s: channels){
            switch (s) {
                case "email" -> channelList.add(Channel.email);
                case "sms" -> channelList.add(Channel.sms);
                case "push" -> channelList.add(Channel.push);
                case "whatsapp" -> channelList.add(Channel.whatsapp);
            }
        }
        return channelList;
    }
}
