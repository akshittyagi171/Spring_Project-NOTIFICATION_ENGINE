package com.notificationengine.common.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsContent {
    private Long notificationId;
    private String mobileNumber;
    private String templateName;
    private Map<String, String> placeholders;
    private String message;
}
