package com.notificationengine.NotificationProcessor.service.exceptions;

public class PreferenceNotFoundException extends RuntimeException {
    public PreferenceNotFoundException(String message) {
        super(message);
    }
}

