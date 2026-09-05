package com.notificationengine.PushNConsumer.service;

import com.notificationengine.common.dto.content.PushContent;
import com.notificationengine.common.dto.response.SendPushNResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNProcessingService {

    private final ResilientPushNVendorClient vendorClient;
    private final PushNNotificationTxManager txManager;

    public void processPushN(PushContent pushContent) {
        if (txManager.isAlreadyProcessed(pushContent.getNotificationId())) {
            log.warn("Idempotency hit: Notification ID {} is already marked as SENT. Discarding duplicate Kafka message.",
                    pushContent.getNotificationId());
            return;
        }

        SendPushNResponse response = vendorClient.sendPushNWithResilience(pushContent);
        txManager.updateNotificationStateAndLog(
                pushContent.getNotificationId(), response.getVendorMessageSid(), response.getMessage());
    }

    public void handlePermanentFailure(Long notificationId, String exceptionMessage) {
        txManager.markNotificationAsFailed(notificationId, exceptionMessage);
    }
}