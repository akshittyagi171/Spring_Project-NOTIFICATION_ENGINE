package com.notificationengine.notificationservice.service;

import com.notificationengine.common.model.Template;
import com.notificationengine.common.repo.TemplateRepository;
import com.notificationengine.notificationservice.models.dtos.request.ContentRequest;
import com.notificationengine.notificationservice.models.dtos.request.NotificationRequest;
import com.notificationengine.notificationservice.models.dtos.request.RecipientRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class NotificationProcessingService {

    private final RedisService redisService;
    private final TemplateRepository templateRepository;

    public NotificationProcessingService(RedisService redisService, TemplateRepository templateRepository) {
        this.redisService = redisService;
        this.templateRepository = templateRepository;
    }

    public void assignIdempotencyKey(NotificationRequest request) {
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            String correlationId = MDC.get("correlationId");
            request.setIdempotencyKey(correlationId != null ? correlationId : UUID.randomUUID().toString());
        }
    }

    public void validateRequest(NotificationRequest request) {
        Integer priority = request.getNotificationPriority();
        if (priority != null && (priority < -1 || priority == 0 || priority > 3)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad priority request.");
        }

        List<String> channels = request.getChannels();
        if (channels == null || channels.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channels list cannot be empty.");
        }

        List<RecipientRequest> recipients = request.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipients list cannot be empty.");
        }

        ContentRequest content = request.getContent();
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content payload is missing.");
        }

        // Validate that requested channels have matching content payloads
        for (String channel : channels) {
            switch (channel.toLowerCase()) {
                case "email" -> {
                    if (content.getEmail() == null) throw badContent("email");
                }
                case "whatsapp" -> {
                    if (content.getWhatsapp() == null) throw badContent("whatsapp");
                }
                case "push" -> {
                    if (content.getPush() == null) throw badContent("push");
                }
                case "sms" -> {
                    if (content.getSms() == null) throw badContent("sms");
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported channel: " + channel);
            }
        }
    }

    private ResponseStatusException badContent(String channel) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing content payload for requested channel: " + channel);
    }

    public void assignPriority(NotificationRequest request) {
        List<String> templatesInUse = extractTemplateNames(request);

        if (templatesInUse.isEmpty()) {
            request.setNotificationPriority(2); // Default fallback
            return;
        }

        int highestPriority = 3; // 1 is highest, 3 is lowest. Start at lowest.

        for (String templateName : templatesInUse) {
            int priority = getTemplatePriority(templateName);
            if (priority != -1 && priority < highestPriority) {
                highestPriority = priority;
            }
        }

        request.setNotificationPriority(highestPriority);
    }

    private List<String> extractTemplateNames(NotificationRequest request) {
        List<String> templates = new ArrayList<>();
        ContentRequest content = request.getContent();

        if (content.getEmail() != null && content.getEmail().getTemplateName() != null)
            templates.add(content.getEmail().getTemplateName());
        if (content.getWhatsapp() != null && content.getWhatsapp().getTemplateName() != null)
            templates.add(content.getWhatsapp().getTemplateName());
        if (content.getPush() != null && content.getPush().getTemplateName() != null)
            templates.add(content.getPush().getTemplateName());
        if (content.getSms() != null && content.getSms().getTemplateName() != null)
            templates.add(content.getSms().getTemplateName());

        return templates;
    }

    private int getTemplatePriority(String templateName) {
        int priority = redisService.get(templateName);
        if (priority == -1) {
            try {
                Template usedTemplate = templateRepository.findByName(templateName).orElse(null);
                if (usedTemplate != null) {
                    priority = usedTemplate.getTemplatePriority();
                    if (priority >= 1 && priority <= 3) {
                        redisService.set(templateName, priority);
                    }
                }
            } catch (Exception e) {
                log.error("Unexpected Exception while fetching template: {} Error: {}", templateName, e.getMessage());
            }
        }
        return (priority >= 1 && priority <= 3) ? priority : -1;
    }
}