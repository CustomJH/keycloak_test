package com.example.usertest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User Test Application
 * 
 * Spring Boot 애플리케이션의 메인 클래스입니다.
 * MyBatis 매퍼 스캔을 통해 데이터 접근 계층을 설정합니다.
 * 
 * @author YourName
 * @version 1.0
 * @since 2024-09
 */
@SpringBootApplication
@MapperScan("com.example.usertest.store.mapper") // MyBatis 매퍼 인터페이스 스캔
public class UserTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserTestApplication.class, args);
    }
}