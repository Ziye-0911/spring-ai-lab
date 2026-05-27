package com.liziye.spring.ai.lab.scenario.rag.pipeline;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ETL 进度监控器。
 *
 * <p>使用原子变量线程安全地追踪 ETL 处理的实时进度，包括：
 * 总文件数、已处理文件数、失败文件数、总文档块数、已用时间。
 *
 * <p>配合 {@link EtlPipeline} 使用，在批量处理开始时调用 {@link #reset(int)} 初始化状态。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
public class EtlPipelineMonitor {

    private final AtomicInteger totalFiles = new AtomicInteger(0);
    private final AtomicInteger processedFiles = new AtomicInteger(0);
    private final AtomicInteger failedFiles = new AtomicInteger(0);
    private final AtomicLong totalChunks = new AtomicLong(0);
    private volatile long startTime;

    public void reset(int total) {
        totalFiles.set(total);
        processedFiles.set(0);
        failedFiles.set(0);
        totalChunks.set(0);
        startTime = System.currentTimeMillis();
    }

    public void incrementProcessed() {
        processedFiles.incrementAndGet();
    }

    public void incrementFailed() {
        failedFiles.incrementAndGet();
    }

    public void incrementChunks(int count) {
        totalChunks.addAndGet(count);
    }

    public double getProgress() {
        int total = totalFiles.get();
        if (total == 0) return 0;
        return (double) (processedFiles.get() + failedFiles.get()) / total * 100;
    }

    public long getElapsedMs() {
        return startTime > 0 ? System.currentTimeMillis() - startTime : 0;
    }
}
