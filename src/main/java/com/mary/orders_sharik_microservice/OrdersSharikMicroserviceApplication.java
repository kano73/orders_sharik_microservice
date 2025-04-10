package com.mary.orders_sharik_microservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrdersSharikMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrdersSharikMicroserviceApplication.class, args);
	}

}
