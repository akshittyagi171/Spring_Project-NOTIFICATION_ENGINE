package com.notificationengine.SMSConsumer.service;

import com.notificationengine.SMSConsumer.config.TwilioConfig;
import com.notificationengine.SMSConsumer.models.SendSmsResponse;
import com.notificationengine.SMSConsumer.models.SmsContent;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsSender {

    private final TwilioConfig twilioConfig;

    public SmsSender(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    public SendSmsResponse sendSms(SmsContent smsContent) {

        try {
            String targetMobile = smsContent.getMobileNumber().trim();
            log.info("Preparing production-ready SMS notification for ID: {}. Target: {}",
                    smsContent.getNotificationId(), targetMobile);

            Message message = Message.creator(
                    new PhoneNumber(targetMobile),
                    new PhoneNumber(twilioConfig.getFromNumber()),
                    smsContent.getMessage()
            ).create();

            log.info("SMS Dispatch Successful! (Notification Id: {}). Response Status: {}, Twilio Message SID: {}",
                    smsContent.getNotificationId(), message.getStatus(), message.getSid());

            return new SendSmsResponse(200, "Sid: " + message.getSid() + " Body: " + message.getBody());

        } catch (ApiException apiException) {
            Integer statusCode = apiException.getStatusCode();
            log.error("Twilio rejected SMS notification ID {}: [{}] {}",
                    smsContent.getNotificationId(), statusCode, apiException.getMessage());

            if (statusCode != null && statusCode >= 400 && statusCode < 500) {
                return new SendSmsResponse(statusCode, "Twilio rejected the request: " + apiException.getMessage());
            }
            return new SendSmsResponse(502, "Twilio vendor error: " + apiException.getMessage());

        } catch (Exception exception) {
            log.error("Fatal exception captured inside Twilio SMS message construction pipeline: {}", exception.toString());
            return new SendSmsResponse(500, "Exception occurred in SMS Engine: " + exception.getMessage());
        }
    }
}