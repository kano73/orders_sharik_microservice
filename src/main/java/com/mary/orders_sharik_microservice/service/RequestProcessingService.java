package com.mary.orders_sharik_microservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mary.orders_sharik_microservice.model.dto.request.ActionWithCartDTO;
import com.mary.orders_sharik_microservice.model.entity.OrdersHistory;
import com.mary.orders_sharik_microservice.model.storage.ProductIdAndQuantity;
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

    public void addToCart(ConsumerRecord<String, String> message) throws JsonProcessingException {
        // Извлекаем данные из запроса
        ActionWithCartDTO actionDTO = extractActionDTO(message);

        // Обрабатываем запрос
        cartService.addToCart(actionDTO);

        sendResponse(message, true, false);
    }

    public void changeAmount(ConsumerRecord<String, String> message) throws JsonProcessingException {
        try {
            // Извлекаем данные из запроса
            ActionWithCartDTO actionDTO = extractActionDTO(message);

            // Обрабатываем запрос
            cartService.changeAmountOrDelete(actionDTO);

            sendResponse(message, true, false);
        } catch (Exception e) {

            sendResponse(message, "External error", true);
        }
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

        String userId;
        try {
            userId = objectMapper.readValue( message.value(), String.class);
        } catch (Exception e) {
            sendResponse(message, "Exception while parsing : "+ String.class.getName(), true);
            throw e;
        }
        Objects.requireNonNull(userId);

        // Обрабатываем запрос
        List<ProductIdAndQuantity> cart = cartService.getCartByUserId(userId);

//      todo: request a list of products from product_microservice

        sendResponse(message, cart, false);
    }

    public void sendOrderHistory(ConsumerRecord<String, String> message) throws JsonProcessingException {
        String userId;
        try {
            userId = objectMapper.readValue(message.value(), String.class);
        } catch (Exception e) {
            sendResponse(message, "External error", true);
            throw e;
        }
        Objects.requireNonNull(userId);

        OrdersHistory history= historyService.getHistoryOfUserById(userId);

        sendResponse(message, history, false);
    }

    private <D> void sendResponse(ConsumerRecord<String, String> message, D data, boolean isError) throws JsonProcessingException {
        // Сериализуем результат в JSON
        String resultJson = objectMapper.writeValueAsString(data);

        // Создаем ответное сообщение
        Message<String> reply;
        if(isError){
            reply = MessageBuilder
                    .withPayload(resultJson)
                    .setHeader(KafkaHeaders.EXCEPTION_MESSAGE, resultJson)
                    .setHeader(KafkaHeaders.TOPIC, message.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value())
                    .setHeader(KafkaHeaders.CORRELATION_ID, message.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value())
                    .build();
        }else{
            reply = MessageBuilder
                    .withPayload(resultJson)
                    .setHeader(KafkaHeaders.TOPIC, message.headers().lastHeader(KafkaHeaders.REPLY_TOPIC).value())
                    .setHeader(KafkaHeaders.CORRELATION_ID, message.headers().lastHeader(KafkaHeaders.CORRELATION_ID).value())
                    .build();
        }
        // Отправляем ответ в reply-topic
        kafkaTemplate.send(reply);
    }
}
