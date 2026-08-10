package com.notificationengine.notificationservice.dto.response;

import com.notificationengine.notificationservice.dto.request.ContentRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String status;
    private String message;
    private List<RecipientResponse> processedRecipients;
    private ContentRequest contentSent;
}
