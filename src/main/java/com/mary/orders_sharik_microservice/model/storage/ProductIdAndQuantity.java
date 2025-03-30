package com.mary.orders_sharik_microservice.model.storage;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class ProductIdAndQuantity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String productId;
    private int quantity;

    @Override
    public String toString() {
        return "ProductIdAndQuantity{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                '}';
    }
}