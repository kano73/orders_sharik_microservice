package com.mary.orders_sharik_microservice.service;

import com.mary.orders_sharik_microservice.exception.MicroserviceExternalException;
import com.mary.orders_sharik_microservice.exception.ValidationFailedException;
import com.mary.orders_sharik_microservice.model.dto.request.ActionWithCartDTO;
import com.mary.orders_sharik_microservice.model.dto.responce.ProductAndQuantity;
import com.mary.orders_sharik_microservice.model.entity.OrdersHistory;
import com.mary.orders_sharik_microservice.model.enums.OrderStatusEnum;
import com.mary.orders_sharik_microservice.model.storage.Product;
import com.mary.orders_sharik_microservice.model.storage.ProductIdAndQuantity;
import com.mary.orders_sharik_microservice.service.kafka.KafkaProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CartService {

    private final RedisTemplate<String, ProductIdAndQuantity> redisTemplate;

    private static final String CART_KEY_PREFIX = "cart:";
    private final KafkaProductService kafkaProductService;
    private final HistoryService historyService;

    public void addToCart(ActionWithCartDTO dto) {
        changeAmount(dto);
    }

    public void changeAmountOrDelete(ActionWithCartDTO dto) {
        String cartKey = CART_KEY_PREFIX + dto.getUserId();
        List<ProductIdAndQuantity> cart = redisTemplate.opsForList().range(cartKey, 0, -1);

        if (cart == null || cart.isEmpty()) {
            return;
        }

        for (int i = 0; i < cart.size(); i++) {
            ProductIdAndQuantity item = cart.get(i);
            if (item.getProductId().equals(dto.getProductId())) {
                if (dto.getQuantity() <= 0) {
                    redisTemplate.opsForList().remove(cartKey, 1, item);
                } else {
                    item.setQuantity(dto.getQuantity());
                    redisTemplate.opsForList().set(cartKey, i, item);
                }
                redisTemplate.expire(cartKey, 1, TimeUnit.HOURS);
                break;
            }
        }
    }

    public List<ProductAndQuantity> getCartByUserId(String userId) {
        String cartKey = CART_KEY_PREFIX + userId;
        List<ProductIdAndQuantity> idsAndQuantity = redisTemplate.opsForList().range(cartKey, 0, -1);

        if (idsAndQuantity == null || idsAndQuantity.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> ids = idsAndQuantity.stream().map(ProductIdAndQuantity::getProductId).toList();
        List<Product> products;
        try {
            products = kafkaProductService.requestProductsByIds(ids);
        } catch (Exception e) {
            throw new MicroserviceExternalException(e);
        }

        return products.stream().map(product -> {
            ProductAndQuantity productAndQuantity = new ProductAndQuantity();
            productAndQuantity.setProduct(product);
            productAndQuantity.setQuantity(
                    idsAndQuantity.stream().filter(productIdAndQuantity ->
                            productIdAndQuantity.getProductId().equals(product.getId())).findFirst().get().getQuantity()
            );
            return productAndQuantity;
        }).collect(Collectors.toList());
    }

    private void changeAmount(ActionWithCartDTO dto) {
        String cartKey = CART_KEY_PREFIX + dto.getUserId();
        List<ProductIdAndQuantity> cart = redisTemplate.opsForList().range(cartKey, 0, -1);

        if (cart == null || cart.isEmpty()) {
            addToCart(dto, cartKey);
            return;
        }

        boolean isChanged = false;
        for (int i = 0; i < cart.size(); i++) {
            ProductIdAndQuantity item = cart.get(i);
            if (item.getProductId().equals(dto.getProductId())) {
                isChanged = true;

                if (item.getQuantity() <= 0) {
                    redisTemplate.opsForList().remove(cartKey, 1, item);
                } else {
                    item.setQuantity(item.getQuantity() + dto.getQuantity());
                    if(dto.getProductAmountLeft()<item.getQuantity()){
                        throw new ValidationFailedException("Not enough product left");
                    }
                    redisTemplate.opsForList().set(cartKey, i, item);
                }

                redisTemplate.expire(cartKey, 1, TimeUnit.HOURS);
                break;
            }
        }
        if(!isChanged){
            addToCart(dto, cartKey);
        }
    }

    private void addToCart(ActionWithCartDTO dto, String cartKey){
        if(dto.getProductAmountLeft()<dto.getQuantity()){
            throw new ValidationFailedException("Not enough product left");
        }

        ProductIdAndQuantity paq = new ProductIdAndQuantity();
        paq.setProductId(dto.getProductId());
        paq.setQuantity(dto.getQuantity());

        redisTemplate.opsForList().rightPush(cartKey, paq);
        redisTemplate.expire(cartKey, 1, TimeUnit.HOURS);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void makeOrder(String userId, String customAddress) {
        moveToHistoryAndSetStatus(userId, OrderStatusEnum.CREATED, customAddress);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void emptyCart(String userId) {
        moveToHistoryAndSetStatus(userId, OrderStatusEnum.CANCELLED, "");
    }

    private void moveToHistoryAndSetStatus(String userId, OrderStatusEnum status, String address) {
        List<ProductAndQuantity> cart = getCartByUserId(userId);
        if (cart == null || cart.isEmpty()) {
            return;
        }
        OrdersHistory ordersHistory = historyService.getHistoryOfUserById(userId);

        List<OrdersHistory.CartItem> cartItems = cart.stream()
                .map(paq ->{
                    if(!paq.getProduct().isAvailable()){
                        throw new ValidationFailedException("Product is not available: "+paq.getProduct().getName());
                    }

                    OrdersHistory.CartItem item = new OrdersHistory.CartItem();
                    item.setProduct(paq.getProduct());
                    item.setQuantity(paq.getQuantity());
                    return item;
                }).toList();

        if(status==OrderStatusEnum.CREATED){
            cart.forEach(productAndQuantity -> {
                if(productAndQuantity.getProduct().getAmountLeft()<productAndQuantity.getQuantity()){
                    throw new ValidationFailedException("Not enough product left: "+productAndQuantity.getProduct().getName());
                }
            });
        }

        OrdersHistory.Order order = new OrdersHistory.Order();
        order.setItems(cartItems);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeliveryAddress(address);

        ordersHistory.getOrders().add(order);

        historyService.updateHistory(ordersHistory);
        redisTemplate.delete(CART_KEY_PREFIX + userId);
    }
}
