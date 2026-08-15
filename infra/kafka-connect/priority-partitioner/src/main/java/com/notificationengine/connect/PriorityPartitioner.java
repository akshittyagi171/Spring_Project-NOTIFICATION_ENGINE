package com.notificationengine.connect;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PriorityPartitioner implements Partitioner {

    private static final String PREFIX = "priority-";
    private final Map<String, AtomicInteger> countersByTopicPriority = new ConcurrentHashMap<>();

    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        int totalPartitions = cluster.partitionCountForTopic(topic);
        if (totalPartitions <= 0) {
            return 0;
        }

        String rawKey = keyBytes != null ? new String(keyBytes, StandardCharsets.UTF_8) : "1";
        String priority = rawKey.startsWith(PREFIX) ? rawKey.substring(PREFIX.length()) : rawKey;

        int p1Count = Math.max(1, (int) (totalPartitions * 0.50));
        int p2Count = Math.max(1, (int) (totalPartitions * 0.30));
        int p3Count = Math.max(1, totalPartitions - p1Count - p2Count);

        if (p1Count + p2Count + p3Count > totalPartitions) {
            p1Count = 1;
            p2Count = Math.min(1, totalPartitions - 1);
            p3Count = Math.max(0, totalPartitions - p1Count - p2Count);
        }

        int startIdx;
        int poolSize;
        switch (priority) {
            case "1":
                startIdx = 0;
                poolSize = p1Count;
                break;
            case "2":
                startIdx = p1Count;
                poolSize = p2Count;
                break;
            default:
                startIdx = p1Count + p2Count;
                poolSize = p3Count;
                break;
        }

        if (poolSize <= 0) {
            return 0;
        }

        String counterKey = topic + ":" + priority;
        AtomicInteger counter = countersByTopicPriority.computeIfAbsent(counterKey, k -> new AtomicInteger(0));
        int index = Integer.remainderUnsigned(counter.getAndIncrement(), poolSize);
        return startIdx + index;
    }

    @Override
    public void close() {
    }
}