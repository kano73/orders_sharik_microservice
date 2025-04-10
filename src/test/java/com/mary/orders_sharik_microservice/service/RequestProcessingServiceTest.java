package com.mary.orders_sharik_microservice.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mary.orders_sharik_microservice.model.dto.request.ActionWithCartDTO;
import com.mary.orders_sharik_microservice.model.dto.request.OrderDetailsDTO;
import com.mary.orders_sharik_microservice.model.dto.responce.ProductAndQuantity;
import com.mary.orders_sharik_microservice.model.entity.OrdersHistory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestProcessingServiceTest {

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CartService cartService;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private RequestProcessingService requestProcessingService;

    @Captor
    private ArgumentCaptor<Message<String>> messageCaptor;

    private ConsumerRecord<String, String> mockConsumerRecord;
    private final String replyTopic = "reply-topic";
    private final String correlationId = "correlation-123";

    @BeforeEach
    void setUp() {
        // Set up headers that will be used across tests
        Headers mockHeaders = new RecordHeaders();
        mockHeaders.add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, replyTopic.getBytes()));
        mockHeaders.add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));

        // Set up base consumer record
        mockConsumerRecord = new ConsumerRecord<>("request-topic", 0, 0, "key", "value");

        // Use reflection to set headers since ConsumerRecord doesn't have a setter
        try {
            java.lang.reflect.Field headersField = ConsumerRecord.class.getDeclaredField("headers");
            headersField.setAccessible(true);
            headersField.set(mockConsumerRecord, mockHeaders);
        } catch (Exception e) {
            fail("Failed to set up test headers: " + e.getMessage());
        }
    }

    @Test
    void emptyCart_Success() throws JsonProcessingException {
        // Arrange
        String userId = "user123";
        when(objectMapper.readValue(anyString(), eq(String.class))).thenReturn(userId);
        when(objectMapper.writeValueAsString(eq(true))).thenReturn("true");
        doNothing().when(cartService).emptyCart(userId);

        // Act
        requestProcessingService.emptyCart(mockConsumerRecord);

        // Assert
        verify(cartService).emptyCart(userId);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertEquals("true", sentMessage.getPayload());
        assertEquals(replyTopic,
                new String((byte[]) Objects.requireNonNull(
                        sentMessage.getHeaders().get(KafkaHeaders.TOPIC))));
        assertEquals(correlationId,
                new String((byte[]) Objects.requireNonNull(
                        sentMessage.getHeaders().get(KafkaHeaders.CORRELATION_ID))));
        assertNull(sentMessage.getHeaders().get(KafkaHeaders.EXCEPTION_MESSAGE));
    }

    @Test
    void emptyCart_Exception() throws JsonProcessingException {
        // Arrange
        String userId = "user123";
        String errorMessage = "Cart not found";
        Exception exception = new RuntimeException(errorMessage);

        when(objectMapper.readValue(anyString(), eq(String.class))).thenReturn(userId);
        doThrow(exception).when(cartService).emptyCart(userId);
        when(objectMapper.writeValueAsString(contains(errorMessage))).thenReturn("\"Unable to empty cart: " + errorMessage + "\"");

        // Act
        requestProcessingService.emptyCart(mockConsumerRecord);

        // Assert
        verify(cartService).emptyCart(userId);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getHeaders().containsKey(KafkaHeaders.EXCEPTION_MESSAGE));
    }

    @Test
    void makeOrder_Success() throws JsonProcessingException {
        // Arrange
        OrderDetailsDTO orderDetails = new OrderDetailsDTO();
        orderDetails.setUserId("user123");
        orderDetails.setCustomAddress("123 Test St");

        when(objectMapper.readValue(anyString(), eq(OrderDetailsDTO.class))).thenReturn(orderDetails);
        when(objectMapper.writeValueAsString(eq(true))).thenReturn("true");
        doNothing().when(cartService).makeOrder(orderDetails.getUserId(), orderDetails.getCustomAddress());

        // Act
        requestProcessingService.makeOrder(mockConsumerRecord);

        // Assert
        verify(cartService).makeOrder(orderDetails.getUserId(), orderDetails.getCustomAddress());
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertEquals("true", sentMessage.getPayload());
    }

    @Test
    void addToCart_Success() throws JsonProcessingException {
        // Arrange
        ActionWithCartDTO actionDTO = new ActionWithCartDTO();

        when(objectMapper.readValue(anyString(), eq(ActionWithCartDTO.class))).thenReturn(actionDTO);
        when(objectMapper.writeValueAsString(eq(true))).thenReturn("true");
        doNothing().when(cartService).addToCart(actionDTO);

        // Act
        requestProcessingService.addToCart(mockConsumerRecord);

        // Assert
        verify(cartService).addToCart(actionDTO);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertEquals("true", sentMessage.getPayload());
    }

    @Test
    void changeAmount_Success() throws JsonProcessingException {
        // Arrange
        ActionWithCartDTO actionDTO = new ActionWithCartDTO();

        when(objectMapper.readValue(anyString(), eq(ActionWithCartDTO.class))).thenReturn(actionDTO);
        when(objectMapper.writeValueAsString(eq(true))).thenReturn("true");
        doNothing().when(cartService).changeAmountOrDelete(actionDTO);

        // Act
        requestProcessingService.changeAmount(mockConsumerRecord);

        // Assert
        verify(cartService).changeAmountOrDelete(actionDTO);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertEquals("true", sentMessage.getPayload());
    }

    @Test
    void sendCart_Success() throws JsonProcessingException {
        // Arrange
        String userId = "user123";
        List<ProductAndQuantity> cart = new ArrayList<>();

        when(objectMapper.readValue(anyString(), eq(String.class))).thenReturn(userId);
        when(cartService.getCartByUserId(userId)).thenReturn(cart);
        when(objectMapper.writeValueAsString(eq(cart))).thenReturn("[]");

        // Act
        requestProcessingService.sendCart(mockConsumerRecord);

        // Assert
        verify(cartService).getCartByUserId(userId);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertEquals("[]", sentMessage.getPayload());
    }

    @Test
    void sendOrderHistory_Success() throws JsonProcessingException {
        // Arrange
        String userId = "user123";
        OrdersHistory history = new OrdersHistory();

        when(objectMapper.readValue(anyString(), eq(String.class))).thenReturn(userId);
        when(historyService.getHistoryOfUserById(userId)).thenReturn(history);
        when(objectMapper.writeValueAsString(eq(history))).thenReturn("{}");

        // Act
        requestProcessingService.sendOrderHistory(mockConsumerRecord);

        // Assert
        verify(historyService).getHistoryOfUserById(userId);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertEquals("{}", sentMessage.getPayload());
    }

    @Test
    void sendWholeHistory_Success() throws JsonProcessingException {
        // Arrange
        Integer page = 1;
        List<OrdersHistory> historyList = new ArrayList<>();

        when(objectMapper.readValue(anyString(), eq(Integer.class))).thenReturn(page);
        when(historyService.getWholeHistory(page)).thenReturn(historyList);
        when(objectMapper.writeValueAsString(eq(historyList))).thenReturn("[]");

        // Act
        requestProcessingService.sendWholeHistory(mockConsumerRecord);

        // Assert
        verify(historyService).getWholeHistory(page);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertEquals("[]", sentMessage.getPayload());
    }
}