package com.notificationengine.notificationservice.dto.request;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String name;
    private String email;
    private String phone;
    private String fcmToken;
}
