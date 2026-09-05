package com.notificationengine.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendPushNResponse {
    private int status;
    private String message;
    private String vendorMessageSid;

    public SendPushNResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}