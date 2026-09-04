package com.notificationengine.WhatsAppConsumer.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.twilio.Twilio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
public class TwilioConfig {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.from-number}")
    private String fromNumber;

    @Value("${twilio.status-callback-base-url}")
    private String statusCallbackBaseUrl;

    @PostConstruct
    public void initTwilio() {
        log.info("Initializing global Twilio container instance context...");
        Twilio.init(this.accountSid, this.authToken);
        log.info("Twilio context context successfully instantiated for SID: {}", this.accountSid);
    }
}