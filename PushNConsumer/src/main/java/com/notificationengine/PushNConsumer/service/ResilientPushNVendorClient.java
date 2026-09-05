package com.notificationengine.PushNConsumer.service;

import com.notificationengine.common.dto.content.PushContent;
import com.notificationengine.common.dto.response.SendPushNResponse;
import com.notificationengine.common.exception.FatalVendorException;
import com.notificationengine.common.exception.RetryableVendorException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientPushNVendorClient {

    private final PushNSender pushNSender;
    private static final String RESILIENCE_INSTANCE = "fcmVendor";
    private final MeterRegistry meterRegistry;

    @RateLimiter(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackPushNVendorCall")
    public SendPushNResponse sendPushNWithResilience(PushContent pushContent) {
        log.info("Attempting dispatch via external vendor gateway for transaction ID: {}", pushContent.getNotificationId());
        SendPushNResponse response = pushNSender.sendPushNotification(pushContent);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            meterRegistry.counter("notification_vendor_result_total", "channel", "push", "status", "ACCEPTED").increment();
            return response;
        }

        if (response.getStatus() == 400 || response.getStatus() == 404) {
            log.error("Permanent push rejection for notification ID {}: {} (Status: {})",
                    pushContent.getNotificationId(), response.getMessage(), response.getStatus());
            meterRegistry.counter("notification_vendor_result_total", "channel", "push", "status", "FATAL").increment();
            throw new FatalVendorException(response.getMessage() + " with Status: " + response.getStatus());
        }

        if (response.getStatus() >= 400 && response.getStatus() < 500) {
            log.error("Permanent vendor rejection for notification ID {}: {} (Status: {})",
                    pushContent.getNotificationId(), response.getMessage(), response.getStatus());
            meterRegistry.counter("notification_vendor_result_total", "channel", "push", "status", "FATAL").increment();
            throw new FatalVendorException(response.getMessage() + " with Status: " + response.getStatus());
        }

        throw new RetryableVendorException(response.getMessage() + " with Status: " + response.getStatus());
    }

    public SendPushNResponse fallbackPushNVendorCall(PushContent pushContent, Throwable throwable) {
        log.error("Vendor call failed for notification ID {}. Reason: {}",
                pushContent.getNotificationId(), throwable.getMessage());
        meterRegistry.counter("notification_vendor_result_total", "channel", "push", "status", "FAILURE").increment();

        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        throw new RetryableVendorException("Vendor call failed: " + throwable.getMessage(), throwable);
    }
}