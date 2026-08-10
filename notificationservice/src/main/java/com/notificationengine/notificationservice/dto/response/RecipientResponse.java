package com.notificationengine.notificationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientResponse {
    private String userId;
    private boolean isNewUser;
    private String name;
    private String email;
    private String phone;
}
