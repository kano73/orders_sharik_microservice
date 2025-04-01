package com.mary.orders_sharik_microservice.service;

import com.mary.orders_sharik_microservice.anotation.ValidActionWithCart;
import com.mary.orders_sharik_microservice.model.dto.request.ActionWithCartDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ActionWithCartValidatorService implements ConstraintValidator<ValidActionWithCart, ActionWithCartDTO> {

    @Override
    public boolean isValid(ActionWithCartDTO dto, ConstraintValidatorContext context) {
        if (dto == null) return true;
        return dto.getProductAmountLeft() <= dto.getQuantity();
    }
}
