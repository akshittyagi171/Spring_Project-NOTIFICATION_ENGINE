package com.notificationengine.WhatsAppConsumer.service;

import com.notificationengine.common.dto.content.WhatsAppContent;
import com.notificationengine.common.dto.response.SendWhatsAppResponse;
import com.notificationengine.WhatsAppConsumer.service.exceptions.FatalVendorException;
import com.notificationengine.WhatsAppConsumer.service.exceptions.RetryableVendorException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.notificationengine.WhatsAppConsumer.constants.Constants.PARTIAL_DELIVERY_STATUS;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientWhatsAppVendorClient {

    private final WhatsAppSender whatsAppSender;
    private static final String RESILIENCE_INSTANCE = "whatsAppVendor";
    private final MeterRegistry meterRegistry;

    @RateLimiter(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackWhatsAppVendorCall")
    public SendWhatsAppResponse sendWhatsAppWithResilience(WhatsAppContent whatsAppContent) {
        log.info("Attempting dispatch via external vendor gateway for transaction ID: {}", whatsAppContent.getNotificationId());
        SendWhatsAppResponse response = whatsAppSender.sendWhatsApp(whatsAppContent);

        if (response.getStatus() == PARTIAL_DELIVERY_STATUS) {
            log.error("Partial delivery for notification ID {}: {}", whatsAppContent.getNotificationId(), response.getMessage());
            meterRegistry.counter("notification_vendor_result_total", "channel", "whatsapp", "status", "PARTIAL").increment();
            throw new FatalVendorException(response.getMessage());
        }

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            response.setMessage("WhatsApp Delivered Successfully");
            meterRegistry.counter("notification_vendor_result_total", "channel", "whatsapp", "status", "SUCCESS").increment();
            return response;
        }

        if (response.getStatus() >= 400 && response.getStatus() < 500) {
            log.error("Permanent vendor rejection for notification ID {}: {} (Status: {})",
                    whatsAppContent.getNotificationId(), response.getMessage(), response.getStatus());
            meterRegistry.counter("notification_vendor_result_total", "channel", "whatsapp", "status", "FATAL").increment();
            throw new FatalVendorException(response.getMessage() + " with Status: " + response.getStatus());
        }

        String reason = response.getMessage() + " with Status: " + response.getStatus();
        throw new RetryableVendorException(reason);
    }

    public SendWhatsAppResponse fallbackWhatsAppVendorCall(WhatsAppContent whatsAppContent, Throwable throwable) {
        log.error("Vendor call failed for notification ID {}. Reason: {}",
                whatsAppContent.getNotificationId(), throwable.getMessage());
        meterRegistry.counter("notification_vendor_result_total", "channel", "whatsapp", "status", "FAILURE").increment();

        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        throw new RetryableVendorException("Vendor call failed: " + throwable.getMessage(), throwable);
    }
}