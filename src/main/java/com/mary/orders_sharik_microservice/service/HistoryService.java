package com.mary.orders_sharik_microservice.service;

import com.mary.orders_sharik_microservice.model.entity.OrdersHistory;
import com.mary.orders_sharik_microservice.model.enumClass.OrderStatusEnum;
import com.mary.orders_sharik_microservice.repository.OrdersHistoryRepository;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final OrdersHistoryRepository ordersHistoryRepository;

    @Value("${page.size.history}")
    private Integer PAGE_SIZE;

    public OrdersHistory getHistoryOfUserById(String userId) {
        return ordersHistoryRepository.findByUserId(userId)
                .orElseGet(() -> {
                    OrdersHistory newOrderHistory = new OrdersHistory();
                    newOrderHistory.setUserId(userId);
                    return newOrderHistory;
                });
    }

    public List<OrdersHistory> getWholeHistory(@NotBlank @Min(1) Integer page) {
        return ordersHistoryRepository
                .findAll(PageRequest.of(page - 1, PAGE_SIZE))
                .getContent();

    }

//    scheduled task

    @Scheduled(cron = "0 0 * * * *")
    public void simulateStatusModification() {
        System.out.println("simulateStatusModification");
        List<OrderStatusEnum> statusEnumList = Arrays.asList(OrderStatusEnum.values());
        List<OrdersHistory> all = ordersHistoryRepository.findAll();
        all.forEach(ordersHistory -> ordersHistory.getOrders().forEach(order -> {
            OrderStatusEnum status = order.getStatus();
            int index = statusEnumList.indexOf(status);
            if(index != statusEnumList.size() - 3 && status!=OrderStatusEnum.CANCELLED) {
                status = statusEnumList.get(index + 1);
                order.setStatus(status);
            }
        }));
        ordersHistoryRepository.saveAll(all);
    }

    public void updateHistory(OrdersHistory ordersHistory) {
        ordersHistoryRepository.save(ordersHistory);
    }
}
