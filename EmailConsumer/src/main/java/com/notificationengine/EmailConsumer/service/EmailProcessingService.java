package com.notificationengine.EmailConsumer.service;

import com.notificationengine.EmailConsumer.models.EmailContent;
import com.notificationengine.EmailConsumer.models.SendEmailResponse;
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
        SendEmailResponse response = vendorClient.sendEmailWithResilience(emailContent);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            txManager.updateNotificationStateAndLog(emailContent.getNotificationId());
        } else {
            log.error("Notification ID {} explicitly rejected by provider gateway. Status: {}, Message: {}",
                    emailContent.getNotificationId(), response.getStatus(), response.getMessage());

            throw new RuntimeException("Outbound delivery failed with status code: " + response.getStatus());
        }
    }

    public void handlePermanentFailure(Long notificationId, String exceptionMessage) {
        txManager.markNotificationAsFailed(notificationId, exceptionMessage);
    }
}