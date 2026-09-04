package com.notificationengine.notificationservice.controllers;

import com.notificationengine.common.enums.Channel;
import com.notificationengine.notificationservice.config.TwilioWebhookConfig;
import com.notificationengine.notificationservice.service.TwilioMessageDeliveryStatusService;
import com.twilio.security.RequestValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Receives Twilio's asynchronous delivery-status callbacks for channels that go through Twilio's
 * Message resource (WhatsApp and SMS). This is the piece the codebase previously had no way to
 * receive: the consumers only ever saw Twilio's *initial* "queued/accepted" response, and that
 * got reported upstream as delivery success. This is where the real, final outcome comes back.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TwilioWebhookController {

    private final TwilioMessageDeliveryStatusService deliveryStatusService;
    private final TwilioWebhookConfig twilioWebhookConfig;

    @PostMapping(value = "/api/webhooks/twilio/whatsapp-status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleWhatsAppStatusCallback(
            HttpServletRequest request,
            @RequestParam Long notificationId,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String twilioSignature) {
        return handleStatusCallback(Channel.whatsapp, twilioWebhookConfig.getWebhookBaseUrl(),
                request, notificationId, twilioSignature);
    }

    @PostMapping(value = "/api/webhooks/twilio/sms-status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleSmsStatusCallback(
            HttpServletRequest request,
            @RequestParam Long notificationId,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String twilioSignature) {
        return handleStatusCallback(Channel.sms, twilioWebhookConfig.getSmsWebhookBaseUrl(),
                request, notificationId, twilioSignature);
    }

    private ResponseEntity<Void> handleStatusCallback(Channel channel, String webhookBaseUrl,
                                                      HttpServletRequest request, Long notificationId,
                                                      String twilioSignature) {
        Set<String> queryParamNames = new HashSet<>();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isBlank()) {
            for (String pair : queryString.split("&")) {
                String key = pair.split("=", 2)[0];
                queryParamNames.add(URLDecoder.decode(key, StandardCharsets.UTF_8));
            }
        }

        Map<String, String> bodyParams = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (!queryParamNames.contains(key) && values != null && values.length > 0) {
                bodyParams.put(key, values[0]);
            }
        });

        String expectedCallbackUrl = webhookBaseUrl + "?notificationId=" + notificationId;

        boolean signatureValid = twilioSignature != null &&
                new RequestValidator(twilioWebhookConfig.getAuthToken())
                        .validate(expectedCallbackUrl, bodyParams, twilioSignature);

        if (!signatureValid) {
            log.warn("Rejected Twilio {} webhook for notificationId {}: missing/invalid X-Twilio-Signature",
                    channel, notificationId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        deliveryStatusService.applyStatusCallback(
                channel,
                bodyParams.get("MessageSid"),
                bodyParams.get("MessageStatus"),
                bodyParams.get("ErrorCode"),
                bodyParams.get("ErrorMessage"));

        return ResponseEntity.ok().build();
    }
}