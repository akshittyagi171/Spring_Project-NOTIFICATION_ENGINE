package com.notificationengine.NotificationProcessor.models.dtos.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    private EmailContent email;
    private WhatsappContent whatsapp;
    private PushContent push;
    private SmsContent sms;
}
