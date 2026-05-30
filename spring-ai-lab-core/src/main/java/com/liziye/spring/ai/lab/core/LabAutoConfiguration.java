package com.liziye.spring.ai.lab.core;

import com.liziye.spring.ai.lab.core.config.DashScopeProperties;
import com.liziye.spring.ai.lab.core.config.LabProperties;
import com.liziye.spring.ai.lab.core.config.MemoryProperties;
import com.liziye.spring.ai.lab.core.config.ModelProviderProperties;
import com.liziye.spring.ai.lab.core.config.SkillProperties;
import com.liziye.spring.ai.lab.core.llm.DashScopeChatModel;
import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.memory.InMemoryConversationMemory;
import com.liziye.spring.ai.lab.core.memory.RedisConversationMemory;
import com.liziye.spring.ai.lab.core.observation.*;
import com.liziye.spring.ai.lab.core.resilience.CircuitBreakerManager;
import com.liziye.spring.ai.lab.core.resilience.FallbackManager;
import com.liziye.spring.ai.lab.core.routing.DefaultModelProviderManager;
import com.liziye.spring.ai.lab.core.skill.InMemorySkillRegistry;
import com.liziye.spring.ai.lab.core.skill.SemanticSkillRouter;
import com.liziye.spring.ai.lab.core.skill.SkillLoader;
import com.liziye.spring.ai.lab.core.skill.SkillRegistry;
import com.liziye.spring.ai.lab.core.skill.SkillRouter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Spring AI Lab 核心自动配置入口。
 *
 * <p>负责装配框架核心 Bean，包括对话记忆、可观测性指标、容错降级、
 * LLM 基础设施和 Micrometer 指标导出。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({
        LabProperties.class,
        MemoryProperties.class,
        ModelProviderProperties.class,
        DashScopeProperties.class,
        SkillProperties.class
})
public class LabAutoConfiguration {

    // ===== 对话记忆 =====

    /**
     * 创建基于内存的对话记忆 Bean。
     *
     * <p>当未配置其他 {@link ConversationMemory} 实现且
     * {@code spring.ai.lab.memory.type=in-memory}（默认值）时创建。
     *
     * @param memoryProperties 记忆配置属性
     * @return {@link InMemoryConversationMemory} 实例
     */
    @Bean
    @ConditionalOnMissingBean(ConversationMemory.class)
    @ConditionalOnProperty(prefix = "spring.ai.lab.memory", name = "type", havingValue = "in-memory",
            matchIfMissing = true)
    public InMemoryConversationMemory inMemoryConversationMemory(MemoryProperties memoryProperties) {
        long ttlMillis = TimeUnit.MINUTES.toMillis(memoryProperties.getTtlMinutes());
        InMemoryConversationMemory memory = new InMemoryConversationMemory(
                ttlMillis, memoryProperties.getMaxHistory());
        log.info("Initialized InMemoryConversationMemory with TTL={}min, maxHistory={}",
                memoryProperties.getTtlMinutes(), memoryProperties.getMaxHistory());
        return memory;
    }

    /**
     * 创建基于 Redis 的对话记忆 Bean（生产环境推荐）。
     *
     * <p>仅在类路径存在 {@code StringRedisTemplate} 且
     * {@code spring.ai.lab.memory.type=redis} 时创建。
     *
     * @param memoryProperties 记忆配置属性
     * @return {@link ConversationMemory} 实例
     */
    @Bean
    @ConditionalOnMissingBean(ConversationMemory.class)
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnProperty(prefix = "spring.ai.lab.memory", name = "type", havingValue = "redis")
    public ConversationMemory redisConversationMemory(MemoryProperties memoryProperties) {
        long ttlMillis = TimeUnit.MINUTES.toMillis(memoryProperties.getTtlMinutes());
        log.info("Initialized RedisConversationMemory with TTL={}min", memoryProperties.getTtlMinutes());
        return new RedisConversationMemory(null, ttlMillis, memoryProperties.getMaxHistory());
    }

    // ===== 可观测性 =====

