package com.mary.orders_sharik_microservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mary.orders_sharik_microservice.model.dto.request.ActionWithCartDTO;
import com.mary.orders_sharik_microservice.model.dto.request.OrderDetailsDTO;
import com.mary.orders_sharik_microservice.model.dto.responce.ProductAndQuantity;
import com.mary.orders_sharik_microservice.model.entity.OrdersHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class RequestProcessingService {

    private final KafkaTemplate<?, ?> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final CartService cartService;
    private final HistoryService historyService;

    public void emptyCart(ConsumerRecord<String, String> message) throws JsonProcessingException {
        try {
            String userId = objectMapper.readValue(message.value(), String.class);

            Objects.requireNonNull(userId);

            cartService.emptyCart(userId);
        } catch (Exception e) {
            sendResponse(message, "Unable to empty cart: "+e.getMessage(), true);
            return;
        }
        sendResponse(message, true, false);
    }

    public void makeOrder(ConsumerRecord<String, String> message) throws JsonProcessingException {
        try {
            OrderDetailsDTO orderDetails = objectMapper.readValue(message.value(), OrderDetailsDTO.class);
            Objects.requireNonNull(orderDetails);
            cartService.makeOrder(orderDetails.getUserId(), orderDetails.getCustomAddress());
        } catch (Exception e) {
            sendResponse(message, "Unable to make order: "+e.getMessage(), true);
            return;
        }
        sendResponse(message, true, false);
    }

    public void addToCart(ConsumerRecord<String, String> message) throws JsonProcessingException {
        try{
            cartService.addToCart(extractActionDTO(message));
        }catch (Exception e){
            sendResponse(message, "Unable to add to cart: "+e.getMessage(), true);
            return;
        }
        sendResponse(message, true, false);
    }

    public void changeAmount(ConsumerRecord<String, String> message) throws JsonProcessingException {
        try{
            cartService.changeAmountOrDelete(extractActionDTO(message));
        }catch (Exception e){
            sendResponse(message, "Unable to add to change amount: "+e.getMessage(), true);
            return;
        }
        sendResponse(message, true, false);
    }
    private ActionWithCartDTO extractActionDTO(ConsumerRecord<String, String> message) throws JsonProcessingException {
        ActionWithCartDTO actionDTO;
        try{
            actionDTO = objectMapper.readValue(message.value(), ActionWithCartDTO.class);
        } catch (JsonProcessingException e) {
            sendResponse(message, "Exception while parsing : "+ ActionWithCartDTO.class.getName(), true);
            throw e;
        }
        Objects.requireNonNull(actionDTO);

        return actionDTO;
    }

    public void sendCart(ConsumerRecord<String, String> message) throws JsonProcessingException {
        List<ProductAndQuantity> cart;
        try {
            String userId = objectMapper.readValue(message.value(), String.class);
            Objects.requireNonNull(userId);

            cart = cartService.getCartByUserId(userId);
        } catch (Exception e) {
            log.error("e: ", e);
            sendResponse(message, "Unable to get cart details: "+e.getMessage(), true);
            return;
        }
        sendResponse(message, cart, false);
    }

    public void sendOrderHistory(ConsumerRecord<String, String> message) throws JsonProcessingException {
        OrdersHistory history;
        try {
            String userId = objectMapper.readValue(message.value(), String.class);
            Objects.requireNonNull(userId);
            history = historyService.getHistoryOfUserById(userId);
        } catch (Exception e) {
            sendResponse(message, "Unable to get history of user:"+e.getMessage(), true);
            return;
        }
        sendResponse(message, history, false);
    }

    public void sendWholeHistory(ConsumerRecord<String, String> message) throws JsonProcessingException {
        List<OrdersHistory> history;
        try {
            Integer page = objectMapper.readValue(message.value(), Integer.class);
            Objects.requireNonNull(page);
            history = historyService.getWholeHistory(page);
        } catch (Exception e) {
            sendResponse(message, "Unable to get whole history", true);
            return;
        }
        sendResponse(message, history, false);
    }

    private <D> void sendResponse(ConsumerRecord<String, String> message, D data, boolean isError)
            throws JsonProcessingException {
        String resultJson = objectMapper.writeValueAsString(data);

        Message<String> reply;
        if(isError){
            reply = MessageBuilder
                    .withPayload(resultJson)
                    .setHeader(KafkaHeaders.EXCEPTION_MESSAGE, resultJson)
                    .setHeader(KafkaHeaders.TOPIC, message.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value())
                    .setHeader(KafkaHeaders.CORRELATION_ID,
                            message.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value())
                    .build();
        }else{
            reply = MessageBuilder
                    .withPayload(resultJson)
                    .setHeader(KafkaHeaders.TOPIC, message.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value())
                    .setHeader(KafkaHeaders.CORRELATION_ID,
                            message.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value())
                    .build();
        }
        kafkaTemplate.send(reply);
    }
}
