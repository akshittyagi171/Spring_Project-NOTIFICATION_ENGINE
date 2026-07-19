package com.notificationengine.EmailConsumer.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EmailConfig {

    @Value("${spring.sendgrid.api-key}")
    private String apiKey;

    @Bean
    public SendGrid sendGridClient() {
        return new SendGrid(apiKey);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}