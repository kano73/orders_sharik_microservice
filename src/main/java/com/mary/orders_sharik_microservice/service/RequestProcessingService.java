package com.mary.orders_sharik_microservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mary.orders_sharik_microservice.model.dto.request.ActionWithCartDTO;
import com.mary.orders_sharik_microservice.model.dto.request.OrderDetailsDTO;
import com.mary.orders_sharik_microservice.model.dto.responce.ProductAndQuantity;
import com.mary.orders_sharik_microservice.model.entity.OrdersHistory;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class RequestProcessingService {

    private final KafkaTemplate<?, ?> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final CartService cartService;
    private final HistoryService historyService;

    public void emptyCart(ConsumerRecord<String, String> message) throws JsonProcessingException {
        String userId;
        try {
            userId = objectMapper.readValue(message.value(), String.class);
        } catch (Exception e) {
            sendResponse(message, "Exception while parsing : "+ String.class.getName(), true);
            return;
        }
        Objects.requireNonNull(userId);

        // Обрабатываем запрос
        cartService.emptyCart(userId);

        sendResponse(message, true, false);
    }

    public void makeOrder(ConsumerRecord<String, String> message) throws JsonProcessingException {
        OrderDetailsDTO orderDetails;
        try {
            orderDetails = objectMapper.readValue(message.value(), OrderDetailsDTO.class);
        } catch (Exception e) {
            sendResponse(message, "Exception while parsing : "+ OrderDetailsDTO.class.getName(), true);
            return;
        }
        Objects.requireNonNull(orderDetails);

        // Обрабатываем запрос
        cartService.makeOrder(orderDetails.getUserId(), orderDetails.getCustomAddress());

        sendResponse(message, true, false);
    }

    public void addToCart(ConsumerRecord<String, String> message) throws JsonProcessingException {
        cartService.addToCart(extractActionDTO(message));

        sendResponse(message, true, false);
    }

    public void changeAmount(ConsumerRecord<String, String> message) throws JsonProcessingException {
        cartService.changeAmountOrDelete(extractActionDTO(message));
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

        System.out.println(actionDTO);

        return actionDTO;
    }

    public void sendCart(ConsumerRecord<String, String> message) throws JsonProcessingException {

        String userId;
        try {
            userId = objectMapper.readValue(message.value(), String.class);
        } catch (Exception e) {
            sendResponse(message, "Exception while parsing : "+ String.class.getName(), true);
            return;
        }
        Objects.requireNonNull(userId);

        // Обрабатываем запрос
        List<ProductAndQuantity> cart = cartService.getCartByUserId(userId);

        sendResponse(message, cart, false);
    }

    public void sendOrderHistory(ConsumerRecord<String, String> message) throws JsonProcessingException {
        String userId;
        try {
            userId = objectMapper.readValue(message.value(), String.class);
        } catch (Exception e) {
            sendResponse(message, "Error while parsing data from json", true);
            return;
        }
        Objects.requireNonNull(userId);

        OrdersHistory history= historyService.getHistoryOfUserById(userId);

        sendResponse(message, history, false);
    }

    public void sendWholeHistory(ConsumerRecord<String, String> message) throws JsonProcessingException {
        Integer page;
        try {
            page = objectMapper.readValue(message.value(), Integer.class);
        } catch (Exception e) {
            sendResponse(message, "Error while parsing data from json", true);
            return;
        }
        Objects.requireNonNull(page);

        List<OrdersHistory> history= historyService.getWholeHistory(page);

        sendResponse(message, history, false);
    }

    private <D> void sendResponse(ConsumerRecord<String, String> message, D data, boolean isError)
            throws JsonProcessingException {
        // Сериализуем результат в JSON
        String resultJson = objectMapper.writeValueAsString(data);

        // Создаем ответное сообщение
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
        // Отправляем ответ в reply-topic
        kafkaTemplate.send(reply);
    }
}
