package com.liziye.spring.ai.lab.core.exception;

import com.liziye.spring.ai.lab.core.model.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理切面 — 统一处理框架异常并返回 {@link ApiResult} 格式。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理模型不可用异常。
     *
     * @param e 模型不可用异常
     * @return HTTP 503 响应
     */
    @ExceptionHandler(ModelNotAvailableException.class)
    public ResponseEntity<ApiResult<Void>> handleModelNotAvailable(ModelNotAvailableException e) {
        log.error("[EXCEPTION] Model not available: model={}", e.getModelName(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResult.error(503, "模型不可用: " + e.getModelName()));
    }

    /**
     * 处理文档加载异常。
     *
     * @param e 文档加载异常
     * @return HTTP 400 响应
     */
    @ExceptionHandler(DocumentLoadException.class)
    public ResponseEntity<ApiResult<Void>> handleDocumentLoad(DocumentLoadException e) {
        log.error("[EXCEPTION] Document load failed: file={}", e.getFilePath(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(400, "文档加载失败: " + e.getMessage()));
    }

    /**
     * 处理 AI Lab 基础异常。
     *
     * @param e AI Lab 异常
     * @return HTTP 500 响应
     */
    @ExceptionHandler(AiLabException.class)
    public ResponseEntity<ApiResult<Void>> handleAiLab(AiLabException e) {
        log.error("[EXCEPTION] AI Lab error: code={} message={}", e.getErrorCode(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(500, e.getMessage()));
    }

    /**
     * 处理非法参数异常。
     *
     * @param e 非法参数异常
     * @return HTTP 400 响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[EXCEPTION] Invalid argument: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResult.error(400, "参数错误: " + e.getMessage()));
    }

    /**
     * 处理未预期的通用异常。
     *
     * @param e 通用异常
     * @return HTTP 500 响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleGeneric(Exception e) {
        log.error("[EXCEPTION] Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(500, "内部服务错误: " + e.getMessage()));
    }
}
