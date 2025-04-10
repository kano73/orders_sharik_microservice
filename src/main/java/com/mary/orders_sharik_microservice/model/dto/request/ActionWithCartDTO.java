package com.mary.orders_sharik_microservice.model.dto.request;

import com.mary.orders_sharik_microservice.anotation.ValidActionWithCart;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@ValidActionWithCart
public class ActionWithCartDTO {
    @NotBlank
    private String userId;

    @NotNull
    private String productId;

    @NotNull
    @Min(0)
    private int quantity;

    @NotNull
    @Min(0)
    private int productAmountLeft;
}