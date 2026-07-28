package com.notificationengine.EmailConsumer.service;

import com.notificationengine.EmailConsumer.models.EmailContent;
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
import java.util.List;

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

        Email from = new Email(fromEmailAddress);
        String subject = emailContent.getSubject();
        Email to = new Email(emailContent.getEmailId());

        // The 'message' now directly contains the fully processed HTML from the database template
        Content content = new Content("text/html", emailContent.getMessage());

        Mail mail = new Mail(from, subject, to, content);

        // Process attachments using the new List of Objects
        List<EmailContent.EmailAttachment> attachments = emailContent.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            for (int i = 0; i < attachments.size(); i++) {
                EmailContent.EmailAttachment attachmentDto = attachments.get(i);
                if (attachmentDto.getUrl() != null && !attachmentDto.getUrl().trim().isEmpty()) {
                    attachDynamicUrlAsset(mail, attachmentDto, i);
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

    private void attachDynamicUrlAsset(Mail mail, EmailContent.EmailAttachment attachmentDto, int index) {
        try {
            log.info("Downloading external notification attachment asset resource from: {}", attachmentDto.getUrl());

            ResponseEntity<byte[]> response = restTemplate.getForEntity(attachmentDto.getUrl(), byte[].class);
            byte[] fileBytes = response.getBody();

            if (fileBytes != null && fileBytes.length > 0) {
                String base64Content = Base64.getEncoder().encodeToString(fileBytes);

                // Prioritize the type provided in the DTO, fallback to HTTP headers, fallback to URL guessing
                String contentType = attachmentDto.getType();
                if (contentType == null || contentType.isEmpty()) {
                    if (response.getHeaders().getContentType() != null) {
                        contentType = response.getHeaders().getContentType().toString().split(";")[0].trim();
                    }
                }

                if (contentType == null || contentType.isEmpty() || contentType.equals("application/octet-stream")) {
                    contentType = determineMimeTypeFromUrl(attachmentDto.getUrl());
                }

                // Prioritize filename from DTO, fallback to generated name
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
            }
        } catch (Exception ex) {
            log.error("Resilient fallback isolated: Unable to attach asset source from metadata URL link: {}", attachmentDto.getUrl(), ex);
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