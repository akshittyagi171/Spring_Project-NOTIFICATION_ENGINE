package com.notificationengine.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendSmsResponse {
    private int status;
    private String message;
    private String vendorMessageSid;
    private String vendorInitialStatus;

    public SendSmsResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}