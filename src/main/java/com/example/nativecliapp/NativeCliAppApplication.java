package com.example.nativecliapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class NativeCliAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(NativeCliAppApplication.class, args);
    }

}
