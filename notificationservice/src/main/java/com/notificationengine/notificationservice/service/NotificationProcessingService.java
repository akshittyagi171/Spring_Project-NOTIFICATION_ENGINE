package com.notificationengine.notificationservice.service;

import com.notificationengine.notificationservice.models.dtos.Content;
import com.notificationengine.notificationservice.models.dtos.NotificationSendRequest;
import com.notificationengine.notificationservice.models.dtos.Recipient;
import com.notificationengine.notificationservice.models.db.Template;
import com.notificationengine.notificationservice.repo.TemplateRepository;
import com.notificationengine.notificationservice.service.exceptions.TemplateNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Service
@Slf4j
public class NotificationProcessingService {
    RedisService redisService;
    TemplateRepository templateRepository;

    public NotificationProcessingService(RedisService redisService, TemplateRepository templateRepository){
        this.redisService = redisService;
        this.templateRepository = templateRepository;
    }

    public void validateRequest(NotificationSendRequest notificationSendRequest) {
        int priority = notificationSendRequest.getNotificationPriority();
        if ( priority < -1 || priority == 0 || priority > 3){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bad priority request.");
        }

        String channels = Arrays.toString(notificationSendRequest.getChannels());
        if (!channels.contains("sms") && !channels.contains("push") && !channels.contains("email") && !channels.contains("whatsapp")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bad channels request");
        }

        Recipient recipient = notificationSendRequest.getRecipient();
        if (recipient.getUserId().isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bad request recipient.");
        }

        Content content = notificationSendRequest.getContent();
        if(content.getMessage().isEmpty() && !content.isUsingTemplates()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bad request content.");
        }

    }

    public void assignPriority(NotificationSendRequest notificationSendRequest) {
        if(!notificationSendRequest.getContent().isUsingTemplates()){
            notificationSendRequest.setNotificationPriority(2);
        }else{
            assignPriorityWithTemplate(notificationSendRequest);
        }
    }

    private void assignPriorityWithTemplate(NotificationSendRequest notificationSendRequest) {
        String templateName = notificationSendRequest.getContent().getTemplateName();
        int priority = redisService.get(templateName);
        if(priority == -1){ //not got through redis, try db
            try{
                Template usedTemplate = templateRepository.findByName(templateName)
                        .orElseThrow(() -> new TemplateNotFoundException("Template with name: " + templateName + " Not found"));

                priority = usedTemplate.getTemplatePriority();
                if(priority == 1 || priority == 2 || priority == 3){
                    redisService.set(templateName,priority);
                }
            } catch (TemplateNotFoundException e){
                log.error("{} For notificationRequest: {}", e, notificationSendRequest);
            } catch (Exception e){
                log.error("Unexpected Exception: {} For notificationRequest: {}", e, notificationSendRequest);
            }
        }

        if(priority != 1 && priority != 2 && priority != 3){
            notificationSendRequest.setNotificationPriority(2);
        } else {
            notificationSendRequest.setNotificationPriority(priority);
        }
    }
}
