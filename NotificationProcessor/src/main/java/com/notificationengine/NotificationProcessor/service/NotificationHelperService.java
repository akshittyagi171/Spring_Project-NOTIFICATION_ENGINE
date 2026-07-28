package com.notificationengine.NotificationProcessor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.NotificationProcessor.models.db.Preference;
import com.notificationengine.NotificationProcessor.models.enums.Channel;
import com.notificationengine.NotificationProcessor.models.dtos.content.EmailContent;
import com.notificationengine.NotificationProcessor.models.dtos.content.PushContent;
import com.notificationengine.NotificationProcessor.models.dtos.content.SmsContent;
import com.notificationengine.NotificationProcessor.models.dtos.content.WhatsappContent;
import com.notificationengine.NotificationProcessor.repo.PreferenceRepository;
import com.notificationengine.NotificationProcessor.service.exceptions.PreferenceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;

@Service
@Slf4j
public class NotificationHelperService {

    private final PreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;
    private final int currentPriority;

    public NotificationHelperService(PreferenceRepository preferenceRepository,
                                     ObjectMapper objectMapper,
                                     @Value("${notification.processor.priority}") int currentPriority) {
        this.preferenceRepository = preferenceRepository;
        this.objectMapper = objectMapper;
        this.currentPriority = currentPriority;
    }

    public boolean isNotificationAllowed_PreferenceCheck(Long userId, Channel channel) {
        Preference channelPreference = preferenceRepository.findByUserIdAndChannel(userId, channel)
                .orElseThrow(() -> {
                    log.error("{} preference not found for userId: {}", channel, userId);
                    return new PreferenceNotFoundException(channel + " preference not found for userId: " + userId);
                });

        if (!channelPreference.isEnabled()) {
            log.info("Preference: Channel is disabled for userId {} and channel {}", userId, channel);
            return false;
        }

        try {
            ArrayList<Integer> allowedPriority = objectMapper.readValue(channelPreference.getAllowedMessagesPriority(), ArrayList.class);
            if (!allowedPriority.contains(currentPriority)) {
                log.info("Preference: Priority {} is disabled for channel {} for userId {}", currentPriority, channel, userId);
                return false;
            }

            JsonNode quietHours = objectMapper.readTree(channelPreference.getQuietHours());
            if (quietHours.get("quietHoursEnabled").asBoolean() && quietHoursActive(quietHours)) {
                log.info("Preference: Quiet hours active for userId {} and channel {}", userId, channel);
                return false;
            }
            return true;
        } catch (JsonProcessingException e) {
            log.error("Error parsing preferences metadata constraints", e);
        }
        return false;
    }

    private boolean quietHoursActive(JsonNode quietHours) {
        String startTimeString = quietHours.get("start").asText();
        String endTimeString = quietHours.get("end").asText();

        LocalTime startTime = LocalTime.parse(startTimeString);
        LocalTime endTime = LocalTime.parse(endTimeString);

        if (startTime.isAfter(endTime)) {
            return LocalTime.now().isAfter(startTime) || LocalTime.now().isBefore(endTime);
        } else {
            return LocalTime.now().isAfter(startTime) && LocalTime.now().isBefore(endTime);
        }
    }

    public String getSmsHash(SmsContent smsContent, Long userId) {
        return DigestUtils.sha256Hex(currentPriority + "&" + smsContent.getMessage() + "&" + smsContent.getMobileNumber() + "&" + userId + "&" + LocalTime.now());
    }

    public String getWhatsAppHash(WhatsappContent whatsAppContent, Long userId) {
        return DigestUtils.sha256Hex(currentPriority + "&" + whatsAppContent.getMessage() + "&" + whatsAppContent.getMobileNumber() + "&" + userId + "&" + LocalTime.now());
    }

    public String getPushNHash(PushContent pushNRequest, Long userId) {
        return DigestUtils.sha256Hex(currentPriority + "&" + pushNRequest.getTitle() + "&" + pushNRequest.getMessage() + "&" + pushNRequest.getAction() + "&" + userId + "&" + LocalTime.now());
    }

    public String getEmailHash(EmailContent emailContent, Long userId) {
        return DigestUtils.sha256Hex(currentPriority + "&" + emailContent.getEmailSubject() + "&" + emailContent.getMessage() + "&" + Arrays.toString(emailContent.getEmailAttachments()) + "&" + userId + "&" + LocalTime.now());
    }
}