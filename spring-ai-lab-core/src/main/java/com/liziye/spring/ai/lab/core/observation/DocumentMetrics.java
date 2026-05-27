package com.liziye.spring.ai.lab.core.observation;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文档处理指标 — 统计文档加载、切分、入库的成功率和耗时。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class DocumentMetrics {

    private final AtomicLong documentsLoadedSuccess = new AtomicLong(0);
    private final AtomicLong documentsLoadedFailed = new AtomicLong(0);
    private final AtomicLong vectorsStoredSuccess = new AtomicLong(0);
    private final AtomicLong vectorsStoredFailed = new AtomicLong(0);
    private final AtomicLong etlTotalTimeMs = new AtomicLong(0);
    private final AtomicInteger activeSessions = new AtomicInteger(0);

    /**
     * 记录文档加载结果。
     *
     * @param success 是否成功
     */
    public void recordDocumentLoaded(boolean success) {
        if (success) {
            documentsLoadedSuccess.incrementAndGet();
        } else {
            documentsLoadedFailed.incrementAndGet();
        }
    }

    /**
     * 记录向量入库结果。
     *
     * @param success 是否成功
     */
    public void recordVectorStored(boolean success) {
        if (success) {
            vectorsStoredSuccess.incrementAndGet();
        } else {
            vectorsStoredFailed.incrementAndGet();
        }
    }

    /**
     * 记录 ETL 处理耗时。
     *
     * @param timeMs 耗时（毫秒）
     */
    public void addEtlTime(long timeMs) {
        etlTotalTimeMs.addAndGet(timeMs);
    }

    public void incrementActiveSessions() {
        activeSessions.incrementAndGet();
    }

    public void decrementActiveSessions() {
        activeSessions.decrementAndGet();
    }

    public long getDocumentsLoadedSuccess() {
        return documentsLoadedSuccess.get();
    }

    public long getDocumentsLoadedFailed() {
        return documentsLoadedFailed.get();
    }

    public long getVectorsStoredSuccess() {
        return vectorsStoredSuccess.get();
    }

    public long getVectorsStoredFailed() {
        return vectorsStoredFailed.get();
    }

    public long getEtlTotalTimeMs() {
        return etlTotalTimeMs.get();
    }

    public int getActiveSessions() {
        return activeSessions.get();
    }

    /**
     * 获取文档加载成功率。
     *
     * @return 成功率（0.0 ~ 1.0）
     */
    public double getDocumentLoadSuccessRate() {
        long total = documentsLoadedSuccess.get() + documentsLoadedFailed.get();
        return total > 0 ? (double) documentsLoadedSuccess.get() / total : 0;
    }
}
