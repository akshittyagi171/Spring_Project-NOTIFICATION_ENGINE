package com.notificationengine.EmailConsumer.service;

import com.notificationengine.EmailConsumer.models.EmailContent;
import com.notificationengine.EmailConsumer.models.SendEmailResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientEmailVendorClient {

    private final EmailSender emailSender;
    private static final String RESILIENCE_INSTANCE = "EmailVendor";

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackEmailVendorCall")
    public SendEmailResponse sendEmailWithResilience(EmailContent emailContent) {
        log.info("Attempting dispatch via external vendor gateway for transaction ID: {}", emailContent.getNotificationId());
        SendEmailResponse response = emailSender.sendEmail(emailContent);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            response.setMessage("Email Delivered Successfully");
            return response;
        }

        throw new RuntimeException("Vendor endpoint returned non-2xx response status code: " + response.getStatus());
    }

    public SendEmailResponse fallbackEmailVendorCall(EmailContent emailContent, Throwable throwable) {
        log.error("Circuit tripped or backend processing failed over. Reason: {}", throwable.getMessage());
        SendEmailResponse failureResponse = new SendEmailResponse();
        failureResponse.setStatus(503);
        failureResponse.setMessage("Vendor gateway unavailable. Circuit active: " + throwable.getMessage());
        return failureResponse;
    }
}