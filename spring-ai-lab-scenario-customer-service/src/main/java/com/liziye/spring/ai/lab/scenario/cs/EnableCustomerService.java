package com.liziye.spring.ai.lab.scenario.cs;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用智能客服场景。
 *
 * <p>在 Spring Boot 应用启动类添加此注解，自动装配智能客服组件：
 * 意图识别、自动路由、多轮对话记忆、客服专用 Prompt。
 *
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableCustomerService
 * public class MyApp {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApp.class, args);
 *     }
 * }
 * </pre>
 *
 * <p>API 端点：
 * <ul>
 *   <li>POST /api/cs/chat — 智能客服对话</li>
 *   <li>POST /api/cs/chat/stream — SSE 流式客服对话</li>
 *   <li>GET /api/cs/session/{id}/history — 会话历史</li>
 * </ul>
 *
 * @author liziye
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(CustomerServiceAutoConfiguration.class)
public @interface EnableCustomerService {

    /** 默认系统 Prompt 语言：zh / en */
    String locale() default "zh";

    /** 最大对话轮次 */
    int maxTurns() default 20;

    /** 是否启用情感安抚 */
    boolean empathyEnabled() default true;
}
