package com.mary.orders_sharik_microservice.repository;

import com.mary.orders_sharik_microservice.model.entity.OrdersHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrdersHistoryRepository extends MongoRepository<OrdersHistory, String> {
    Optional<OrdersHistory> findByUserId(String userId);
}

