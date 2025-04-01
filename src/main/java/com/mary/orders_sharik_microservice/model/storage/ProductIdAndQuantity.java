package com.mary.orders_sharik_microservice.model.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductIdAndQuantity implements Serializable {
    @Serial
    private static final long serialVersionUID = 6L;

    private String productId;
    private int quantity;

}