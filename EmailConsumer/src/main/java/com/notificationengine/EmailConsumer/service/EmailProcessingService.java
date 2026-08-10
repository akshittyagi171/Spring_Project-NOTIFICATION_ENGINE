package com.notificationengine.EmailConsumer.service;

import com.notificationengine.common.dto.content.EmailContent;
import com.notificationengine.common.dto.response.SendEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProcessingService {

    private final ResilientEmailVendorClient vendorClient;
    private final EmailNotificationTxManager txManager;

    public void processEmail(EmailContent emailContent) {
        if (txManager.isAlreadyProcessed(emailContent.getNotificationId())) {
            log.warn("Idempotency hit: Notification ID {} is already marked as SENT. Discarding duplicate Kafka message.",
                    emailContent.getNotificationId());
            return;
        }

        SendEmailResponse response = vendorClient.sendEmailWithResilience(emailContent);
        txManager.updateNotificationStateAndLog(emailContent.getNotificationId(), response.getMessage());
    }

    public void handlePermanentFailure(Long notificationId, String exceptionMessage) {
        txManager.markNotificationAsFailed(notificationId, exceptionMessage);
    }
}