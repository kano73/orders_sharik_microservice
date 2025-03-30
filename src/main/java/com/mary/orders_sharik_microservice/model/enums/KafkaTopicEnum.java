package com.mary.orders_sharik_microservice.model.enums;

public enum KafkaTopicEnum {
    // PRODUCT
    PRODUCT_BY_FILTER_TOPIC,
    PRODUCT_BY_ID_TOPIC,
    PRODUCT_SET_STATUS_TOPIC,
    PRODUCT_CREATE_TOPIC,

    PRODUCT_REPLY_TOPIC,

    //HISTORY + CART
    CART_EMPTY_TOPIC,
    CART_ADD_TOPIC,
    CART_CHANGE_AMOUNT_TOPIC,
    CART_ORDER_TOPIC,
    CART_VIEW_TOPIC,
    HISTORY_VIEW_TOPIC,
    HISTORY_ALL_TOPIC
}