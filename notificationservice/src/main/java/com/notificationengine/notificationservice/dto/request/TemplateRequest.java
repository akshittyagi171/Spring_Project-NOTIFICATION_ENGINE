package com.notificationengine.notificationservice.dto.request;

import lombok.Data;

@Data
public class TemplateRequest {
    private String name;
    private String content;
    private String placeholders;
    private int templatePriority;
}
