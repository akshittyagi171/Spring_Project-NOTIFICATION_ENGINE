package com.notificationengine.NotificationProcessor.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;


import static com.notificationengine.NotificationProcessor.constants.Constants.*;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin.NewTopics createTopic(){
        NewTopic smsTopic = TopicBuilder
                .name(SMS_TOPIC)
                .partitions(3)
                .build();
        NewTopic emailTopic = TopicBuilder
                .name(EMAIL_TOPIC)
                .partitions(3)
                .build();
        NewTopic pushNTopic = TopicBuilder
                .name(PUSH_N_TOPIC)
                .partitions(3)
                .build();
        NewTopic whatsApp = TopicBuilder
                .name(WHATSAPP_TOPIC)
                .partitions(3)
                .build();

        return new KafkaAdmin.NewTopics(smsTopic, emailTopic, pushNTopic, whatsApp);

    }
}
