package com.notificationengine.NotificationProcessor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.NotificationProcessor.models.db.Preference;
import com.notificationengine.NotificationProcessor.models.enums.Channel;
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

    private final PreferenceCacheService preferenceCacheService;
    private final ObjectMapper objectMapper;
    private final int currentPriority;

    public NotificationHelperService(PreferenceCacheService preferenceCacheService,
                                     ObjectMapper objectMapper,
                                     @Value("${notification.processor.priority}") int currentPriority) {
        this.preferenceCacheService = preferenceCacheService;
        this.objectMapper = objectMapper;
        this.currentPriority = currentPriority;
    }

    public boolean isNotificationAllowed_PreferenceCheck(Long userId, Channel channel) {
        Preference channelPreference = preferenceCacheService.findByUserIdAndChannel(userId, channel)
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

    public String getNotificationHash(String idempotencyKey, Long userId, Channel channel) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.error("Missing idempotencyKey for userId={}, channel={} — falling back to a random key (dedup will not apply to this message)", userId, channel);
            idempotencyKey = java.util.UUID.randomUUID().toString();
        }
        return DigestUtils.sha256Hex(idempotencyKey + "&" + userId + "&" + channel);
    }
}