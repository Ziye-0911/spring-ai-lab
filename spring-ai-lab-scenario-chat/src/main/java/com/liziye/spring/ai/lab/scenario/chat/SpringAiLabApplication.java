package com.liziye.spring.ai.lab.scenario.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI Lab 主应用入口。
 *
 * <p>集成了所有场景模块（Chat、Multi-Agent、Code Review 等），可直接运行测试。
 *
 * @author liziye
 * @since 1.0.0
 */
@SpringBootApplication
public class SpringAiLabApplication {

    /**
     * 应用启动入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringAiLabApplication.class, args);
    }
}