    /**
     * 创建 Token 使用量指标 Bean。
     *
     * @return {@link TokenMetrics} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.lab.observation", name = "token-tracking",
            havingValue = "true", matchIfMissing = true)
    public TokenMetrics tokenMetrics() {
        log.debug("TokenMetrics initialized");
        return new TokenMetrics();
    }

    /**
     * 创建延迟指标 Bean。
     *
     * @return {@link LatencyMetrics} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.lab.observation", name = "latency-tracking",
            havingValue = "true", matchIfMissing = true)
    public LatencyMetrics latencyMetrics() {
        log.debug("LatencyMetrics initialized");
        return new LatencyMetrics();
    }

    /**
     * 创建文档处理指标 Bean。
     *
     * @return {@link DocumentMetrics} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DocumentMetrics documentMetrics() {
        return new DocumentMetrics();
    }

    /**
     * 创建工具调用指标 Bean。
     *
     * @return {@link ToolCallMetrics} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolCallMetrics toolCallMetrics() {
        return new ToolCallMetrics();
    }

    /**
     * 创建错误指标 Bean。
     *
     * @return {@link ErrorMetrics} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorMetrics errorMetrics() {
        return new ErrorMetrics();
    }

    // ===== 容错与降级 =====

    /**
     * 创建降级管理器 Bean。
     *
     * @param labProperties Lab 全局配置属性
     * @return {@link FallbackManager} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.lab.fallback", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public FallbackManager fallbackManager(LabProperties labProperties) {
        return new FallbackManager(
                true,
                labProperties.getModel() != null ? labProperties.getModel().getFallbackProvider() : null,
                "抱歉，AI 服务暂时不可用，请稍后重试。"
        );
    }

    /**
     * 创建熔断器管理器 Bean。
     *
     * @return {@link CircuitBreakerManager} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerManager circuitBreakerManager() {
        CircuitBreakerManager.CircuitBreakerConfig config = new CircuitBreakerManager.CircuitBreakerConfig();
        config.setFailureRateThreshold(50);
        config.setWaitDurationInOpen(Duration.ofSeconds(60));
        config.setSlidingWindowSize(10);
        config.setMinimumNumberOfCalls(5);
        return new CircuitBreakerManager(config);
    }

    // ===== LLM 基础设施 =====

    /**
     * 创建 DashScope ChatModel Bean（仅当配置了 api-key 时创建）。
     *
     * @param properties DashScope API 配置
     * @return {@link DashScopeChatModel} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.dashscope", name = "api-key",
            matchIfMissing = false)
    public DashScopeChatModel dashScopeChatModel(DashScopeProperties properties) {
        log.info("Creating DashScopeChatModel: model={}", properties.getModel());
        return new DashScopeChatModel(properties);
    }

    /**
     * 创建默认模型提供商管理器。
     *
     * <p>注册 {@link DashScopeChatModel} 作为默认提供商。
     *
     * @param chatModel DashScope ChatModel
     * @return {@link DefaultModelProviderManager} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultModelProviderManager modelProviderManager(DashScopeChatModel chatModel) {
        DefaultModelProviderManager manager = new DefaultModelProviderManager();
        manager.addModel("dashscope", chatModel, "default", "premium");
        manager.setDefaultModel("dashscope");
        log.info("Registered DashScope ChatModel as default provider");
        return manager;
    }

    // ===== Micrometer 指标导出 =====

    /**
     * 创建 Micrometer 指标导出器。
     *
     * <p>仅在类路径存在 {@code MeterRegistry} 时装配，
     * 将 Lab 自定义指标桥接到 Micrometer → Prometheus/Grafana。
     *
     * @param tokenMetrics Token 指标
     * @param latencyMetrics 延迟指标
     * @param documentMetrics 文档指标
     * @param toolCallMetrics 工具调用指标
     * @param errorMetrics 错误指标
     * @param meterRegistry Micrometer 注册表
     * @return {@link MicrometerMetricsExporter} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnProperty(prefix = "spring.ai.lab.observation", name = "metrics-export",
            havingValue = "true", matchIfMissing = true)
    public MicrometerMetricsExporter micrometerMetricsExporter(
            TokenMetrics tokenMetrics,
            LatencyMetrics latencyMetrics,
            DocumentMetrics documentMetrics,
            ToolCallMetrics toolCallMetrics,
            ErrorMetrics errorMetrics,
            MeterRegistry meterRegistry) {
        log.info("MicrometerMetricsExporter activated: Prometheus/Grafana metrics enabled");
        return new MicrometerMetricsExporter(
                tokenMetrics, latencyMetrics, documentMetrics,
                toolCallMetrics, errorMetrics, meterRegistry);
    }

    // ===== Skill 系统 =====

    /**
     * 创建 Skill 注册中心 Bean。
     *
     * @return {@link InMemorySkillRegistry} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.lab.skill", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public SkillRegistry skillRegistry() {
        log.info("SkillRegistry initialized");
        return new InMemorySkillRegistry();
    }

    /**
     * 创建语义 Skill 路由器 Bean。
     *
     * @param skillProperties Skill 配置属性
     * @return {@link SemanticSkillRouter} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.lab.skill", name = "routing-strategy",
            havingValue = "semantic", matchIfMissing = true)
    public SkillRouter skillRouter(SkillProperties skillProperties) {
        log.info("SemanticSkillRouter initialized with threshold={}", skillProperties.getSimilarityThreshold());
        return new SemanticSkillRouter(skillProperties);
    }

    /**
     * 创建 Skill 加载器 Bean。
     *
     * <p>在初始化完成后立即扫描 skills 目录，如果启用了热加载则启动文件监听。
     *
     * @param skillProperties Skill 配置属性
     * @param skillRegistry   Skill 注册中心
     * @param resourceLoader  资源加载器
     * @return {@link SkillLoader} 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.lab.skill", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public SkillLoader skillLoader(SkillProperties skillProperties,
                                   SkillRegistry skillRegistry,
                                   ResourceLoader resourceLoader) {
        SkillLoader loader = new SkillLoader(skillProperties, skillRegistry, resourceLoader);
        loader.load();
        return loader;
    }

    /**
     * SkillLoader 销毁时停止热加载监听。
     */
    @PreDestroy
    public void shutdownSkillLoader() {
        // SkillLoader 作为 Bean 存在时由容器管理生命周期
    }
}
