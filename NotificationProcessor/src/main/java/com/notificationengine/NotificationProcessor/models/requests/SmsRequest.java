package com.notificationengine.NotificationProcessor.models.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsRequest {
    private String mobileNumber;
    private String message;
    private Long notificationId;

    public SmsRequest(String mobileNumber, String message){
        this.mobileNumber= mobileNumber;
        this.message = message;
    }
}
