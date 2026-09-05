package com.notificationengine.EmailConsumer.service;

import com.notificationengine.common.dto.content.EmailContent;
import com.notificationengine.common.dto.response.SendEmailResponse;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.notificationengine.EmailConsumer.constants.Constants.PARTIAL_DELIVERY_STATUS;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSender {

    private final SendGrid sendGridClient;
    private final RestTemplate restTemplate;

    @Value("${spring.sendgrid.from-email}")
    private String fromEmailAddress;

    public SendEmailResponse sendEmail(EmailContent emailContent) {
        log.info("Initiating SendGrid processing block context for Notification ID: {}", emailContent.getNotificationId());

        Mail mail = getMail(emailContent);

        List<String> failedAttachments = new ArrayList<>();
        List<EmailContent.EmailAttachment> attachments = emailContent.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            for (int i = 0; i < attachments.size(); i++) {
                EmailContent.EmailAttachment attachmentDto = attachments.get(i);
                if (attachmentDto.getUrl() != null && !attachmentDto.getUrl().trim().isEmpty()) {
                    boolean attached = attachDynamicUrlAsset(mail, attachmentDto, i);
                    if (!attached) {
                        failedAttachments.add(attachmentDto.getUrl());
                    }
                }
            }
        }

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGridClient.api(request);

            log.info("SendGrid Dispatch Logged - Status: {}, NotificationId: {}", response.getStatusCode(), emailContent.getNotificationId());

            String vendorMessageId = extractHeaderCaseInsensitive(response.getHeaders(), "X-Message-Id");

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                if (!failedAttachments.isEmpty()) {
                    String message = String.format(
                            "Email sent but %d attachment(s) failed to download and were omitted: %s",
                            failedAttachments.size(), String.join(", ", failedAttachments));
                    log.error("PARTIAL DELIVERY for notification ID {}: {}", emailContent.getNotificationId(), message);
                    SendEmailResponse partial = new SendEmailResponse(PARTIAL_DELIVERY_STATUS, message);
                    partial.setVendorMessageSid(vendorMessageId);
                    return partial;
                }
                SendEmailResponse success = new SendEmailResponse(response.getStatusCode(), "Email accepted via SendGrid.");
                success.setVendorMessageSid(vendorMessageId);
                return success;
            } else {
                return new SendEmailResponse(response.getStatusCode(), response.getBody());
            }
        } catch (IOException ex) {
            log.error("Fatal delivery crash occurred at SendGrid interface integration layer: ", ex);
            return new SendEmailResponse(500, "SendGrid SDK integration runtime collapse: " + ex.getMessage());
        }
    }

    private Mail getMail(EmailContent emailContent) {
        Email from = new Email(fromEmailAddress);
        String subject = emailContent.getSubject();
        Email to = new Email(emailContent.getEmailId());
        Content content = new Content("text/html", emailContent.getMessage());

        Personalization personalization = new Personalization();
        personalization.addTo(to);
        personalization.addCustomArg("notificationId", String.valueOf(emailContent.getNotificationId()));

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(subject);
        mail.addContent(content);
        mail.addPersonalization(personalization);
        return mail;
    }

    private String extractHeaderCaseInsensitive(Map<String, String> headers, String headerName) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean attachDynamicUrlAsset(Mail mail, EmailContent.EmailAttachment attachmentDto, int index) {
        try {
            log.info("Downloading external notification attachment asset resource from: {}", attachmentDto.getUrl());

            ResponseEntity<byte[]> response = restTemplate.getForEntity(attachmentDto.getUrl(), byte[].class);
            byte[] fileBytes = response.getBody();

            if (fileBytes == null || fileBytes.length == 0) {
                log.error("Attachment download returned empty body for URL: {}", attachmentDto.getUrl());
                return false;
            }

            String base64Content = Base64.getEncoder().encodeToString(fileBytes);

            String contentType = attachmentDto.getType();
            if (contentType == null || contentType.isEmpty()) {
                if (response.getHeaders().getContentType() != null) {
                    contentType = response.getHeaders().getContentType().toString().split(";")[0].trim();
                }
            }
            if (contentType == null || contentType.isEmpty() || contentType.equals("application/octet-stream")) {
                contentType = determineMimeTypeFromUrl(attachmentDto.getUrl());
            }

            String filename = attachmentDto.getFilename();
            if (filename == null || filename.isBlank()) {
                String extension = determineExtensionFromMimeType(contentType);
                filename = "attachment_" + (index + 1) + "." + extension;
            }

            Attachments sgAttachment = new Attachments();
            sgAttachment.setContent(base64Content);
            sgAttachment.setType(contentType);
            sgAttachment.setFilename(filename);
            sgAttachment.setDisposition("attachment");

            mail.addAttachments(sgAttachment);
            log.info("Successfully bound downloaded binary asset to SendGrid envelope. Filename: {}, Type: {}", filename, contentType);
            return true;

        } catch (Exception ex) {
            log.error("Unable to attach asset source from metadata URL link: {}", attachmentDto.getUrl(), ex);
            return false;
        }
    }

    private String determineExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return "dat";
        return switch (mimeType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/gif" -> "gif";
            case "application/pdf" -> "pdf";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            case "text/plain" -> "txt";
            case "text/html" -> "html";
            default -> "dat";
        };
    }

    private String determineMimeTypeFromUrl(String fileUrl) {
        try {
            String cleanPath = fileUrl.split("\\?")[0].toLowerCase();
            if (cleanPath.endsWith(".png")) return "image/png";
            if (cleanPath.endsWith(".jpg") || cleanPath.endsWith(".jpeg")) return "image/jpeg";
            if (cleanPath.endsWith(".pdf")) return "application/pdf";
            if (cleanPath.endsWith(".gif")) return "image/gif";
        } catch (Exception e) {
            log.warn("Failed parsing extension directly from raw URL string pattern mapping.");
        }
        return "application/octet-stream";
    }
}