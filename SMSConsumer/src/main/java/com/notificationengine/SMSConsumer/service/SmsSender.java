package com.notificationengine.SMSConsumer.service;

import com.notificationengine.SMSConsumer.config.TwilioConfig;
import com.notificationengine.SMSConsumer.models.SendSmsResponse;
import com.notificationengine.SMSConsumer.models.SmsRequest;
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

    public SendSmsResponse sendSms(SmsRequest smsRequest) {

        try {
            String targetMobile = smsRequest.getMobileNumber().trim();
            log.info("Preparing production-ready rich SMS notification for ID: {}. Target: {}",
                    smsRequest.getNotificationId(), targetMobile);

            var messageCreator = Message.creator(
                    new PhoneNumber(targetMobile),
                    new PhoneNumber(twilioConfig.getFromNumber()),
                    smsRequest.getMessage()
            );

            Message message = messageCreator.create();

            log.info("SMS Dispatch Successful! (Notification Id: {}). Response Status: {}, Twilio Message SID: {}",
                    smsRequest.getNotificationId(), message.getStatus(), message.getSid());

            return new SendSmsResponse(200, "Sid: " + message.getSid() + " Body: " + message.getBody());

        } catch (Exception exception) {
            log.error("Fatal exception captured inside Twilio media message construction pipeline: {}", exception.toString());
            return new SendSmsResponse(500, "Exception occurred in WhatsApp Rich Media Engine: " + exception.getMessage());
        }
    }
}
