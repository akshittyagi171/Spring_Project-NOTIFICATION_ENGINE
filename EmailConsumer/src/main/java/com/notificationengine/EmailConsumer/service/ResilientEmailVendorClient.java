package com.notificationengine.EmailConsumer.service;

import com.notificationengine.common.dto.content.EmailContent;
import com.notificationengine.common.dto.response.SendEmailResponse;
import com.notificationengine.common.exception.FatalVendorException;
import com.notificationengine.common.exception.RetryableVendorException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.notificationengine.EmailConsumer.constants.Constants.PARTIAL_DELIVERY_STATUS;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientEmailVendorClient {

    private final EmailSender emailSender;
    private static final String RESILIENCE_INSTANCE = "EmailVendor";
    private final MeterRegistry meterRegistry;

    @RateLimiter(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackEmailVendorCall")
    public SendEmailResponse sendEmailWithResilience(EmailContent emailContent) {
        log.info("Attempting dispatch via external vendor gateway for transaction ID: {}", emailContent.getNotificationId());
        SendEmailResponse response = emailSender.sendEmail(emailContent);

        if (response.getStatus() == PARTIAL_DELIVERY_STATUS) {
            log.error("Partial delivery for notification ID {}: {}", emailContent.getNotificationId(), response.getMessage());
            meterRegistry.counter("notification_vendor_result_total", "channel", "email", "status", "PARTIAL").increment();
            throw new FatalVendorException(response.getMessage());
        }

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            meterRegistry.counter("notification_vendor_result_total", "channel", "email", "status", "ACCEPTED").increment();
            return response;
        }

        if (response.getStatus() == 429) {
            log.warn("SendGrid rate limit hit for notification ID {}. Will retry via Kafka backoff.",
                    emailContent.getNotificationId());
            meterRegistry.counter("notification_vendor_result_total", "channel", "email", "status", "RATE_LIMITED").increment();
            throw new RetryableVendorException(response.getMessage() + " with Status: 429 (rate limited)");
        }

        if (response.getStatus() >= 400 && response.getStatus() < 500) {
            log.error("Permanent vendor rejection for notification ID {}: {} (Status: {})",
                    emailContent.getNotificationId(), response.getMessage(), response.getStatus());
            meterRegistry.counter("notification_vendor_result_total", "channel", "email", "status", "FATAL").increment();
            throw new FatalVendorException(response.getMessage() + " with Status: " + response.getStatus());
        }

        throw new RetryableVendorException(response.getMessage() + " with Status: " + response.getStatus());
    }

    public SendEmailResponse fallbackEmailVendorCall(EmailContent emailContent, Throwable throwable) {
        log.error("Vendor call failed for notification ID {}. Reason: {}",
                emailContent.getNotificationId(), throwable.getMessage());
        meterRegistry.counter("notification_vendor_result_total", "channel", "email", "status", "FAILURE").increment();

        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        throw new RetryableVendorException("Vendor call failed: " + throwable.getMessage(), throwable);
    }
}