package com.liziye.spring.ai.lab.core.exception;

/**
 * 文档加载异常 — 文档加载过程中发生的错误。
 *
 * @author liziye
 * @since 1.0.0
 */
public class DocumentLoadException extends AiLabException {

    /** 失败的文件路径 */
    private final String filePath;

    /**
     * 构造文档加载异常。
     *
     * @param filePath 失败的文件路径
     * @param message  错误消息
     */
    public DocumentLoadException(String filePath, String message) {
        super("DOCUMENT_LOAD_ERROR", message);
        this.filePath = filePath;
    }

    /**
     * 构造文档加载异常（包含原始异常）。
     *
     * @param filePath 失败的文件路径
     * @param message  错误消息
     * @param cause    原始异常
     */
    public DocumentLoadException(String filePath, String message, Throwable cause) {
        super("DOCUMENT_LOAD_ERROR", message, cause);
        this.filePath = filePath;
    }

    /**
     * 获取失败的文件路径。
     *
     * @return 文件路径
     */
    public String getFilePath() {
        return filePath;
    }
}
