package com.notificationengine.NotificationProcessor.models.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppRequest {
    private String mobileNumber;
    private String message;
    private Long notificationId;
    private String templateName;
    private Map<String, String> placeholders;
    private List<String> mediaUrls;
}
