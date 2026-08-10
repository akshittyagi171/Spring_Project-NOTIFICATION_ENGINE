package com.notificationengine.notificationservice.models.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCreateResponse {
    private Long id;
    private String email;
    private String status;
}
