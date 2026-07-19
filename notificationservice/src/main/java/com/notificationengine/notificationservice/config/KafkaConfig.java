package com.notificationengine.notificationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import static com.notificationengine.notificationservice.constants.Constants.*;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin.NewTopics createTopics(){

        NewTopic priority1Topic = TopicBuilder
                .name(TOPIC_PRIORITY_1)
                .build();
        NewTopic priority2Topic = TopicBuilder
                .name(TOPIC_PRIORITY_2)
                .build();
        NewTopic priority3Topic = TopicBuilder
                .name(TOPIC_PRIORITY_3)
                .build();

        return new KafkaAdmin.NewTopics(priority1Topic,priority2Topic,priority3Topic);

    }
}
