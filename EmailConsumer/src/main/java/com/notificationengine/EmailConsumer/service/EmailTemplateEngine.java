package com.notificationengine.EmailConsumer.service;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateEngine {

    public String buildHtmlTemplate(String rawMessage, String subject) {
        String bodyText = (rawMessage == null || rawMessage.trim().isEmpty())
                ? "Here is an exclusive update regarding your notification account state."
                : rawMessage;

        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "  <meta charset='utf-8'>"
                + "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "  <style>"
                + "    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 0; -webkit-font-smoothing: antialiased; }"
                + "    .wrapper { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }"
                + "    .header { background: linear-gradient(135deg, #4F46E5, #3730A3); padding: 30px; text-align: center; color: #ffffff; }"
                + "    .header h1 { margin: 0; font-size: 24px; font-weight: 600; letter-spacing: -0.5px; }"
                + "    .content { padding: 40px 35px; color: #334155; line-height: 1.6; font-size: 16px; }"
                + "    .content p { margin-top: 0; margin-bottom: 20px; }"
                + "    .cta-container { text-align: center; margin: 35px 0 15px; }"
                + "    .cta-button { background-color: #4F46E5; color: #ffffff !important; padding: 14px 30px; text-decoration: none; font-weight: bold; border-radius: 6px; display: inline-block; transition: background 0.2s; box-shadow: 0 2px 5px rgba(79,70,229,0.3); }"
                + "    .footer { background-color: #f8fafc; padding: 25px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }"
                + "  </style>"
                + "</head>"
                + "<body>"
                + "  <div class='wrapper'>"
                + "    <div class='header'>"
                + "      <h1>" + subject + "</h1>"
                + "    </div>"
                + "    <div class='content'>"
                + "      <p>" + bodyText.replace("\n", "<br/>") + "</p>"
                + "    </div>"
                + "    <div class='footer'>"
                + "      <p>© 2026 Scalable Notification Engine. All rights reserved.</p>"
                + "      <p>You received this email because you are registered under user alert preferences.</p>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }
}