package com.example.hotelbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.hotelbooking.repository")
public class HotelbookingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelbookingSystemApplication.class, args);
    }
}