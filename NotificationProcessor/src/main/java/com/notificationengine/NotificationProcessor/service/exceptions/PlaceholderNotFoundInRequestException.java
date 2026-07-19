package com.notificationengine.NotificationProcessor.service.exceptions;

public class PlaceholderNotFoundInRequestException extends RuntimeException {
    public PlaceholderNotFoundInRequestException(String message) {
        super(message);
    }
}

