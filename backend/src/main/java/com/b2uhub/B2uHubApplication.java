package com.b2uhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class B2uHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(B2uHubApplication.class, args);
    }
}
