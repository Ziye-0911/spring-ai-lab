package com.liziye.spring.ai.lab.core.exception;

/**
 * 模型不可用异常 — 指定的 AI 模型未配置或无法访问。
 *
 * @author liziye
 * @since 1.0.0
 */
public class ModelNotAvailableException extends AiLabException {

    /** 不可用的模型名称 */
    private final String modelName;

    /**
     * 构造模型不可用异常。
     *
     * @param modelName 模型名称
     */
    public ModelNotAvailableException(String modelName) {
        super("MODEL_NOT_AVAILABLE", "模型不可用: " + modelName);
        this.modelName = modelName;
    }

    /**
     * 构造模型不可用异常（包含原始异常）。
     *
     * @param modelName 模型名称
     * @param cause     原始异常
     */
    public ModelNotAvailableException(String modelName, Throwable cause) {
        super("MODEL_NOT_AVAILABLE", "模型不可用: " + modelName, cause);
        this.modelName = modelName;
    }

    /**
     * 获取不可用的模型名称。
     *
     * @return 模型名称
     */
    public String getModelName() {
        return modelName;
    }
}
