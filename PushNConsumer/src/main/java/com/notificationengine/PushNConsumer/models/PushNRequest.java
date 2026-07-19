package com.notificationengine.PushNConsumer.models;

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
}
