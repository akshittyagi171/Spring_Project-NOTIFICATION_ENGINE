package com.notificationengine.common.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppContent {
    private Long notificationId;
    private String mobileNumber;
    private String templateName;
    private Map<String, String> placeholders;
    private String message;
    private List<WhatsappAttachment> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhatsappAttachment {
        private String type;
        private String url;
        private String caption;
    }
}
