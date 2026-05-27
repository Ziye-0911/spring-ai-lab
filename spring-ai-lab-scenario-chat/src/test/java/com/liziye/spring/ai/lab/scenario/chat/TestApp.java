package com.liziye.spring.ai.lab.scenario.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 全场景集成测试 Spring Boot 应用。
 * 通过 AutoConfiguration.imports 机制自动加载 core + 所有场景模块。
 */
@SpringBootApplication
public class TestApp {

    public static void main(String[] args) {
        SpringApplication.run(TestApp.class, args);
    }
}
