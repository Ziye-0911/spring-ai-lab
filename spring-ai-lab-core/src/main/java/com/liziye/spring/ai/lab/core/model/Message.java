package com.liziye.spring.ai.lab.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息模型 — 表示对话中的一条消息。
 *
 * <p>提供工厂方法快速创建 {@code user}、{@code assistant}、{@code system} 三种角色的消息。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /** 角色: user / assistant / system */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息时间戳 */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 创建用户消息。
     *
     * @param content 消息内容
     * @return 用户消息
     */
    public static Message user(String content) {
        return Message.builder().role("user").content(content).build();
    }

    /**
     * 创建助手消息。
     *
     * @param content 消息内容
     * @return 助手消息
     */
    public static Message assistant(String content) {
        return Message.builder().role("assistant").content(content).build();
    }

    /**
     * 创建系统消息。
     *
     * @param content 消息内容
     * @return 系统消息
     */
    public static Message system(String content) {
        return Message.builder().role("system").content(content).build();
    }
}
