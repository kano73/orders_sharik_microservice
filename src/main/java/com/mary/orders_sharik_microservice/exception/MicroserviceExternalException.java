package com.mary.orders_sharik_microservice.exception;

public class MicroserviceExternalException extends CustomHandleRuntimeException {
    public MicroserviceExternalException(String message) {
        super(message);
    }

    public MicroserviceExternalException(Exception e) {
        super(e.getMessage());
    }
}
