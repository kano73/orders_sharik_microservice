package com.mary.orders_sharik_microservice.exception.handler;

import com.mary.orders_sharik_microservice.exception.CustomHandleRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(CustomHandleRuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String customRuntime (CustomHandleRuntimeException exception){
        return exception.getMessage();
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFoundException() {
        return "404 not found";
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String runtime(RuntimeException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String runtime(MethodArgumentNotValidException exception) {
        return exception.getMessage();
    }
}