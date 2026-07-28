package com.notificationengine.WhatsAppConsumer.service;

import com.notificationengine.WhatsAppConsumer.config.TwilioConfig;
import com.notificationengine.WhatsAppConsumer.models.SendWhatsAppResponse;
import com.notificationengine.WhatsAppConsumer.models.WhatsAppContent;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WhatsAppSender {

    private final TwilioConfig twilioConfig;

    public WhatsAppSender(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    public SendWhatsAppResponse sendWhatsApp(WhatsAppContent whatsAppContent) {

        try {
            String targetMobile = whatsAppContent.getMobileNumber().trim();
            String toAddress = "whatsapp:" + targetMobile;

            log.info("Preparing production-ready rich WhatsApp notification for ID: {}. Target: {}",
                    whatsAppContent.getNotificationId(), toAddress);

            var messageCreator = Message.creator(
                    new PhoneNumber(toAddress),
                    new PhoneNumber("whatsapp:" + twilioConfig.getFromNumber()),
                    whatsAppContent.getMessage()
            );

            if (whatsAppContent.getAttachments() != null && !whatsAppContent.getAttachments().isEmpty()) {
                List<URI> mediaUris = new ArrayList<>();
                for (WhatsAppContent.WhatsappAttachment attachment : whatsAppContent.getAttachments()) {
                    if (attachment.getUrl() != null && !attachment.getUrl().isBlank()) {
                        mediaUris.add(URI.create(attachment.getUrl().trim()));
                    }
                }

                if (!mediaUris.isEmpty()) {
                    messageCreator.setMediaUrl(mediaUris);
                    log.info("Successfully bound {} media URI assets to the WhatsApp payload transaction channel.", mediaUris.size());
                }
            }

            Message message = messageCreator.create();

            log.info("WhatsApp Dispatch Successful! (Notification Id: {}). Response Status: {}, Twilio Message SID: {}",
                    whatsAppContent.getNotificationId(), message.getStatus(), message.getSid());

            return new SendWhatsAppResponse(200, "Sid: " + message.getSid() + " Body: " + message.getBody());

        } catch (Exception exception) {
            log.error("Fatal exception captured inside Twilio media message construction pipeline: {}", exception.toString());
            return new SendWhatsAppResponse(500, "Exception occurred in WhatsApp Rich Media Engine: " + exception.getMessage());
        }
    }
}