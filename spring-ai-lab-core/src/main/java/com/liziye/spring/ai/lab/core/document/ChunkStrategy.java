package com.liziye.spring.ai.lab.core.document;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文档切分策略接口 — 决定如何将大文档切分成适合 Embedding 的小块。
 *
 * <p>支持能力：
 * <ul>
 *   <li>按文档类型动态指定切分大小和重叠度</li>
 *   <li>支持全局默认值 + 按类型覆盖</li>
 * </ul>
 *
 * @author liziye
 * @since 1.0.0
 */
public interface ChunkStrategy {

    /**
     * 使用默认参数切分。
     *
     * @param document 原始文档
     * @return 切分后的 Document 列表
     */
    List<Document> chunk(Document document);

    /**
     * 按文档类型动态指定切分参数。
     *
     * @param document    原始文档
     * @param chunkSize   每块最大字符数
     * @param overlapSize 相邻块重叠字符数
     * @return 切分后的 Document 列表
     */
    List<Document> chunk(Document document, int chunkSize, int overlapSize);

    /** 策略名称 */
    String name();

    /** 获取默认块大小 */
    int defaultChunkSize();

    /** 获取默认重叠大小 */
    int defaultOverlapSize();
}
