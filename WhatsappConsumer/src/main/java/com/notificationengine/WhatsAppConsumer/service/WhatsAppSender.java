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
            String fromAddress = "whatsapp:" + twilioConfig.getFromNumber();

            log.info("Preparing production-ready sequential WhatsApp notification for ID: {}. Target: {}",
                    whatsAppContent.getNotificationId(), toAddress);

            List<URI> mediaUris = new ArrayList<>();
            if (whatsAppContent.getAttachments() != null && !whatsAppContent.getAttachments().isEmpty()) {
                for (WhatsAppContent.WhatsappAttachment attachment : whatsAppContent.getAttachments()) {
                    if (attachment.getUrl() != null && !attachment.getUrl().isBlank()) {
                        mediaUris.add(URI.create(attachment.getUrl().trim()));
                    }
                }
            }

            Message lastMessage = null;
            StringBuilder sids = new StringBuilder();

            if (mediaUris.isEmpty()) {
                lastMessage = Message.creator(
                        new PhoneNumber(toAddress),
                        new PhoneNumber(fromAddress),
                        whatsAppContent.getMessage()
                ).create();

                sids.append(lastMessage.getSid());
            } else {
                lastMessage = Message.creator(
                        new PhoneNumber(toAddress),
                        new PhoneNumber(fromAddress),
                        whatsAppContent.getMessage()
                ).setMediaUrl(List.of(mediaUris.get(0))).create();

                sids.append(lastMessage.getSid());
                log.info("Dispatched Message 1 (Body + Media 1). SID: {}", lastMessage.getSid());

                for (int i = 1; i < mediaUris.size(); i++) {
                    Message subsequentMessage = Message.creator(
                            new PhoneNumber(toAddress),
                            new PhoneNumber(fromAddress),
                            ""
                    ).setMediaUrl(List.of(mediaUris.get(i))).create();

                    sids.append(", ").append(subsequentMessage.getSid());
                    lastMessage = subsequentMessage;
                    log.info("Dispatched Message {} (Media only). SID: {}", i + 1, subsequentMessage.getSid());
                }
            }

            log.info("WhatsApp Dispatch Complete! (Notification Id: {}). Final Status: {}, Twilio Message SIDs: [{}]",
                    whatsAppContent.getNotificationId(), lastMessage.getStatus(), sids.toString());

            return new SendWhatsAppResponse(200, "Sids: [" + sids.toString() + "]");

        } catch (Exception exception) {
            log.error("Fatal exception captured inside Twilio sequential media dispatch pipeline: {}", exception.toString());
            return new SendWhatsAppResponse(500, "Exception occurred in WhatsApp Rich Media Engine: " + exception.getMessage());
        }
    }
}