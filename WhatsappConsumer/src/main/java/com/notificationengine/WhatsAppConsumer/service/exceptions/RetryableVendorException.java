package com.notificationengine.WhatsAppConsumer.service.exceptions;

public class RetryableVendorException extends RuntimeException {
    public RetryableVendorException(String message) {
        super(message);
    }

    public RetryableVendorException(String message, Throwable cause) {
        super(message, cause);
    }
}
