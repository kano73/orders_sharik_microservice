package com.mary.orders_sharik_microservice.anotation;

import com.mary.orders_sharik_microservice.service.ActionWithCartValidatorService;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ActionWithCartValidatorService.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidActionWithCart {
    String message() default "productAmountLeft should not be greater than quantity";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
