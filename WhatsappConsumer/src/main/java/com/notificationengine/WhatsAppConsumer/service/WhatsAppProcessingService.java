package com.notificationengine.WhatsAppConsumer.service;

import com.notificationengine.WhatsAppConsumer.models.SendWhatsAppResponse;
import com.notificationengine.WhatsAppConsumer.models.WhatsAppContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppProcessingService {

    private final ResilientWhatsAppVendorClient vendorClient;
    private final WhatsAppNotificationTxManager txManager;

    public void processWhatsApp(WhatsAppContent whatsAppContent) {
        SendWhatsAppResponse response = vendorClient.sendWhatsAppWithResilience(whatsAppContent);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            txManager.updateNotificationStateAndLog(whatsAppContent.getNotificationId());
        } else {
            log.error("Notification ID {} explicitly rejected by provider gateway. Status: {}, Message: {}",
                    whatsAppContent.getNotificationId(), response.getStatus(), response.getMessage());

            throw new RuntimeException("Outbound delivery failed with status code: " + response.getStatus());
        }
    }

    public void handlePermanentFailure(Long notificationId, String exceptionMessage) {
        txManager.markNotificationAsFailed(notificationId, exceptionMessage);
    }
}