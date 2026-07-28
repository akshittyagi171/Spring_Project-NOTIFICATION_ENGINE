package com.notificationengine.PushNConsumer.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.notificationengine.PushNConsumer.models.PushContent;
import com.notificationengine.PushNConsumer.models.SendPushNResponse;
import com.notificationengine.PushNConsumer.repo.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
@AllArgsConstructor
@Slf4j
public class PushNSender {

    private UserRepository userRepository;

    public SendPushNResponse sendPushNotification(PushContent pushContent) {
        log.info("Initiating push notification delivery for notification ID: {}", pushContent.getNotificationId());

        try {
            if (pushContent.getFcmToken() == null || pushContent.getFcmToken().isBlank()) {
                log.warn("FCM Token is null or blank for Notification ID: {}", pushContent.getNotificationId());
                return new SendPushNResponse(400, "Bad Request: Missing FCM Token");
            }

            // 1. Build the notification with Title and Body
            Notification.Builder notifBuilder = Notification.builder()
                    .setTitle(pushContent.getTitle())
                    .setBody(pushContent.getMessage());

            // 2. Attach the image directly if the URL is provided
            if (pushContent.getMediaUrl() != null && !pushContent.getMediaUrl().isBlank()) {
                notifBuilder.setImage(pushContent.getMediaUrl().trim());
                log.info("Attached rich media URL to Push Notification ID: {}", pushContent.getNotificationId());
            }

            // 3. Extract the Deep Link URL for the action
            String actionData = (pushContent.getAction() != null && pushContent.getAction().getUrl() != null)
                    ? pushContent.getAction().getUrl()
                    : "DEFAULT";

            Message firebaseMessage = Message.builder()
                    .setToken(pushContent.getFcmToken().trim())
                    .setNotification(notifBuilder.build())
                    .putData("action", actionData)
                    .putData("notificationId", String.valueOf(pushContent.getNotificationId()))
                    .build();

            String responseId = FirebaseMessaging.getInstance().send(firebaseMessage);
            log.info("Push Notification successfully broadcasted to FCM network. Message ID: {}", responseId);
            return new SendPushNResponse(202, "Delivered: " + responseId);

        } catch (FirebaseMessagingException e) {
            MessagingErrorCode errorCode = e.getMessagingErrorCode();

            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                log.error("Stale or Invalid FCM token detected for Notification ID: {}. Error Code: {}",
                        pushContent.getNotificationId(), errorCode);

                String targetToken = pushContent.getFcmToken().trim();

                userRepository.findByFcmToken(targetToken)
                        .stream()
                        .peek(user -> log.info("Deactivating stale token for User ID: {}", user.getId()))
                        .forEach(user -> {
                            user.setFcmToken(null);
                            userRepository.save(user);
                        });
                return new SendPushNResponse(404, "Device token is no longer registered with FCM");
            }

            log.warn("Firebase technical error occurred (Retrying...). Error Code: {}", errorCode);
            throw new RuntimeException("FCM Gateway temporary failure: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Critical internal failure during transmission setup for ID: {}", pushContent.getNotificationId(), e);
            throw new RuntimeException("Internal pipeline error: " + e.getMessage(), e);
        }
    }
}