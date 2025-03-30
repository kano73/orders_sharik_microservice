package com.mary.orders_sharik_microservice.service;

import com.mary.orders_sharik_microservice.model.dto.request.ActionWithCartDTO;
import com.mary.orders_sharik_microservice.model.storage.ProductIdAndQuantity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class CartService {

    private final RedisTemplate<String, ProductIdAndQuantity> redisTemplate;

    private static final String CART_KEY_PREFIX = "cart:";

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
//                    if(item.getProduct().getAmountLeft()<item.getQuantity()) {
//                        throw new ValidationFailedException("There is not enough product amount left");
//                    }
//                    change to request to product_microservice (get product by id)
                    item.setQuantity(dto.getQuantity());
                    redisTemplate.opsForList().set(cartKey, i, item);
                }
                redisTemplate.expire(cartKey, 1, TimeUnit.HOURS);
                break;
            }
        }
    }

    public List<ProductIdAndQuantity> getCartByUserId(String userId) {
        String cartKey = CART_KEY_PREFIX + userId;
        return redisTemplate.opsForList().range(cartKey, 0, -1);
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
                    item.setQuantity(item.getQuantity() + (dto.getQuantity()));
//                    if(item.getProduct().getAmountLeft()<item.getQuantity()) {
//                        throw new ValidationFailedException("There is not enough product amount left");
//                    }
//                    change to request to product_microservice (get product by id)

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
//        if(product.getAmountLeft()<dto.getQuantity()){
//            throw new ValidationFailedException("There is not enough product amount left");
//        }
//        change to request to product_microservice (get product by id)

        ProductIdAndQuantity paq = new ProductIdAndQuantity();
        paq.setProductId(dto.getProductId());
        paq.setQuantity(dto.getQuantity());
        redisTemplate.opsForList().rightPush(cartKey, paq);
        redisTemplate.expire(cartKey, 1, TimeUnit.HOURS);
    }
}
