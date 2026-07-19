package com.notificationengine.NotificationProcessor.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;

@Slf4j
public class CustomPartitioner implements Partitioner {

    private int targetPartition = 1;

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        log.debug("Routing message on topic: {} to targeted partition: {}", topic, targetPartition);
        return targetPartition;
    }

    @Override
    public void configure(Map<String, ?> configs) {
        Object priorityConfig = configs.get("notification.processor.priority");
        if (priorityConfig != null) {
            try {
                int priority = Integer.parseInt(priorityConfig.toString());
                this.targetPartition = priority - 1;
                log.info("CustomPartitioner successfully configured for targeted partition: {}", this.targetPartition);
            } catch (NumberFormatException e) {
                log.error("Failed to parse notification.processor.priority in CustomPartitioner config", e);
            }
        }
    }

    @Override
    public void close() {
        // Clean up resources if any
    }
}