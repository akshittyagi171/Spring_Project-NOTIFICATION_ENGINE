package com.notificationengine.notificationservice.models.dtos.request;

import lombok.Data;

@Data
public class TemplateRequest {
    private String name;
    private String content;
    private String placeholders;
    private int templatePriority;
}
