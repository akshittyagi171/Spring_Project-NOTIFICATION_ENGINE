package com.notificationengine.EmailConsumer.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String emailId;
    private String message;
    private String emailSubject;
    private String[] emailAttachments;
    private Long notificationId;

    public EmailRequest(String emailId, String message, String emailSubject, String[] emailAttachments){
        this.emailId = emailId;
        this.message = message;
        this.emailSubject = emailSubject;
        this.emailAttachments = emailAttachments;
    }
}
