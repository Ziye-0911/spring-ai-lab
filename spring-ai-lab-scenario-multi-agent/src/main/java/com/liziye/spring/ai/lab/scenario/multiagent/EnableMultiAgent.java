package com.liziye.spring.ai.lab.scenario.multiagent;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用多 Agent 协作场景。
 *
 * <p>将此注解添加到 Spring Boot 启动类上即可自动装配多 Agent 组件。
 *
 * <p>示例用法：
 * <pre>{@code
 * @SpringBootApplication
 * @EnableMultiAgent
 * public class MyApp {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApp.class, args);
 *     }
 * }
 * }</pre>
 *
 * @author liziye
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MultiAgentAutoConfiguration.class)
public @interface EnableMultiAgent {

    /** 默认协作模式：sequential / parallel / router / debate */
    String defaultMode() default "sequential";

    /** 最大迭代轮次 */
    int maxIterations() default 3;

    /** 最大 Agent 数量 */
    int maxAgents() default 5;
}
