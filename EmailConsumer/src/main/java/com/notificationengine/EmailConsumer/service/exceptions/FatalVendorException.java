package com.notificationengine.EmailConsumer.service.exceptions;

public class FatalVendorException extends RuntimeException {
    public FatalVendorException(String message) {
        super(message);
    }
}
