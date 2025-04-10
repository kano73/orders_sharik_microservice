package com.mary.orders_sharik_microservice.consumer;

import com.mary.orders_sharik_microservice.service.RequestProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumerService {
    private final RequestProcessingService requestProcessingService;

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.orders_sharik_microservice.model.enumClass.KafkaTopic)." +
                    "CART_EMPTY_TOPIC.name()}",
            groupId = "cart_history_group")
    public void emptyCart(ConsumerRecord<String, String> message){
        requestProcessingService.emptyCart(message);
    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.orders_sharik_microservice.model.enumClass.KafkaTopic)." +
                    "CART_ORDER_TOPIC.name()}",
            groupId = "cart_history_group")
    public void makeOrder(ConsumerRecord<String,String> message){
        requestProcessingService.makeOrder(message);
    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.orders_sharik_microservice.model.enumClass.KafkaTopic)." +
                    "CART_ADD_TOPIC.name()}",
            groupId = "cart_history_group")
    public void addToCart(ConsumerRecord<String,String> message){
        requestProcessingService.addToCart(message);
    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.orders_sharik_microservice.model.enumClass.KafkaTopic)." +
                    "CART_CHANGE_AMOUNT_TOPIC.name()}",
            groupId = "cart_history_group")
    public void changeAmount(ConsumerRecord<String,String> message){
        requestProcessingService.changeAmount(message);
    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.orders_sharik_microservice.model.enumClass.KafkaTopic)." +
                    "CART_VIEW_TOPIC.name()}",
            groupId = "cart_history_group")
    public void viewCart(ConsumerRecord<String,String> message){
        requestProcessingService.sendCart(message);
    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.orders_sharik_microservice.model.enumClass.KafkaTopic)." +
                    "HISTORY_VIEW_TOPIC.name()}",
            groupId = "cart_history_group")
    public void viewHistory(ConsumerRecord<String,String> message){
        requestProcessingService.sendOrderHistory(message);
    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.orders_sharik_microservice.model.enumClass.KafkaTopic)." +
                    "HISTORY_ALL_TOPIC.name()}",
            groupId = "cart_history_group")
    public void viewAllHistories(ConsumerRecord<String,String> message){
        requestProcessingService.sendWholeHistory(message);
    }
}