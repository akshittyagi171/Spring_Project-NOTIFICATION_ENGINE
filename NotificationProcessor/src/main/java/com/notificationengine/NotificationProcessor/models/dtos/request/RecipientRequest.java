package com.notificationengine.NotificationProcessor.models.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientRequest {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String fcmToken;
}