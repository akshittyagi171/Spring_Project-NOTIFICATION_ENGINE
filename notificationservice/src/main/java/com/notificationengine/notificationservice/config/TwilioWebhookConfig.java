package com.notificationengine.notificationservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class TwilioWebhookConfig {

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.status-callback-base-url}")
    private String webhookBaseUrl;

    @Value("${twilio.sms-status-callback-base-url}")
    private String smsWebhookBaseUrl;
}