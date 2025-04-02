package com.mary.orders_sharik_microservice.service;

import com.mary.orders_sharik_microservice.exception.MicroserviceExternalException;
import com.mary.orders_sharik_microservice.exception.ValidationFailedException;
import com.mary.orders_sharik_microservice.model.dto.request.ActionWithCartDTO;
import com.mary.orders_sharik_microservice.model.dto.responce.ProductAndQuantity;
import com.mary.orders_sharik_microservice.model.entity.OrdersHistory;
import com.mary.orders_sharik_microservice.model.enumClass.OrderStatusEnum;
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

    private static final String CART_KEY_PREFIX = "cart:";
    private final RedisTemplate<String, ProductIdAndQuantity> redisTemplate;
    private final KafkaProductService kafkaProductService;
    private final HistoryService historyService;

    public void addToCart(ActionWithCartDTO dto) {
        String cartKey = CART_KEY_PREFIX + dto.getUserId();

        if (dto.getProductAmountLeft() < dto.getQuantity()) {
            throw new ValidationFailedException("Not enough product left");
        }

        String productKey = dto.getProductId();
        ProductIdAndQuantity existingItem = (ProductIdAndQuantity) redisTemplate.opsForHash().get(cartKey, productKey);

        if (existingItem == null) {
            existingItem = new ProductIdAndQuantity();
            existingItem.setProductId(productKey);
            existingItem.setQuantity(dto.getQuantity());
        } else {
            int newQuantity = existingItem.getQuantity() + dto.getQuantity();
            if (dto.getProductAmountLeft() < newQuantity) {
                throw new ValidationFailedException("Not enough product left");
            }
            existingItem.setQuantity(newQuantity);
        }

        redisTemplate.opsForHash().put(cartKey, productKey, existingItem);
        redisTemplate.expire(cartKey, 1, TimeUnit.HOURS);

    }

    public void changeAmountOrDelete(ActionWithCartDTO dto) {
        String cartKey = CART_KEY_PREFIX + dto.getUserId();
        String productKey = dto.getProductId();

        ProductIdAndQuantity item = (ProductIdAndQuantity) redisTemplate.opsForHash().get(cartKey, productKey);

        if (item == null) {
            return;
        }

        if (dto.getQuantity() <= 0) {
            redisTemplate.opsForHash().delete(cartKey, productKey);
        } else {
            item.setQuantity(dto.getQuantity());
            redisTemplate.opsForHash().put(cartKey, productKey, item);
        }

        redisTemplate.expire(cartKey, 1, TimeUnit.HOURS);
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
            productAndQuantity.setQuantity(idsAndQuantity.stream().filter(productIdAndQuantity -> productIdAndQuantity.getProductId().equals(product.getId())).findFirst().get().getQuantity());
            return productAndQuantity;
        }).collect(Collectors.toList());
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

        List<OrdersHistory.CartItem> cartItems = cart.stream().map(paq -> {
            if (!paq.getProduct().isAvailable()) {
                throw new ValidationFailedException("Product is not available: " + paq.getProduct().getName());
            }

            OrdersHistory.CartItem item = new OrdersHistory.CartItem();
            item.setProduct(paq.getProduct());
            item.setQuantity(paq.getQuantity());
            return item;
        }).toList();

        if (status == OrderStatusEnum.CREATED) {
            cart.forEach(productAndQuantity -> {
                if (productAndQuantity.getProduct().getAmountLeft() < productAndQuantity.getQuantity()) {
                    throw new ValidationFailedException("Not enough product left: " + productAndQuantity.getProduct().getName());
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
