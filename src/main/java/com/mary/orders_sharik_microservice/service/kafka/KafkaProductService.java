package com.mary.orders_sharik_microservice.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.mary.orders_sharik_microservice.exception.MicroserviceExternalException;
import com.mary.orders_sharik_microservice.exception.ValidationFailedException;
import com.mary.orders_sharik_microservice.model.enumClass.KafkaTopic;
import com.mary.orders_sharik_microservice.model.storage.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaProductService {

    private final ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate;
    private final ObjectMapper objectMapper;

    public List<Product> requestProductsByIds(List<String> ids) throws Exception {
        String valueJson = objectMapper.writeValueAsString(ids);

        CompletableFuture<ConsumerRecord<String, String>> futureResponse =
                makeRequest(KafkaTopic.PRODUCT_LIST_BY_IDS_TOPIC.name(), valueJson);

        return futureResponse.thenApply(response -> {
            CollectionType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Product.class);
            try {

                return objectMapper.<List<Product>>readValue(response.value(), listType);
            } catch (JsonProcessingException e) {
                log.error("e: ", e);
                throw new ValidationFailedException(e);
            }
        }).exceptionally(ex -> {
            throw new CompletionException(ex);
        }).join();
    }

    private CompletableFuture<ConsumerRecord<String, String>> makeRequest(String topic, String value)
            throws MicroserviceExternalException {
        // Создаем сообщение с заголовком reply-topic
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, value);

        // Добавляем заголовок с correlation ID
        String correlationId = UUID.randomUUID().toString();
        record.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));
        record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, KafkaTopic.REPLY_TOPIC.name().getBytes()));

        RequestReplyFuture<String, String, String> future =
                replyingKafkaTemplate.sendAndReceive(record, Duration.ofSeconds(5));

        // Получаем ответ
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Получаем ответ
                ConsumerRecord<String, String> response = future.get();
                for (Header header : response.headers()) {
                    if (header.key().equals(KafkaHeaders.EXCEPTION_MESSAGE)) {
                        String string = new String(header.value(), StandardCharsets.UTF_8);

                        log.error("here is an error from product_microservice: {}", string);

                        throw new CompletionException(new MicroserviceExternalException(string));
                    }
                }
                return response;
            } catch (InterruptedException | ExecutionException e) {
                throw new CompletionException(e);
            }
        });
    }
}