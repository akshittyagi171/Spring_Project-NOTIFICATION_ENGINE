package com.notificationengine.notificationservice.config;

import lombok.Getter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Configuration
@Getter
public class SendGridWebhookConfig {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${sendgrid.event-webhook-verification-key}")
    private String verificationKey;
}