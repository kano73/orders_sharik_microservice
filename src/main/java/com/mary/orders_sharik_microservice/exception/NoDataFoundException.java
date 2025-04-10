package com.mary.orders_sharik_microservice.exception;

public class NoDataFoundException extends CustomHandleRuntimeException {
    public NoDataFoundException(String message) {
        super(message);
    }
}
