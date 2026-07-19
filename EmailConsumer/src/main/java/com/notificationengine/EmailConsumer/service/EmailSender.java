package com.notificationengine.EmailConsumer.service;

import com.notificationengine.EmailConsumer.models.EmailRequest;
import com.notificationengine.EmailConsumer.models.SendEmailResponse;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSender {

    private final SendGrid sendGridClient;
    private final RestTemplate restTemplate;
    private final EmailTemplateEngine emailTemplateEngine;

    @Value("${spring.sendgrid.from-email}")
    private String fromEmailAddress;

    public SendEmailResponse sendEmail(EmailRequest emailRequest) {
        log.info("Initiating SendGrid processing block context for Notification ID: {}", emailRequest.getNotificationId());

        Email from = new Email(fromEmailAddress);
        String subject = emailRequest.getEmailSubject();
        Email to = new Email(emailRequest.getEmailId());

        String htmlBody = emailTemplateEngine.buildHtmlTemplate(emailRequest.getMessage(), subject);
        Content content = new Content("text/html", htmlBody);

        Mail mail = new Mail(from, subject, to, content);

        if (emailRequest.getEmailAttachments() != null && emailRequest.getEmailAttachments().length > 0) {
            for (int i = 0; i < emailRequest.getEmailAttachments().length; i++) {
                String assetUrl = emailRequest.getEmailAttachments()[i];
                if (assetUrl != null && !assetUrl.trim().isEmpty()) {
                    attachDynamicUrlAsset(mail, assetUrl, i);
                }
            }
        }

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGridClient.api(request);

            log.info("SendGrid Dispatch Logged - Status: {}, NotificationId: {}", response.getStatusCode(), emailRequest.getNotificationId());

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                return new SendEmailResponse(response.getStatusCode(), "Email accepted via SendGrid.");
            } else {
                return new SendEmailResponse(response.getStatusCode(), response.getBody());
            }
        } catch (IOException ex) {
            log.error("Fatal delivery crash occurred at SendGrid interface integration layer: ", ex);
            return new SendEmailResponse(500, "SendGrid SDK integration runtime collapse: " + ex.getMessage());
        }
    }

    private void attachDynamicUrlAsset(Mail mail, String fileUrl, int index) {
        try {
            log.info("Downloading external notification attachment asset resource from: {}", fileUrl);

            ResponseEntity<byte[]> response = restTemplate.getForEntity(fileUrl, byte[].class);
            byte[] fileBytes = response.getBody();

            if (fileBytes != null && fileBytes.length > 0) {
                String base64Content = Base64.getEncoder().encodeToString(fileBytes);

                String contentType = null;
                if (response.getHeaders().getContentType() != null) {
                    contentType = response.getHeaders().getContentType().toString();
                    contentType = contentType.split(";")[0].trim();
                }

                if (contentType == null || contentType.isEmpty() || contentType.equals("application/octet-stream")) {
                    contentType = determineMimeTypeFromUrl(fileUrl);
                }

                String extension = determineExtensionFromMimeType(contentType);

                Attachments attachment = new Attachments();
                attachment.setContent(base64Content);
                attachment.setType(contentType);
                attachment.setFilename("attachment_" + (index + 1) + "." + extension);
                attachment.setDisposition("attachment");

                mail.addAttachments(attachment);
                log.info("Successfully bound downloaded binary asset to SendGrid envelope. Type: {}, Ext: {}", contentType, extension);
            }
        } catch (Exception ex) {
            log.error("Resilient fallback isolated: Unable to attach asset source from metadata URL link: {}", fileUrl, ex);
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
            case "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                    "xlsx";
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