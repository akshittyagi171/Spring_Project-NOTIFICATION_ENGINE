package com.notificationengine.SMSConsumer.service;

import com.notificationengine.SMSConsumer.models.SendSmsResponse;
import com.notificationengine.SMSConsumer.models.SmsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsProcessingService {

    private final ResilientSmsVendorClient vendorClient;
    private final SmsNotificationTxManager txManager;

    public void processSms(SmsRequest smsRequest) {
        SendSmsResponse response = vendorClient.sendSmsWithResilience(smsRequest);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            txManager.updateNotificationStateAndLog(smsRequest.getNotificationId());
        } else {
            log.error("Notification ID {} explicitly rejected by provider gateway. Status: {}, Message: {}",
                    smsRequest.getNotificationId(), response.getStatus(), response.getMessage());

            throw new RuntimeException("Outbound delivery failed with status code: " + response.getStatus());
        }
    }

    public void handlePermanentFailure(Long notificationId, String exceptionMessage) {
        txManager.markNotificationAsFailed(notificationId, exceptionMessage);
    }
}