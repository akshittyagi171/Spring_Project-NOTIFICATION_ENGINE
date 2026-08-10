package com.notificationengine.PushNConsumer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.PushNConsumer.service.PushNProcessingService;
import com.notificationengine.common.dto.content.PushContent;
import com.notificationengine.common.exception.FatalVendorException;
import io.micrometer.core.instrument.MeterRegistry;
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

import static com.notificationengine.PushNConsumer.constants.Constants.GROUP_ID;
import static com.notificationengine.PushNConsumer.constants.Constants.TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriorityAwarePartitionConsumer {

    private final PushNProcessingService pushNProcessingService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 5000, multiplier = 3.0, maxDelay = 60000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            exclude = {FatalVendorException.class}
    )
    @KafkaListener(id = GROUP_ID, topics = TOPIC, groupId = GROUP_ID, concurrency = "${notification.kafka.consumer.concurrency}")
    public void consume(ConsumerRecord<String, String> record,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(value = "correlationId", required = false) byte[] correlationIdBytes) {

        String correlationId = correlationIdBytes != null
                ? new String(correlationIdBytes, StandardCharsets.UTF_8)
                : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        log.debug("Record Intercepted from Partition: {}, Offset: {}", partition, record.offset());

        try {
            PushContent request = objectMapper.readValue(record.value(), PushContent.class);
            pushNProcessingService.processPushN(request);
        } catch (FatalVendorException e) {
            log.error("Fatal validation error on partition offset: {}. Routing to DLT.", record.offset(), e);
            throw e;
        } catch (Exception e) {
            log.error("Processing collapsed on partition entity offset: {}", record.offset(), e);
            throw new RuntimeException("Triggering Retries: " + e.getMessage(), e);
        } finally {
            MDC.remove("correlationId");
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
        log.error("CRITICAL AUDIT: DLT Invoked. Reason: {}", exceptionMessage);
        meterRegistry.counter("notification_dlt_total", "topic", record.topic()).increment();

        try {
            PushContent request = objectMapper.readValue(record.value(), PushContent.class);
            if (request != null && request.getNotificationId() != null) {
                pushNProcessingService.handlePermanentFailure(request.getNotificationId(), exceptionMessage);
            } else {
                log.error("Unable to extract notificationId from the raw DLT payload record.");
            }
        } catch (Exception ex) {
            log.error("Failed to process status changes inside DLT: ", ex);
        } finally {
            MDC.remove("correlationId");
        }
    }
}