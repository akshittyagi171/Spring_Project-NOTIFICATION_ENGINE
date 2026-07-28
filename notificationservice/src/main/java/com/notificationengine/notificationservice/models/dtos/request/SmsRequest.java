package com.notificationengine.notificationservice.models.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsRequest {
    private String templateName;
    private Map<String, String> placeholders;
    private String message;
}
