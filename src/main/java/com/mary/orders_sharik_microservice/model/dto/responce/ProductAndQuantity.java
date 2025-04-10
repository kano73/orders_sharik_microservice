package com.mary.orders_sharik_microservice.model.dto.responce;

import com.mary.orders_sharik_microservice.model.storage.Product;
import lombok.Data;

@Data
public class ProductAndQuantity {
    private Product product;
    private int quantity;
}
