package com.notificationengine.PushNConsumer.service.exceptions;

public class RetryableVendorException extends RuntimeException {
    public RetryableVendorException(String message) {
        super(message);
    }

    public RetryableVendorException(String message, Throwable cause) {
        super(message, cause);
    }
}
