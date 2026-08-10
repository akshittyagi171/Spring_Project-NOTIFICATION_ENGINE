package com.notificationengine.common.dto.content;

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
    private WhatsAppContent whatsapp;
    private PushContent push;
    private SmsContent sms;
}
