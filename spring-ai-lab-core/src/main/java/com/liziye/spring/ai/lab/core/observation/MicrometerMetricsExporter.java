package com.liziye.spring.ai.lab.core.observation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * Micrometer 指标导出器 — 将 Lab 自定义指标桥接到 Micrometer。
 *
 * <p>仅在类路径存在 Micrometer {@code MeterRegistry} 时自动装配。
 * 将 {@link TokenMetrics}、{@link LatencyMetrics}、{@link DocumentMetrics}、
 * {@link ToolCallMetrics}、{@link ErrorMetrics}
 * 的数据定期发布到 Micrometer，进而支持 Prometheus/Grafana 抓取。
 *
 * <p>导出的指标名称（前缀 {@code ai_lab_}）：
 * <ul>
 *   <li>{@code ai_lab_tokens_total{model=""}} — 各模型 Token 消耗总量</li>
 *   <li>{@code ai_lab_requests_total{model=""}} — 各模型请求次数</li>
 *   <li>{@code ai_lab_errors_total{type=""}} — 各类型错误次数</li>
 *   <li>{@code ai_lab_tool_calls_total{tool="",status=""}} — 工具调用成功/失败次数</li>
 *   <li>{@code ai_lab_documents_loaded_total{status=""}} — 文档加载成功/失败次数</li>
 *   <li>{@code ai_lab_active_sessions} — 当前活跃会话数</li>
 * </ul>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class MicrometerMetricsExporter {

    private final TokenMetrics tokenMetrics;
    private final LatencyMetrics latencyMetrics;
    private final DocumentMetrics documentMetrics;
    private final ToolCallMetrics toolCallMetrics;
    private final ErrorMetrics errorMetrics;

    public MicrometerMetricsExporter(TokenMetrics tokenMetrics,
                                      LatencyMetrics latencyMetrics,
                                      DocumentMetrics documentMetrics,
                                      ToolCallMetrics toolCallMetrics,
                                      ErrorMetrics errorMetrics,
                                      MeterRegistry meterRegistry) {
        this.tokenMetrics = tokenMetrics;
        this.latencyMetrics = latencyMetrics;
        this.documentMetrics = documentMetrics;
        this.toolCallMetrics = toolCallMetrics;
        this.errorMetrics = errorMetrics;

        // 注册 Gauge（实时读取）
        registerGauges(meterRegistry);
        log.info("MicrometerMetricsExporter initialized: metrics registered to MeterRegistry");
    }

    /**
     * 每 30 秒将计数器数据同步到 Micrometer。
     */
    @Scheduled(fixedRate = 30_000)
    public void export() {
        // 指标通过 Gauge 实时暴露，定时导出无需额外操作
        // Gauge 在 MeterRegistry.scrape() 时自动读取最新值
    }

    private void registerGauges(MeterRegistry registry) {
        // Token 总量
        io.micrometer.core.instrument.Gauge.builder("ai_lab_tokens_total", tokenMetrics::getTotalTokens)
                .description("Total tokens consumed")
                .tags(List.of(Tag.of("aggregation", "total")))
                .register(registry);

        // 错误次数（按类型）
        io.micrometer.core.instrument.Gauge.builder("ai_lab_errors_total", errorMetrics::getTotalErrors)
                .description("Total error count")
                .register(registry);

        // 文档加载成功
        io.micrometer.core.instrument.Gauge.builder("ai_lab_documents_loaded_success",
                        documentMetrics::getDocumentsLoadedSuccess)
                .description("Documents loaded successfully")
                .register(registry);

        // 文档加载失败
        io.micrometer.core.instrument.Gauge.builder("ai_lab_documents_loaded_failed",
                        documentMetrics::getDocumentsLoadedFailed)
                .description("Documents loaded with failure")
                .register(registry);

        // 活跃会话数
        io.micrometer.core.instrument.Gauge.builder("ai_lab_active_sessions",
                        documentMetrics::getActiveSessions)
                .description("Active sessions count")
                .register(registry);

        // 向量入库成功
        io.micrometer.core.instrument.Gauge.builder("ai_lab_vectors_stored_success",
                        documentMetrics::getVectorsStoredSuccess)
                .description("Vectors stored successfully")
                .register(registry);

        // 向量入库失败
        io.micrometer.core.instrument.Gauge.builder("ai_lab_vectors_stored_failed",
                        documentMetrics::getVectorsStoredFailed)
                .description("Vectors stored with failure")
                .register(registry);
    }
}
