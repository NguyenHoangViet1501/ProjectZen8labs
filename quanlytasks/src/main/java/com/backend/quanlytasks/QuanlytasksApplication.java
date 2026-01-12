package com.backend.quanlytasks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class QuanlytasksApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuanlytasksApplication.class, args);
    }

}
