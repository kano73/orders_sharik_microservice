package com.mary.orders_sharik_microservice.model.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductIdAndQuantity{
    private String productId;
    private int quantity;

}