package com.notificationengine.PushNConsumer.service;

import com.notificationengine.PushNConsumer.models.SendPushNResponse;
import com.notificationengine.PushNConsumer.models.PushNRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNProcessingService {

    private final ResilientPushNVendorClient vendorClient;
    private final PushNNotificationTxManager txManager;

    public void processPushN(PushNRequest pushNRequest) {
        SendPushNResponse response = vendorClient.sendWhatsAppWithResilience(pushNRequest);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            txManager.updateNotificationStateAndLog(pushNRequest.getNotificationId());
        } else {
            log.error("Notification ID {} explicitly rejected by provider gateway. Status: {}, Message: {}",
                    pushNRequest.getNotificationId(), response.getStatus(), response.getMessage());

            throw new RuntimeException("Outbound delivery failed with status code: " + response.getStatus());
        }
    }

    public void handlePermanentFailure(Long notificationId, String exceptionMessage) {
        txManager.markNotificationAsFailed(notificationId, exceptionMessage);
    }
}