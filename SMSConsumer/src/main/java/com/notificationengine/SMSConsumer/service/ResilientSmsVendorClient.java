package com.notificationengine.SMSConsumer.service;

import com.notificationengine.common.dto.content.SmsContent;
import com.notificationengine.common.dto.response.SendSmsResponse;
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
public class ResilientSmsVendorClient {

    private final SmsSender smsSender;
    private static final String RESILIENCE_INSTANCE = "smsVendor";
    private final MeterRegistry meterRegistry;

    @RateLimiter(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackSmsVendorCall")
    public SendSmsResponse sendSmsWithResilience(SmsContent smsContent) {
        log.info("Attempting dispatch via external vendor gateway for transaction ID: {}", smsContent.getNotificationId());
        SendSmsResponse response = smsSender.sendSms(smsContent);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            meterRegistry.counter("notification_vendor_result_total", "channel", "sms", "status", "ACCEPTED").increment();
            return response;
        }

        if (response.getStatus() >= 400 && response.getStatus() < 500) {
            log.error("Permanent vendor rejection for notification ID {}: {} (Status: {})",
                    smsContent.getNotificationId(), response.getMessage(), response.getStatus());
            meterRegistry.counter("notification_vendor_result_total", "channel", "sms", "status", "FATAL").increment();
            throw new FatalVendorException(response.getMessage() + " with Status: " + response.getStatus());
        }

        throw new RetryableVendorException(response.getMessage() + " with Status: " + response.getStatus());
    }

    public SendSmsResponse fallbackSmsVendorCall(SmsContent smsContent, Throwable throwable) {
        log.error("Vendor call failed for notification ID {}. Reason: {}",
                smsContent.getNotificationId(), throwable.getMessage());
        meterRegistry.counter("notification_vendor_result_total", "channel", "sms", "status", "FAILURE").increment();

        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        throw new RetryableVendorException("Vendor call failed: " + throwable.getMessage(), throwable);
    }
}