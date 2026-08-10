package com.notificationengine.common.dto.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationContent {
    private Integer notificationPriority;
    private List<String> channels;
    private List<RecipientContent> recipients;
    private Content content;
}
