package com.notificationengine.notificationservice.models.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRequest {
    private EmailRequest email;
    private WhatsappRequest whatsapp;
    private PushRequest push;
    private SmsRequest sms;
}
