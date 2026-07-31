package com.notificationengine.NotificationProcessor.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CustomPartitioner implements Partitioner {

    private int[] targetPartitions;
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void configure(Map<String, ?> configs) {
        Object priorityConfig = configs.get("notification.processor.priority");
        Object partitionsConfig = configs.get("notification.topic.partitions");

        if (priorityConfig != null && partitionsConfig != null) {
            try {
                int priority = Integer.parseInt(priorityConfig.toString());
                int totalPartitions = Integer.parseInt(partitionsConfig.toString());

                int p1Count = Math.max(1, (int) (totalPartitions * 0.50));
                int p2Count = Math.max(1, (int) (totalPartitions * 0.30));
                int p3Count = Math.max(1, totalPartitions - p1Count - p2Count);

                int startIdx = 0;
                int poolSize = switch (priority) {
                    case 1 -> p1Count;
                    case 2 -> {
                        startIdx = p1Count;
                        yield p2Count;
                    }
                    default -> {
                        startIdx = p1Count + p2Count;
                        yield p3Count;
                    }
                };

                targetPartitions = new int[poolSize];
                for (int i = 0; i < poolSize; i++) {
                    targetPartitions[i] = startIdx + i;
                }

                log.info("CustomPartitioner configured for Priority {} | Target Pool: {}", priority, Arrays.toString(targetPartitions));
            } catch (NumberFormatException e) {
                log.error("Failed to parse config in CustomPartitioner", e);
                this.targetPartitions = new int[]{0};
            }
        }
    }

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        if (targetPartitions == null || targetPartitions.length == 0) return 0;
        int index = Math.abs(counter.getAndIncrement()) % targetPartitions.length;
        return targetPartitions[index];
    }

    @Override
    public void close() {}
}