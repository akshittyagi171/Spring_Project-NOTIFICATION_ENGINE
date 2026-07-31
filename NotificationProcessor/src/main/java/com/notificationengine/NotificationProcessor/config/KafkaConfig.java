package com.notificationengine.NotificationProcessor.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;


import static com.notificationengine.NotificationProcessor.constants.Constants.*;

@Configuration
public class KafkaConfig {

    @Value("${notification.kafka.topic.partitions:3}")
    private int totalPartitions;

    @Bean
    public KafkaAdmin.NewTopics createTopic(){
        NewTopic smsTopic = TopicBuilder
                .name(SMS_TOPIC)
                .partitions(totalPartitions)
                .build();
        NewTopic emailTopic = TopicBuilder
                .name(EMAIL_TOPIC)
                .partitions(totalPartitions)
                .build();
        NewTopic pushNTopic = TopicBuilder
                .name(PUSH_N_TOPIC)
                .partitions(totalPartitions)
                .build();
        NewTopic whatsApp = TopicBuilder
                .name(WHATSAPP_TOPIC)
                .partitions(totalPartitions)
                .build();

        return new KafkaAdmin.NewTopics(smsTopic, emailTopic, pushNTopic, whatsApp);

    }
}
