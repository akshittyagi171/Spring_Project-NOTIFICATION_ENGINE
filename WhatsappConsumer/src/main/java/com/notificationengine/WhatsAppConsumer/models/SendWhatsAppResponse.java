package com.notificationengine.WhatsAppConsumer.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendWhatsAppResponse {
    private int status;
    private String message;
}
