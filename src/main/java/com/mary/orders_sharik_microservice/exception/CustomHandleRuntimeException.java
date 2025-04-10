package com.mary.orders_sharik_microservice.exception;

public class CustomHandleRuntimeException extends RuntimeException {
    public CustomHandleRuntimeException(String message) {
        super(message);
    }
}
