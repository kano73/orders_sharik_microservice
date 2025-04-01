package com.mary.orders_sharik_microservice.config;

import com.mary.orders_sharik_microservice.model.enums.KafkaTopicEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

@Configuration
public class KafkaConfig {
    @Bean
    public ReplyingKafkaTemplate<String, String, String>
    replyingKafkaTemplate(
            ProducerFactory<String, String> pf,
            KafkaMessageListenerContainer<String, String> container)
    {
        return new ReplyingKafkaTemplate<>(pf, container);
    }

    @Bean
    public KafkaMessageListenerContainer<String, String>
    replyContainer(ConsumerFactory<String, String> cf)
    {
        ContainerProperties containerProperties = new ContainerProperties(KafkaTopicEnum.REPLY_TOPIC.name());
        return new KafkaMessageListenerContainer<>(cf, containerProperties);
    }
}