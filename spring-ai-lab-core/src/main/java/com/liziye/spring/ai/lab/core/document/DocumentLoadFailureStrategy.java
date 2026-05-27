package com.liziye.spring.ai.lab.core.document;

/**
 * 文档加载失败处理策略。
 *
 * <p>在 ETL Pipeline 批量加载文档时，控制单个文档加载失败时的行为。
 *
 * <p>配置方式：
 * <pre>
 * spring.ai.lab.rag.etl.failure-strategy=SKIP
 * </pre>
 *
 * @author liziye
 * @since 1.0.0
 */
public enum DocumentLoadFailureStrategy {

    /**
     * 跳过失败文档，继续处理后续文档。
     * 记录失败日志和元数据，提供重试接口。
     * 适用场景：大量文档加载，允许个别文档暂时不可用。
     */
    SKIP,

    /**
     * 对失败文档进行重试（最多 N 次，可配置）。
     * 重试仍失败则降级为 SKIP 或 ABORT。
     * 适用场景：网络不稳定或临时文件锁定。
     */
    RETRY,

    /**
     * 任一个文档加载失败即中止整个 ETL 流程。
     * 抛出异常，阻止应用启动。
     * 适用场景：核心文档缺失不可接受的严格场景。
     */
    ABORT
}
