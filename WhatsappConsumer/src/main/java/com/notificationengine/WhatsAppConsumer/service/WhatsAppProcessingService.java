package com.notificationengine.WhatsAppConsumer.service;

import com.notificationengine.WhatsAppConsumer.models.SendWhatsAppResponse;
import com.notificationengine.WhatsAppConsumer.models.WhatsAppContent;
import com.notificationengine.WhatsAppConsumer.service.exceptions.FatalVendorException;
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
        if (txManager.isAlreadyProcessed(whatsAppContent.getNotificationId())) {
            log.warn("Idempotency hit: Notification ID {} is already marked as SENT. Discarding duplicate Kafka message.",
                    whatsAppContent.getNotificationId());
            return;
        }

        SendWhatsAppResponse response = vendorClient.sendWhatsAppWithResilience(whatsAppContent);
        txManager.updateNotificationStateAndLog(whatsAppContent.getNotificationId(), response.getMessage());
    }

    public void handlePermanentFailure(Long notificationId, String exceptionMessage) {
        txManager.markNotificationAsFailed(notificationId, exceptionMessage);
    }
}