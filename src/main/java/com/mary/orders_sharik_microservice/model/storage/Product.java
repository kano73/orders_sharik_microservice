package com.mary.orders_sharik_microservice.model.storage;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Product{
    private String id;
    private String name;
    private BigDecimal price;
    private Integer amountLeft;
    private String description;
    private String imageUrl;
    private List<String> categories;
    private boolean isAvailable;
}
