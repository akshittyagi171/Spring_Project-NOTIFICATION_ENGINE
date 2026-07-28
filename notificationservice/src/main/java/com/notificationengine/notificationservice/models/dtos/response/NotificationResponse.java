package com.notificationengine.notificationservice.models.dtos.response;

import com.notificationengine.notificationservice.models.dtos.request.ContentRequest;
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

    // You can return the full ContentRequest object here,
    // or create a simplified summary object depending on how much data you want to echo.
    private ContentRequest contentSent;
}
