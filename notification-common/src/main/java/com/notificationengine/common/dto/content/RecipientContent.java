package com.notificationengine.common.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientContent {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String fcmToken;
}