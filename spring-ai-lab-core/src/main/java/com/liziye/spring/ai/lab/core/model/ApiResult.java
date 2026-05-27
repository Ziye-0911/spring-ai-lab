package com.liziye.spring.ai.lab.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一 API 响应包装。
 *
 * <p>提供静态工厂方法快速构建成功和失败响应。
 *
 * @param <T> 响应数据类型
 * @author liziye
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

    /** 状态码 */
    private int code;

    /** 消息 */
    private String message;

    /** 数据 */
    private T data;

    /** 时间戳 */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 构建成功响应（默认消息 "success"）。
     *
     * @param <T>  数据类型
     * @param data 响应数据
     * @return 成功 ApiResult
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }

    /**
     * 构建成功响应（自定义消息）。
     *
     * @param <T>     数据类型
     * @param message 成功消息
     * @param data    响应数据
     * @return 成功 ApiResult
     */
    public static <T> ApiResult<T> success(String message, T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 构建错误响应（无数据）。
     *
     * @param <T>     数据类型
     * @param code    错误码
     * @param message 错误消息
     * @return 错误 ApiResult
     */
    public static <T> ApiResult<T> error(int code, String message) {
        return ApiResult.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 构建错误响应（附带数据，如验证错误详情）。
     *
     * @param <T>     数据类型
     * @param code    错误码
     * @param message 错误消息
     * @param data    附加数据
     * @return 错误 ApiResult
     */
    public static <T> ApiResult<T> error(int code, String message, T data) {
        return ApiResult.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
