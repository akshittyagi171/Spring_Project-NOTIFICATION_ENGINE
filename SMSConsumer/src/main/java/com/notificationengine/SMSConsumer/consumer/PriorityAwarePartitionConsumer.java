package com.notificationengine.SMSConsumer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.SMSConsumer.models.SmsContent;
import com.notificationengine.SMSConsumer.service.SmsProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.notificationengine.SMSConsumer.constants.Constants.GROUP_ID;
import static com.notificationengine.SMSConsumer.constants.Constants.TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriorityAwarePartitionConsumer {

    private final SmsProcessingService smsProcessingService;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<Integer, AtomicLong> activeMessageCounts = new ConcurrentHashMap<>();

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 5000, multiplier = 3.0, maxDelay = 60000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = { RuntimeException.class }
    )
    @KafkaListener(id = GROUP_ID, topics = TOPIC, groupId = GROUP_ID, concurrency = "1")
    public void consume(ConsumerRecord<String, String> record,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(value = "correlationId", required = false) byte[] correlationIdBytes) {

        String correlationId = correlationIdBytes != null
                ? new String(correlationIdBytes, StandardCharsets.UTF_8)
                : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        log.debug("Record Intercepted from Partition: {}, Offset: {}", partition, record.offset());

        activeMessageCounts.computeIfAbsent(partition, k -> new AtomicLong(0)).incrementAndGet();

        try {
            evaluatePriorityThrottling(partition);
            // Replaced SmsRequest with SmsContent
            SmsContent request = objectMapper.readValue(record.value(), SmsContent.class);

            smsProcessingService.processSms(request);

        } catch (Exception e) {
            log.error("Processing collapsed on partition entity offset target node: {}", record.offset(), e);
            throw new RuntimeException("Triggering Retries: " + e.getMessage(), e);
        } finally {
            activeMessageCounts.get(partition).decrementAndGet();
            MDC.remove("correlationId");
        }
    }

    private void evaluatePriorityThrottling(int currentPartition) {
        long p1Count = activeMessageCounts.getOrDefault(0, new AtomicLong(0)).get();
        long p2Count = activeMessageCounts.getOrDefault(1, new AtomicLong(0)).get();

        if (currentPartition == 1 && p1Count > 0) {
            log.warn("Throttling current executor thread: P2 payload paused briefly due to active P1 streams.");
            yieldExecutionControl();
        } else if (currentPartition == 2 && (p1Count > 0 || p2Count > 0)) {
            log.warn("Throttling current executor thread: P3 payload paused briefly due to active P1/P2 streams.");
            yieldExecutionControl();
        }
    }

    private void yieldExecutionControl() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @DltHandler
    public void handleDeadLetterQueue(ConsumerRecord<String, String> record,
                                      @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
                                      @Header(value = "correlationId", required = false) byte[] correlationIdBytes) {
        String correlationId = correlationIdBytes != null
                ? new String(correlationIdBytes, StandardCharsets.UTF_8)
                : "UNKNOWN-TRACE";
        MDC.put("correlationId", correlationId);
        log.error("CRITICAL AUDIT: Final retry exhausted on consumer pipelines. Processing permanent failure tracking. Reason: {}", exceptionMessage);

        try {
            SmsContent request = objectMapper.readValue(record.value(), SmsContent.class);

            if (request != null && request.getNotificationId() != null) {
                smsProcessingService.handlePermanentFailure(request.getNotificationId(), exceptionMessage);
                log.info("Successfully moved state to FAILED for notification ID: {}", request.getNotificationId());
            } else {
                log.error("Unable to extract notificationId from the raw DLT payload record.");
            }
        } catch (Exception ex) {
            log.error("Failed to process status changes inside custom DLT wrapper flow: ", ex);
        } finally {
            // 2. Clean up Context
            MDC.remove("correlationId");
        }
    }
}