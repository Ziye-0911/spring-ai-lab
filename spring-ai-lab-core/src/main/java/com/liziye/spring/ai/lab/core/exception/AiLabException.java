package com.liziye.spring.ai.lab.core.exception;

/**
 * Spring AI Lab 框架基础异常。
 *
 * <p>所有框架自定义异常的直接父类。
 *
 * @author liziye
 * @since 1.0.0
 */
public class AiLabException extends RuntimeException {

    /** 错误码 */
    private final String errorCode;

    /**
     * 使用默认错误码构造。
     *
     * @param message 错误消息
     */
    public AiLabException(String message) {
        super(message);
        this.errorCode = "AI_LAB_ERROR";
    }

    /**
     * 使用指定错误码构造。
     *
     * @param errorCode 错误码
     * @param message   错误消息
     */
    public AiLabException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 使用指定错误码和原始异常构造。
     *
     * @param errorCode 错误码
     * @param message   错误消息
     * @param cause     原始异常
     */
    public AiLabException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }
}
