package com.notificationengine.WhatsAppConsumer.service;

import com.notificationengine.WhatsAppConsumer.models.SendWhatsAppResponse;
import com.notificationengine.WhatsAppConsumer.models.WhatsAppContent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientWhatsAppVendorClient {

    private final WhatsAppSender whatsAppSender;
    private static final String RESILIENCE_INSTANCE = "whatsAppVendor";
    private final MeterRegistry meterRegistry;

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackWhatsAppVendorCall")
    public SendWhatsAppResponse sendWhatsAppWithResilience(WhatsAppContent whatsAppContent) {
        log.info("Attempting dispatch via external vendor gateway for transaction ID: {}", whatsAppContent.getNotificationId());
        SendWhatsAppResponse response = whatsAppSender.sendWhatsApp(whatsAppContent);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            response.setMessage("WhatsApp Delivered Successfully");
            meterRegistry.counter("notification_vendor_result_total", "channel", "whatsapp", "status", "SUCCESS").increment();
            return response;
        }

        throw new RuntimeException("Vendor endpoint returned non-2xx response status code: " + response.getStatus());
    }

    public SendWhatsAppResponse fallbackWhatsAppVendorCall(WhatsAppContent whatsAppContent, Throwable throwable) {
        log.error("Circuit tripped or backend processing failed over. Reason: {}", throwable.getMessage());
        meterRegistry.counter("notification_vendor_result_total", "channel", "whatsapp", "status", "FAILURE").increment();
        SendWhatsAppResponse failureResponse = new SendWhatsAppResponse();
        failureResponse.setStatus(503);
        failureResponse.setMessage("Vendor gateway unavailable. Circuit active: " + throwable.getMessage());
        return failureResponse;
    }
}