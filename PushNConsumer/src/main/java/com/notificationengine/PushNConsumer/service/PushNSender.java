package com.notificationengine.PushNConsumer.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.notificationengine.PushNConsumer.models.PushNRequest;
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

    public SendPushNResponse sendPushNotification(PushNRequest pushNRequest) {
        log.info("Initiating push notification delivery for notification ID: {}", pushNRequest.getNotificationId());

        try {
            if (pushNRequest.getFcmToken() == null || pushNRequest.getFcmToken().isBlank()) {
                log.warn("FCM Token is null or blank for Notification ID: {}", pushNRequest.getNotificationId());
                return new SendPushNResponse(400, "Bad Request: Missing FCM Token");
            }

            Notification notification = Notification.builder()
                    .setTitle(pushNRequest.getTitle())
                    .setBody(pushNRequest.getMessage())
                    .build();

            Message firebaseMessage = Message.builder()
                    .setToken(pushNRequest.getFcmToken().trim())
                    .setNotification(notification)
                    .putData("action", pushNRequest.getAction() != null ? pushNRequest.getAction() : "DEFAULT")
                    .putData("notificationId", String.valueOf(pushNRequest.getNotificationId()))
                    .build();

            String responseId = FirebaseMessaging.getInstance().send(firebaseMessage);
            log.info("Push Notification successfully broadcasted to FCM network. Message ID: {}", responseId);
            return new SendPushNResponse(202, "Delivered: " + responseId);

        } catch (FirebaseMessagingException e) {
            MessagingErrorCode errorCode = e.getMessagingErrorCode();

            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                log.error("Stale or Invalid FCM token detected for Notification ID: {}. Error Code: {}",
                        pushNRequest.getNotificationId(), errorCode);

                String targetToken = pushNRequest.getFcmToken().trim();

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
            log.error("Critical internal failure during transmission setup for ID: {}", pushNRequest.getNotificationId(), e);
            throw new RuntimeException("Internal pipeline error: " + e.getMessage(), e);
        }
    }
}