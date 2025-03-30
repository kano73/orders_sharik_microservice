package com.mary.orders_sharik_microservice.consumer;

import com.mary.orders_sharik_microservice.service.CartService;
import com.mary.orders_sharik_microservice.service.RequestProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KafkaConsumerService {
    private final RequestProcessingService requestProcessingService;
    /*
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void makeOrder(String customAddress) {
        moveToHistoryAndSetStatus(OrderStatusEnum.CREATED, customAddress);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void emptyCart() {
        moveToHistoryAndSetStatus(OrderStatusEnum.CANCELLED, "");
    }

    private void moveToHistoryAndSetStatus(OrderStatusEnum status, String customAddress, String userId) {
        OrdersHistory ordersHistory = getHistoryOfUserById(userId);

        List<ProductIdAndQuantity> paq = redisTemplate.opsForList().range(CART_KEY_PREFIX + userId, 0, -1);

        if(paq == null || paq.isEmpty()) {
            return;
        }

        List<OrdersHistory.CartItem> cartItems = paq.stream()
                .map(product ->{
                    OrdersHistory.CartItem item = new OrdersHistory.CartItem();
                    item.setProduct(product.getProduct());
                    item.setQuantity(product.getQuantity());
                    return item;
                }).toList();

        if(status==OrderStatusEnum.CREATED){
            cartItems.forEach(paq1 -> {
                Product prod = productRepository.findById(paq1.getProduct().getId()).orElseThrow(()->
                        new NoDataFoundException("no product found with id:"+ paq1.getProduct().getId())
                );

                if(prod.getAmountLeft()<paq1.getQuantity()){
                    throw new ValidationFailedException("These is not enough product amount left");
                }

                prod.setAmountLeft(prod.getAmountLeft()-paq1.getQuantity());
                productRepository.save(prod);
            });
        }

        OrdersHistory.Order order = new OrdersHistory.Order();
        order.setItems(cartItems);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeliveryAddress(customAddress == null ? user.getAddress() : customAddress);

        ordersHistory.getOrders().add(order);

        redisTemplate.delete(CART_KEY_PREFIX + user.getId());
        ordersHistoryRepository.save(ordersHistory);
    }
*/

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.product_microservice_sharik.model.enums)." +
                    "CART_EMPTY_TOPIC.name()}",
            groupId = "product_group")
    public void emptyCart(ConsumerRecord<String, String> message){

    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.product_microservice_sharik.model.enums)." +
                    "CART_ORDER_TOPIC.name()}",
            groupId = "product_group")
    public void makeOrder(ConsumerRecord<String,String> message){
    }
//    here (up)

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.product_microservice_sharik.model.enums)." +
                    "CART_ADD_TOPIC.name()}",
            groupId = "product_group")
    public void addToCart(ConsumerRecord<String,String> message){
        requestProcessingService.addToCart(message);
    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.product_microservice_sharik.model.enums)." +
                    "CART_CHANGE_AMOUNT_TOPIC.name()}",
            groupId = "product_group")
    public void changeAmount(ConsumerRecord<String,String> message){
        requestProcessingService.changeAmount(message);
    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.product_microservice_sharik.model.enums)." +
                    "CART_VIEW_TOPIC.name()}",
            groupId = "product_group")
    public void viewCart(ConsumerRecord<String,String> message){
        requestProcessingService.sendCart(message);
    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.product_microservice_sharik.model.enums)." +
                    "HISTORY_VIEW_TOPIC.name()}",
            groupId = "product_group")
    public void viewHistory(ConsumerRecord<String,String> message){
        requestProcessingService.sendOrderHistory(message);
    }

    @SneakyThrows
    @KafkaListener(
            topics = "#{T(com.mary.product_microservice_sharik.model.enums)." +
                    "HISTORY_ALL_TOPIC.name()}",
            groupId = "product_group")
    public void viewAllHistories(ConsumerRecord<String,String> message){

    }
}