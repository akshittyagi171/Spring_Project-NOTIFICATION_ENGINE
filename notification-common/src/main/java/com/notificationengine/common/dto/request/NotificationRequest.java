package com.notificationengine.common.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Integer notificationPriority;
    private List<String> channels;
    private List<RecipientRequest> recipients;
    private ContentRequest content;
    private String idempotencyKey;
}
