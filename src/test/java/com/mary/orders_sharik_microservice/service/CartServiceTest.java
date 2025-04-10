package com.mary.orders_sharik_microservice.service;

import com.mary.orders_sharik_microservice.exception.ValidationFailedException;
import com.mary.orders_sharik_microservice.model.dto.request.ActionWithCartDTO;
import com.mary.orders_sharik_microservice.model.dto.responce.ProductAndQuantity;
import com.mary.orders_sharik_microservice.model.entity.OrdersHistory;
import com.mary.orders_sharik_microservice.model.storage.Product;
import com.mary.orders_sharik_microservice.model.storage.ProductIdAndQuantity;
import com.mary.orders_sharik_microservice.service.kafka.KafkaProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private final String userId = "user123";
    private final String productId = "prod456";
    @Mock
    private KafkaProductService kafkaProductService;
    @Mock
    private HistoryService historyService;
    @Mock
    private RedisTemplate<String, ProductIdAndQuantity> redisTemplate;

    @Mock
    private HashOperations<String, String, ProductIdAndQuantity> hashOperations;

    @InjectMocks
    private CartService cartService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenAnswer(invocation -> hashOperations);
    }

    @Test
    void addToCart_newItem_success() {
        ActionWithCartDTO dto = new ActionWithCartDTO();
        dto.setUserId(userId);
        dto.setProductId(productId);
        dto.setQuantity(2);
        dto.setProductAmountLeft(10);

        ProductIdAndQuantity productIdAndQuantity = new ProductIdAndQuantity(productId, 2);

        when(hashOperations.get(any(), any())).thenReturn(productIdAndQuantity);

        cartService.addToCart(dto);

        verify(hashOperations).put(eq("cart:" + userId), eq(productId), any(ProductIdAndQuantity.class));
    }

    @Test
    void addToCart_newQuantity_success() {
        ActionWithCartDTO dto = new ActionWithCartDTO();
        dto.setUserId(userId);
        dto.setProductId(productId);
        dto.setQuantity(2);
        dto.setProductAmountLeft(10);

        when(hashOperations.get(any(), any())).thenReturn(null);

        cartService.addToCart(dto);

        verify(hashOperations).put(eq("cart:" + userId), eq(productId), any(ProductIdAndQuantity.class));
    }

    @Test
    void addToCart_newQuantity_throwsException() {
        ActionWithCartDTO dto = new ActionWithCartDTO();
        dto.setUserId(userId);
        dto.setProductId(productId);
        dto.setQuantity(10);
        dto.setProductAmountLeft(10);

        ProductIdAndQuantity productIdAndQuantity = new ProductIdAndQuantity(productId, 2);

        when(hashOperations.get(any(), any())).thenReturn(productIdAndQuantity);

        assertThrows(ValidationFailedException.class, () -> cartService.addToCart(dto),
                "Not enough product left");
    }

    @Test
    void changeAmountOrDelete_changeAmount_success() {
        ProductIdAndQuantity item = new ProductIdAndQuantity(productId, 5);
        when(hashOperations.get(any(), any())).thenReturn(item);

        ActionWithCartDTO dto = new ActionWithCartDTO();
        dto.setUserId(userId);
        dto.setProductId(productId);
        dto.setQuantity(10);

        cartService.changeAmountOrDelete(dto);

        verify(hashOperations).put(eq("cart:" + userId), eq(productId), any(ProductIdAndQuantity.class));
    }

    @Test
    void changeAmountOrDelete_noProduct() {
        when(hashOperations.get(any(), any())).thenReturn(null);

        ActionWithCartDTO dto = new ActionWithCartDTO();
        dto.setUserId(userId);
        dto.setProductId(productId);
        dto.setQuantity(10);

        cartService.changeAmountOrDelete(dto);

        verify(redisTemplate.opsForHash(), never()).put(any(), any(), any());
    }

    @Test
    void changeAmountOrDelete_deleteItem_success() {
        ProductIdAndQuantity item = new ProductIdAndQuantity(productId, 5);
        when(hashOperations.get(any(), any())).thenReturn(item);

        ActionWithCartDTO dto = new ActionWithCartDTO();
        dto.setUserId(userId);
        dto.setProductId(productId);
        dto.setQuantity(0);

        cartService.changeAmountOrDelete(dto);

        verify(hashOperations).delete(eq("cart:" + userId), eq(productId));
    }

    @Test
    void getCartByUserId_success() throws Exception {
        ProductIdAndQuantity entry = new ProductIdAndQuantity(productId, 2);
        Product product = new Product();
        product.setId(productId);
        product.setAvailable(true);

        when(hashOperations.values(any())).thenReturn(List.of(entry));
        when(kafkaProductService.requestProductsByIds(any())).thenReturn(List.of(product));

        List<ProductAndQuantity> result = cartService.getCartByUserId(userId);

        assertEquals(1, result.size());
    }

    @Test
    void makeOrder_success() {
        Product product = new Product();
        product.setId(productId);
        product.setAvailable(true);
        product.setAmountLeft(5);

        ProductAndQuantity pq = new ProductAndQuantity();
        pq.setProduct(product);
        pq.setQuantity(2);

        OrdersHistory history = new OrdersHistory();
        history.setOrders(new ArrayList<>());

        CartService spyCart = Mockito.spy(cartService);
        doReturn(List.of(pq)).when(spyCart).getCartByUserId(userId);
        when(historyService.getHistoryOfUserById(userId)).thenReturn(history);

        spyCart.makeOrder(userId, "some address");

        verify(historyService).updateHistory(any());
        verify(redisTemplate).delete("cart:" + userId);
    }

    @Test
    void emptyCart_success() {
        Product product = new Product();
        product.setId(productId);
        product.setAvailable(true);

        ProductAndQuantity pq = new ProductAndQuantity();
        pq.setProduct(product);
        pq.setQuantity(1);

        OrdersHistory history = new OrdersHistory();
        history.setOrders(new ArrayList<>());

        CartService spyCart = Mockito.spy(cartService);
        doReturn(List.of(pq)).when(spyCart).getCartByUserId(userId);
        when(historyService.getHistoryOfUserById(userId)).thenReturn(history);

        spyCart.emptyCart(userId);

        verify(historyService).updateHistory(any());
        verify(redisTemplate).delete("cart:" + userId);
    }
}
