package com.notificationengine.SMSConsumer.service;

import com.notificationengine.SMSConsumer.models.SendSmsResponse;
import com.notificationengine.SMSConsumer.models.SmsContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsProcessingService {

    private final ResilientSmsVendorClient vendorClient;
    private final SmsNotificationTxManager txManager;

    public void processSms(SmsContent smsContent) {
        if (txManager.isAlreadyProcessed(smsContent.getNotificationId())) {
            log.warn("Idempotency hit: Notification ID {} is already marked as SENT. Discarding duplicate Kafka message.",
                    smsContent.getNotificationId());
            return;
        }

        SendSmsResponse response = vendorClient.sendSmsWithResilience(smsContent);
        txManager.updateNotificationStateAndLog(smsContent.getNotificationId(), response.getMessage());
    }

    public void handlePermanentFailure(Long notificationId, String exceptionMessage) {
        txManager.markNotificationAsFailed(notificationId, exceptionMessage);
    }
}