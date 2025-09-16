package com.example.usertest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class UserTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserTestApplication.class, args);
    }
}