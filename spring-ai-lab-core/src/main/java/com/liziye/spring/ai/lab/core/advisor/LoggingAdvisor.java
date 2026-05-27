package com.liziye.spring.ai.lab.core.advisor;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志记录增强器 — 记录每次对话请求和响应的关键信息。
 *
 * <p>支持请求日志、响应日志和异常日志三类记录。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class LoggingAdvisor {

    /** 是否记录请求日志 */
    private final boolean logRequest;

    /** 是否记录响应日志 */
    private final boolean logResponse;

    /** 日志内容最大长度（超出截断） */
    private final int maxContentLength;

    /**
     * 使用默认配置构造（启用请求/响应日志，最大长度 500）。
     */
    public LoggingAdvisor() {
        this(true, true, 500);
    }

    /**
     * 自定义日志配置构造。
     *
     * @param logRequest      是否记录请求日志
     * @param logResponse     是否记录响应日志
     * @param maxContentLength 日志内容最大长度
     */
    public LoggingAdvisor(boolean logRequest, boolean logResponse, int maxContentLength) {
        this.logRequest = logRequest;
        this.logResponse = logResponse;
        this.maxContentLength = maxContentLength;
    }

    /**
     * 调用前记录请求日志。
     *
     * @param conversationId 会话 ID
     * @param userInput      用户输入
     * @return 会话 ID
     */
    public String beforeCall(String conversationId, String userInput) {
        if (logRequest) {
            log.info("[AI-REQ] conversation={} input={}",
                    conversationId, truncate(userInput, maxContentLength));
        }
        return conversationId;
    }

    /**
     * 调用后记录响应日志。
     *
     * @param conversationId 会话 ID
     * @param response       响应内容
     * @param elapsedMs      耗时（毫秒）
     */
    public void afterCall(String conversationId, String response, long elapsedMs) {
        if (logResponse) {
            log.info("[AI-RES] conversation={} output={} elapsed={}ms",
                    conversationId, truncate(response, maxContentLength), elapsedMs);
        }
    }

    /**
     * 记录异常日志。
     *
     * @param conversationId 会话 ID
     * @param error          异常对象
     * @param elapsedMs      耗时（毫秒）
     */
    public void onError(String conversationId, Throwable error, long elapsedMs) {
        log.error("[AI-ERR] conversation={} error={} elapsed={}ms",
                conversationId, error.getMessage(), elapsedMs);
    }

    /**
     * 截断字符串，超出最大长度时追加 "..."
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
