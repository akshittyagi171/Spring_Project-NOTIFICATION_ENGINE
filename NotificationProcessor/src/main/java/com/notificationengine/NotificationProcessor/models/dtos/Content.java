package com.notificationengine.NotificationProcessor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    private boolean usingTemplates;
    private String templateName;
    private Map<String,String> placeholders;
    private String message;
    private String emailSubject;
    private String[] emailAttachments;
    private PushNotification pushNotification;
}
