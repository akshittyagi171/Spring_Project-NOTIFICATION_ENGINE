package com.notificationengine.notificationservice.models.dtos.request;

import com.notificationengine.common.enums.Channel;
import lombok.Data;

@Data
public class PreferenceUpdateRequest {
    private Channel channel;
    private Boolean isEnabled;
    private String allowedMessagesPriority;
    private String quietHours;
}
