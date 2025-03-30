package com.mary.orders_sharik_microservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String customAddress;
}


