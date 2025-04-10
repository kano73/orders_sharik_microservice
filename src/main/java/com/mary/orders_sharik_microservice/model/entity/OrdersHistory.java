package com.mary.orders_sharik_microservice.model.entity;

import com.mary.orders_sharik_microservice.model.enumClass.OrderStatus;
import com.mary.orders_sharik_microservice.model.storage.Product;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "orderHistory")
public class OrdersHistory {

    @Id
    private String userId;
    private List<Order> orders = new ArrayList<>();

    @Data
    public static class Order{
        private List<CartItem> items;
        private LocalDateTime createdAt;
        private String orderId;
        private OrderStatus status;
        private String deliveryAddress;
    }

    @Data
    public static class CartItem {
        private Product product;
        private int quantity;
    }
}
