package com.notificationengine.notificationservice.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationengine.notificationservice.config.SendGridWebhookConfig;
import com.notificationengine.notificationservice.service.SendGridEmailDeliveryStatusService;
import com.sendgrid.helpers.eventwebhook.EventWebhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SendGridWebhookController {

    private final SendGridEmailDeliveryStatusService deliveryStatusService;
    private final SendGridWebhookConfig sendGridWebhookConfig;
    private final ObjectMapper objectMapper;
    private final EventWebhook eventWebhook = new EventWebhook();

    @PostMapping(value = "/api/webhooks/sendgrid/email-status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleEmailStatusCallback(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Twilio-Email-Event-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Twilio-Email-Event-Webhook-Timestamp", required = false) String timestamp) {

        if (signature == null || timestamp == null) {
            log.warn("Rejected SendGrid webhook: missing signature/timestamp header");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean signatureValid;
        try {
            ECPublicKey publicKey = eventWebhook.ConvertPublicKeyToECDSA(sendGridWebhookConfig.getVerificationKey());
            signatureValid = eventWebhook.VerifySignature(publicKey, rawPayload, signature, timestamp);
        } catch (Exception e) {
            log.error("Failed to verify SendGrid webhook signature", e);
            signatureValid = false;
        }

        if (!signatureValid) {
            log.warn("Rejected SendGrid webhook: invalid signature");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Map<String, Object>> events;
        try {
            events = objectMapper.readValue(rawPayload, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to parse SendGrid webhook payload", e);
            return ResponseEntity.badRequest().build();
        }

        for (Map<String, Object> event : events) {
            try {
                deliveryStatusService.applyEvent(event);
            } catch (Exception e) {
                log.error("Failed to apply SendGrid event, skipping just this one: {}", event, e);
            }
        }

        return ResponseEntity.ok().build();
    }
}