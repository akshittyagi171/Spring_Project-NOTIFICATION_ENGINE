package com.notificationengine.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.common.model.Preference;
import com.notificationengine.common.model.User;
import com.notificationengine.common.repo.PreferenceRepository;
import com.notificationengine.common.repo.UserRepository;
import com.notificationengine.notificationservice.models.dtos.request.PreferenceUpdateRequest;
import com.notificationengine.notificationservice.models.dtos.request.UserCreateRequest;
import com.notificationengine.notificationservice.models.dtos.request.UserCreateResponse;
import com.notificationengine.notificationservice.models.dtos.request.RecipientRequest;
import com.notificationengine.notificationservice.models.dtos.response.RecipientResponse;
import com.notificationengine.common.enums.Channel;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PreferenceRepository preferenceRepository;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserService self;

    @Value("${app.cache.user.ttl-hours:24}")
    private long userCacheTtlHours;

    private static final String USER_CACHE_PREFIX = "user:exists:";

    public UserService(UserRepository userRepository,
                       PreferenceRepository preferenceRepository,
                       RedisTemplate<Object, Object> redisTemplate,
                       ObjectMapper objectMapper,
                       @Lazy UserService self) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Transactional
    public UserCreateResponse createOrFindUser(UserCreateRequest request) {
        String cacheKey = USER_CACHE_PREFIX + request.getEmail();

        try {
            String cachedUserJson = (String) redisTemplate.opsForValue().get(cacheKey);
            if (cachedUserJson != null) {
                log.info("Cache HIT! User details fetched from Redis: {}", request.getEmail());
                User cachedUser = objectMapper.readValue(cachedUserJson, User.class);
                return new UserCreateResponse(cachedUser.getId(), cachedUser.getEmail(), "ALREADY_EXISTS");
            }
        } catch (Exception e) {
            log.error("Redis read layer failed, falling back to Database logic directly. Error: {}", e.getMessage());
        }

        log.info("Checking Database for email: {}", request.getEmail());
        Optional<User> dbUser = userRepository.findByEmail(request.getEmail());

        if (dbUser.isPresent()) {
            User existingUser = dbUser.get();
            populateCacheQuietly(cacheKey, existingUser);
            return new UserCreateResponse(existingUser.getId(), existingUser.getEmail(), "ALREADY_EXISTS");
        }

        log.info("User not found. Executing creation and default preference allocation pipelines.");

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setPhone(request.getPhone());
        newUser.setFcmToken(request.getFcmToken());

        User savedUser = userRepository.save(newUser);

        List<Preference> defaultPreferences = createDefaultPreferencesList(savedUser);
        preferenceRepository.saveAll(defaultPreferences);
        log.info("Successfully provisioned 4 corporate channel preference profiles for User ID: {}", savedUser.getId());

        populateCacheQuietly(cacheKey, savedUser);

        return new UserCreateResponse(savedUser.getId(), savedUser.getEmail(), "CREATED");
    }

    @Transactional
    public UserCreateResponse updateUserDetails(UserCreateRequest request) {
        String cacheKey = USER_CACHE_PREFIX + request.getEmail();
        log.info("Initiating update pipeline for user email: {}", request.getEmail());

        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User profile not found for email: " + request.getEmail()));

        if (request.getName() != null && !request.getName().isBlank()) {
            existingUser.setName(request.getName());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            existingUser.setPhone(request.getPhone());
        }

        if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
            existingUser.setFcmToken(request.getFcmToken());
        }

        User updatedUser = userRepository.save(existingUser);
        log.info("User database state updated successfully for ID: {}", updatedUser.getId());

        populateCacheQuietly(cacheKey, updatedUser);

        return new UserCreateResponse(updatedUser.getId(), updatedUser.getEmail(), "UPDATED");
    }

    @Transactional
    public void updateUserPreferences(Long userId, List<PreferenceUpdateRequest> requests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found for ID: " + userId));

        log.info("Processing {} preference updates for User ID: {}", requests.size(), userId);
        LocalDateTime adjustmentTime = LocalDateTime.now();

        for (PreferenceUpdateRequest request : requests) {
            if (request.getChannel() == null) {
                continue;
            }

            Optional<Preference> existingPrefOpt = preferenceRepository.findByUserIdAndChannel(userId, request.getChannel());

            Preference preference;
            if (existingPrefOpt.isPresent()) {
                preference = existingPrefOpt.get();
            } else {
                preference = new Preference();
                preference.setUser(user);
                preference.setChannel(request.getChannel());
                preference.setCreatedAt(adjustmentTime);
            }

            if (request.getIsEnabled() != null) {
                preference.setEnabled(request.getIsEnabled());
            }
            if (request.getAllowedMessagesPriority() != null) {
                preference.setAllowedMessagesPriority(request.getAllowedMessagesPriority());
            }
            if (request.getQuietHours() != null) {
                preference.setQuietHours(request.getQuietHours());
            }

            preference.setUpdatedAt(adjustmentTime);
            preferenceRepository.save(preference);
        }

        try {
            String cacheKey = "user:preferences:" + userId;
            redisTemplate.delete(cacheKey);
            log.debug("Evicted stale preferences from Redis cache wrapper for User ID: {}", userId);
        } catch (Exception e) {
            log.warn("Isolated Warning: Failed to clean up operational cache layers: {}", e.getMessage());
        }
    }

    public List<RecipientResponse> processRecipients(List<RecipientRequest> requests) {
        List<RecipientResponse> responses = new ArrayList<>();

        for (RecipientRequest req : requests) {
            // 1. If User ID is provided, treat as existing user
            if (req.getUserId() != null && !req.getUserId().isBlank()) {
                responses.add(RecipientResponse.builder()
                        .userId(req.getUserId())
                        .isNewUser(false)
                        .name(req.getName())
                        .email(req.getEmail())
                        .phone(req.getPhone())
                        .build());
                continue;
            }

            // 2. Validate Email
            if (req.getEmail() == null || req.getEmail().isBlank()) {
                log.warn("Skipping recipient creation: No UserId or Email provided for recipient: {}", req.getName());
                continue;
            }

            UserCreateRequest createReq = new UserCreateRequest();
            createReq.setName(req.getName());
            createReq.setEmail(req.getEmail());
            createReq.setPhone(req.getPhone());
            createReq.setFcmToken(req.getFcmToken());

            try {
                // 3. Call via the proxy ('self')!
                // Now @Transactional on createOrFindUser executes in its own isolated transaction.
                UserCreateResponse userResult = self.createOrFindUser(createReq);

                responses.add(RecipientResponse.builder()
                        .userId(userResult.getId().toString())
                        .isNewUser("CREATED".equals(userResult.getStatus()))
                        .name(req.getName())
                        .email(req.getEmail())
                        .phone(req.getPhone())
                        .build());

            } catch (Exception e) {
                // If one user fails to save (e.g., DB constraint), we catch it here.
                // The loop continues processing the rest of the batch successfully.
                log.error("Failed to process recipient {}: {}", req.getEmail(), e.getMessage());
            }
        }

        return responses;
    }

    private List<Preference> createDefaultPreferencesList(User user) {
        List<Preference> preferences = new ArrayList<>();

        String defaultPriorities = "[1, 2, 3]";
        LocalDateTime rightNow = LocalDateTime.now();

        Channel[] channels = {Channel.email, Channel.sms, Channel.push, Channel.whatsapp};

        for (Channel channel : channels) {
            Preference preference = new Preference();
            preference.setUser(user);
            preference.setChannel(channel);
            preference.setEnabled(true);
            preference.setAllowedMessagesPriority(defaultPriorities);

            String quietHoursJson = buildQuietHoursJsonForChannel(channel);
            preference.setQuietHours(quietHoursJson);

            preference.setCreatedAt(rightNow);
            preference.setUpdatedAt(rightNow);

            preferences.add(preference);
        }

        return preferences;
    }

    private String buildQuietHoursJsonForChannel(Channel channel) {
        return switch (channel) {
            case email -> "{\"end\": \"06:00\", \"start\": \"22:00\", \"quietHoursEnabled\": false}";
            case sms -> "{\"end\": \"09:00\", \"start\": \"18:00\", \"quietHoursEnabled\": false}";
            default -> "{\"end\": \"00:00\", \"start\": \"00:00\", \"quietHoursEnabled\": false}";
        };
    }

    private void populateCacheQuietly(String cacheKey, User user) {
        try {
            String userJson = objectMapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(cacheKey, userJson, Duration.ofHours(userCacheTtlHours));
        } catch (Exception e) {
            log.warn("Isolated Warning: Failed to write entity node mapping matrix to Redis cache layer: {}", e.getMessage());
        }
    }
}