package com.notificationengine.SMSConsumer.service;

import com.notificationengine.SMSConsumer.models.SendSmsResponse;
import com.notificationengine.SMSConsumer.models.SmsContent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientSmsVendorClient {

    private final SmsSender smsSender;
    private static final String RESILIENCE_INSTANCE = "smsVendor";

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackSmsVendorCall")
    public SendSmsResponse sendSmsWithResilience(SmsContent smsContent) {
        log.info("Attempting dispatch via external vendor gateway for transaction ID: {}", smsContent.getNotificationId());
        SendSmsResponse response = smsSender.sendSms(smsContent);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            response.setMessage("SMS Delivered Successfully");
            return response;
        }

        throw new RuntimeException("Vendor endpoint returned non-2xx response status code: " + response.getStatus());
    }

    public SendSmsResponse fallbackSmsVendorCall(SmsContent smsContent, Throwable throwable) {
        log.error("Circuit tripped or backend processing failed over. Reason: {}", throwable.getMessage());
        SendSmsResponse failureResponse = new SendSmsResponse();
        failureResponse.setStatus(503);
        failureResponse.setMessage("Vendor gateway unavailable. Circuit active: " + throwable.getMessage());
        return failureResponse;
    }
}