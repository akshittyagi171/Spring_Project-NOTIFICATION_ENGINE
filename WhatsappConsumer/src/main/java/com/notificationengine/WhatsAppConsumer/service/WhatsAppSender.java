package com.notificationengine.WhatsAppConsumer.service;

import com.notificationengine.WhatsAppConsumer.config.TwilioConfig;
import com.notificationengine.WhatsAppConsumer.models.SendWhatsAppResponse;
import com.notificationengine.WhatsAppConsumer.models.WhatsAppContent;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.notificationengine.WhatsAppConsumer.constants.Constants.PARTIAL_DELIVERY_STATUS;

@Service
@Slf4j
public class WhatsAppSender {

    private final TwilioConfig twilioConfig;

    public WhatsAppSender(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    public SendWhatsAppResponse sendWhatsApp(WhatsAppContent whatsAppContent) {

        String toAddress = "whatsapp:" + whatsAppContent.getMobileNumber().trim();
        String fromAddress = "whatsapp:" + twilioConfig.getFromNumber();
        List<String> sentSids = new ArrayList<>();

        List<URI> mediaUris = new ArrayList<>();
        if (whatsAppContent.getAttachments() != null && !whatsAppContent.getAttachments().isEmpty()) {
            for (WhatsAppContent.WhatsappAttachment attachment : whatsAppContent.getAttachments()) {
                if (attachment.getUrl() != null && !attachment.getUrl().isBlank()) {
                    mediaUris.add(URI.create(attachment.getUrl().trim()));
                }
            }
        }

        try {
            log.info("Preparing production-ready sequential WhatsApp notification for ID: {}. Target: {}",
                    whatsAppContent.getNotificationId(), toAddress);

            Message lastMessage;

            if (mediaUris.isEmpty()) {
                lastMessage = Message.creator(
                        new PhoneNumber(toAddress), new PhoneNumber(fromAddress), whatsAppContent.getMessage()
                ).create();
                sentSids.add(lastMessage.getSid());
            } else {
                lastMessage = Message.creator(
                        new PhoneNumber(toAddress), new PhoneNumber(fromAddress), whatsAppContent.getMessage()
                ).setMediaUrl(List.of(mediaUris.get(0))).create();
                sentSids.add(lastMessage.getSid());
                log.info("Dispatched Message 1 (Body + Media 1). SID: {}", lastMessage.getSid());

                for (int i = 1; i < mediaUris.size(); i++) {
                    Message subsequentMessage = Message.creator(
                            new PhoneNumber(toAddress), new PhoneNumber(fromAddress), ""
                    ).setMediaUrl(List.of(mediaUris.get(i))).create();

                    sentSids.add(subsequentMessage.getSid());
                    lastMessage = subsequentMessage;
                    log.info("Dispatched Message {} (Media only). SID: {}", i + 1, subsequentMessage.getSid());
                }
            }

            log.info("WhatsApp Dispatch Complete! (Notification Id: {}). Final Status: {}, Twilio Message SIDs: [{}]",
                    whatsAppContent.getNotificationId(), lastMessage.getStatus(), String.join(", ", sentSids));

            return new SendWhatsAppResponse(200, "Sids: [" + String.join(", ", sentSids) + "]");

        } catch (ApiException apiException) {
            if (!sentSids.isEmpty()) {
                return partialFailureResponse(whatsAppContent, sentSids, mediaUris.size(), apiException.getMessage());
            }

            Integer statusCode = apiException.getStatusCode();
            log.error("Twilio rejected notification ID {}: [{}] {}",
                    whatsAppContent.getNotificationId(), statusCode, apiException.getMessage());

            if (statusCode != null && statusCode >= 400 && statusCode < 500) {
                return new SendWhatsAppResponse(statusCode, "Twilio rejected the request: " + apiException.getMessage());
            }
            return new SendWhatsAppResponse(502, "Twilio vendor error: " + apiException.getMessage());

        } catch (Exception exception) {
            if (!sentSids.isEmpty()) {
                return partialFailureResponse(whatsAppContent, sentSids, mediaUris.size(), exception.getMessage());
            }

            log.error("Fatal exception captured inside Twilio sequential media dispatch pipeline: {}", exception.toString());
            return new SendWhatsAppResponse(500, "Exception occurred in WhatsApp Rich Media Engine: " + exception.getMessage());
        }
    }

    private SendWhatsAppResponse partialFailureResponse(WhatsAppContent whatsAppContent, List<String> sentSids,
                                                        int totalIntended, String failureReason) {
        String message = String.format(
                "Partial delivery: %d of %d messages sent before failure. Sent SIDs: [%s]. Failure: %s",
                sentSids.size(), totalIntended, String.join(", ", sentSids), failureReason);

        log.error("PARTIAL DELIVERY for notification ID {}: {}", whatsAppContent.getNotificationId(), message);
        return new SendWhatsAppResponse(PARTIAL_DELIVERY_STATUS, message);
    }
}