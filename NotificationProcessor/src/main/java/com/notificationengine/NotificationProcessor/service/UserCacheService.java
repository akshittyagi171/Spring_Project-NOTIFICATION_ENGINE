package com.notificationengine.NotificationProcessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.common.repo.UserRepository;
import com.notificationengine.common.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserCacheService {

    private static final String USER_CACHE_PREFIX = "processor:user:";

    private final UserRepository userRepository;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.user.ttl-minutes:15}")
    private long userCacheTtlMinutes;

    public UserCacheService(UserRepository userRepository,
                            RedisTemplate<Object, Object> redisTemplate,
                            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<User> findById(Long userId) {
        String cacheKey = USER_CACHE_PREFIX + userId;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                UserCacheDto dto = objectMapper.readValue(cached.toString(), UserCacheDto.class);
                return Optional.of(dto.toEntity());
            }
        } catch (Exception e) {
            log.error("Redis read failed for user cache key {}: {}", cacheKey, e.getMessage());
        }

        Optional<User> userFromDb = userRepository.findById(userId);
        userFromDb.ifPresent(user -> cacheUser(cacheKey, user));
        return userFromDb;
    }

    private void cacheUser(String cacheKey, User user) {
        try {
            UserCacheDto dto = UserCacheDto.from(user);
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(dto), userCacheTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Redis write failed for user cache key {}: {}", cacheKey, e.getMessage());
        }
    }

    private record UserCacheDto(Long id, String email, String phone, String fcmToken) {
        static UserCacheDto from(User user) {
            return new UserCacheDto(user.getId(), user.getEmail(), user.getPhone(), user.getFcmToken());
        }

        User toEntity() {
            User user = new User();
            user.setId(id);
            user.setEmail(email);
            user.setPhone(phone);
            user.setFcmToken(fcmToken);
            return user;
        }
    }
}