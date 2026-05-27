package com.liziye.spring.ai.lab.scenario.rag;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.observation.DocumentMetrics;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.DefaultModelProviderManager;
import com.liziye.spring.ai.lab.scenario.rag.controller.DocumentController;
import com.liziye.spring.ai.lab.scenario.rag.controller.RagQaController;
import com.liziye.spring.ai.lab.scenario.rag.orchestrator.RagAgentOrchestrator;
import com.liziye.spring.ai.lab.scenario.rag.pipeline.EtlPipeline;
import com.liziye.spring.ai.lab.test.MockEmbeddingModel;
import com.liziye.spring.ai.lab.test.MockVectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * RAG QA 自动配置。
 *
 * <p>自动装配 RAG 场景所需的 Bean：向量模型、向量存储、ETL 管道、
 * {@link com.liziye.spring.ai.lab.scenario.rag.orchestrator.RagAgentOrchestrator} 编排器、
 * REST 控制器等。
 *
 * <p>注：{@code TokenMetrics}、{@code LatencyMetrics}、{@code ConversationMemory}、
 * {@code DefaultModelProviderManager} 等基础 Bean 已由
 * {@link com.liziye.spring.ai.lab.core.LabAutoConfiguration} (core) 统一提供。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RagQaProperties.class)
public class RagQaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MockEmbeddingModel embeddingModel() {
        log.info("Creating MockEmbeddingModel (configure a real one for production)");
        return new MockEmbeddingModel(1536);
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorStore vectorStore() {
        log.info("Creating MockVectorStore (configure pgvector/redis/etc for production)");
        return new MockVectorStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public EtlPipeline etlPipeline(VectorStore vectorStore,
                                   RagQaProperties properties,
                                   DocumentMetrics documentMetrics) {
        log.info("Initializing EtlPipeline: chunkSize={}, topK={}",
                properties.getChunk().getDefaultChunkSize(), properties.getTopK());
        return new EtlPipeline(vectorStore, properties, documentMetrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public RagAgentOrchestrator ragAgentOrchestrator(
            DefaultModelProviderManager modelManager,
            ConversationMemory memory,
            TokenMetrics tokenMetrics,
            LatencyMetrics latencyMetrics,
            EtlPipeline etlPipeline,
            RagQaProperties ragProperties) {
        log.info("Creating RagAgentOrchestrator");
        return new RagAgentOrchestrator(modelManager, memory, List.of(),
                tokenMetrics, latencyMetrics, etlPipeline, ragProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RagQaController ragQaController(RagAgentOrchestrator orchestrator) {
        return new RagQaController(orchestrator);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentController documentController(EtlPipeline etlPipeline) {
        return new DocumentController(etlPipeline);
    }
}
