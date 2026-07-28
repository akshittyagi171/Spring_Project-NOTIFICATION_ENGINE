package com.notificationengine.PushNConsumer.service;

import com.notificationengine.PushNConsumer.models.SendPushNResponse;
import com.notificationengine.PushNConsumer.models.PushContent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientPushNVendorClient {

    private final PushNSender pushNSender;
    private static final String RESILIENCE_INSTANCE = "fcmVendor";

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackPushNVendorCall")
    public SendPushNResponse sendPushNWithResilience(PushContent pushContent) {
        log.info("Attempting dispatch via external vendor gateway for transaction ID: {}", pushContent.getNotificationId());
        SendPushNResponse response = pushNSender.sendPushNotification(pushContent);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            response.setMessage("Push Notification Delivered Successfully");
            return response;
        }

        throw new RuntimeException("Vendor endpoint returned non-2xx response status code: " + response.getStatus());
    }

    public SendPushNResponse fallbackPushNVendorCall(PushContent pushContent, Throwable throwable) {
        log.error("Circuit tripped or backend processing failed over. Reason: {}", throwable.getMessage());
        SendPushNResponse failureResponse = new SendPushNResponse();
        failureResponse.setStatus(503);
        failureResponse.setMessage("Vendor gateway unavailable. Circuit active: " + throwable.getMessage());
        return failureResponse;
    }
}