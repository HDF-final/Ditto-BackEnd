package com.ditto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class DittoApplication {

    @PostConstruct
    void started() {
        // 서비스 전역 기준 시간대: Asia/Seoul
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(DittoApplication.class, args);
    }
}
