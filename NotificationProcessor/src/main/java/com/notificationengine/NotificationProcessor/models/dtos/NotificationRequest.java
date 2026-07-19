package com.notificationengine.NotificationProcessor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private int notificationPriority;
    private String[] channels;
    private Recipient recipient;
    private Content content;
}
