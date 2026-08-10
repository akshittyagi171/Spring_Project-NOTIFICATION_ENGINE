package com.notificationengine.NotificationProcessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.common.model.Preference;
import com.notificationengine.common.enums.Channel;
import com.notificationengine.common.repo.PreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PreferenceCacheService {

    private static final String PREFERENCE_CACHE_PREFIX = "processor:preference:";

    private final PreferenceRepository preferenceRepository;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.preference.ttl-minutes:15}")
    private long preferenceCacheTtlMinutes;

    public PreferenceCacheService(PreferenceRepository preferenceRepository,
                                  RedisTemplate<Object, Object> redisTemplate,
                                  ObjectMapper objectMapper) {
        this.preferenceRepository = preferenceRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<Preference> findByUserIdAndChannel(Long userId, Channel channel) {
        String cacheKey = PREFERENCE_CACHE_PREFIX + userId + ":" + channel;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                PreferenceCacheDto dto = objectMapper.readValue(cached.toString(), PreferenceCacheDto.class);
                return Optional.of(dto.toEntity());
            }
        } catch (Exception e) {
            log.error("Redis read failed for preference cache key {}: {}", cacheKey, e.getMessage());
        }

        Optional<Preference> preferenceFromDb = preferenceRepository.findByUserIdAndChannel(userId, channel);
        preferenceFromDb.ifPresent(preference -> cachePreference(cacheKey, preference));
        return preferenceFromDb;
    }

    private void cachePreference(String cacheKey, Preference preference) {
        try {
            PreferenceCacheDto dto = PreferenceCacheDto.from(preference);
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(dto), preferenceCacheTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Redis write failed for preference cache key {}: {}", cacheKey, e.getMessage());
        }
    }

    private record PreferenceCacheDto(boolean isEnabled, String allowedMessagesPriority, String quietHours) {
        static PreferenceCacheDto from(Preference preference) {
            return new PreferenceCacheDto(preference.isEnabled(), preference.getAllowedMessagesPriority(), preference.getQuietHours());
        }

        Preference toEntity() {
            Preference preference = new Preference();
            preference.setEnabled(isEnabled);
            preference.setAllowedMessagesPriority(allowedMessagesPriority);
            preference.setQuietHours(quietHours);
            return preference;
        }
    }
}