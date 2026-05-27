package com.liziye.spring.ai.lab.scenario.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG QA 场景配置属性。
 *
 * <p>配置前缀：{@code spring.ai.lab.rag}。
 * 包含检索参数（Top-K、相似度阈值等）、ETL 配置（文档类型、并行线程等）、
 * 切分配置（分块大小、重叠大小等）以及系统提示词。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.rag")
public class RagQaProperties {

    /** 默认检索 Top-K */
    private int topK = 5;

    /** 相似度阈值 */
    private double similarityThreshold = 0.7;

    /** 是否启用重排序 */
    private boolean rerankEnabled = false;

    /** 最大 Token 数 */
    private int maxTokens = 4096;

    /** 默认温度 */
    private double temperature = 0.7;

    /** 系统提示（可覆盖） */
    private String systemPrompt = """
            你是一个基于知识库的问答助手。请基于提供的参考资料回答用户问题。
            - 如果参考资料中包含答案，请引用资料内容并标注来源。
            - 如果参考资料中没有答案，请如实告知用户。
            - 保持回答清晰、准确、有条理。
            """;

    /** ETL 配置 */
    private EtlConfig etl = new EtlConfig();

    /** 切分配置 */
    private ChunkConfig chunk = new ChunkConfig();

    @Data
    public static class EtlConfig {
        /** 文件上传最大大小（MB） */
        private int maxFileSizeMb = 50;
        /** 支持的文档类型 */
        private String[] supportedExtensions = {"pdf", "txt", "md", "docx"};
        /** ETL 并行线程数 */
        private int parallelThreads = 4;
    }

    @Data
    public static class ChunkConfig {
        /** 默认切分大小 */
        private int defaultChunkSize = 500;
        /** 默认重叠大小 */
        private int defaultOverlap = 50;
        /** PDF 文档专用切分大小 */
        private int pdfChunkSize = 500;
        /** 文本/MD 文档专用切分大小 */
        private int textChunkSize = 300;
    }
}
