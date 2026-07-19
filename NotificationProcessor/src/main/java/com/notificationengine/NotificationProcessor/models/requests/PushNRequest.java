package com.notificationengine.NotificationProcessor.models.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushNRequest {
    private String title;
    private String message;
    private String action;
    private Long notificationId;
    private String fcmToken;

    public PushNRequest(String title, String message, String action, String fcmToken){
        this.title = title;
        this.message = message;
        this.action = action;
        this.fcmToken = fcmToken;
    }
}
