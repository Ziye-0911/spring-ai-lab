package com.liziye.spring.ai.lab.scenario.rag.orchestrator;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.orchestrator.BaseOrchestrator;
import com.liziye.spring.ai.lab.core.routing.ModelProviderManager;
import com.liziye.spring.ai.lab.scenario.rag.RagQaProperties;
import com.liziye.spring.ai.lab.scenario.rag.model.RagAgentContext;
import com.liziye.spring.ai.lab.scenario.rag.pipeline.EtlPipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG Agent 编排器。
 *
 * <p>继承 {@link com.liziye.spring.ai.lab.core.orchestrator.BaseOrchestrator}，
 * 实现检索增强生成（Retrieval-Augmented Generation）的完整流程：
 * <ol>
 *   <li>检索相关文档</li>
 *   <li>构建增强的 Prompt</li>
 *   <li>调用大模型生成回答</li>
 *   <li>构建响应元数据</li>
 * </ol>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class RagAgentOrchestrator extends BaseOrchestrator<RagAgentContext> {

    private final EtlPipeline etlPipeline;
    private final RagQaProperties ragProperties;

    public RagAgentOrchestrator(ModelProviderManager modelManager,
                                ConversationMemory memory,
                                List<Advisor> advisors,
                                TokenMetrics tokenMetrics,
                                LatencyMetrics latencyMetrics,
                                EtlPipeline etlPipeline,
                                RagQaProperties ragProperties) {
        super(modelManager, memory, advisors, tokenMetrics, latencyMetrics);
        this.etlPipeline = etlPipeline;
        this.ragProperties = ragProperties;
    }

    @Override
    protected AgentResponse doExecute(ChatClient chatClient, String userInput, RagAgentContext context) {
        int topK = context.getTopK() > 0 ? context.getTopK() : ragProperties.getTopK();
        double threshold = context.getSimilarityThreshold() > 0
                ? context.getSimilarityThreshold()
                : ragProperties.getSimilarityThreshold();

        // 1. 检索相关文档
        long searchStart = System.currentTimeMillis();
        List<Document> retrievedDocs = etlPipeline.search(userInput, topK, threshold);
        long searchTime = System.currentTimeMillis() - searchStart;
        log.info("[RAG] Retrieved {} documents in {}ms", retrievedDocs.size(), searchTime);

        // 2. 构建增强的 Prompt
        String augmentedPrompt = buildAugmentedPrompt(userInput, retrievedDocs);

        // 3. 调用模型
        String responseText = chatClient.prompt()
                .system(ragProperties.getSystemPrompt())
                .user(augmentedPrompt)
                .call()
                .content();

        // 4. 构建元数据
        Map<String, Object> metadata = Map.of(
                "model", "dashscope",
                "retrievedDocuments", retrievedDocs.size(),
                "searchTimeMs", searchTime,
                "sources", retrievedDocs.stream()
                        .map(d -> d.getMetadata().getOrDefault("source", "unknown"))
                        .distinct()
                        .collect(Collectors.toList())
        );

        return AgentResponse.builder()
                .content(responseText)
                .metadata(metadata)
                .build();
    }

    /**
     * 构建增强的提示词。
     *
     * <p>将检索到的文档内容与用户问题组合，构建包含参考资料和问题的增强 Prompt，
     * 要求模型基于参考资料用中文回答并引用编号。
     *
     * @param userQuery    用户原始问题
     * @param retrievedDocs 检索到的相关文档列表
     * @return 增强后的提示词文本
     */
    private String buildAugmentedPrompt(String userQuery, List<Document> retrievedDocs) {
        if (retrievedDocs.isEmpty()) {
            return userQuery;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("请基于以下参考资料回答用户的问题。\n\n");
        sb.append("=== 参考资料 ===\n");

        for (int i = 0; i < retrievedDocs.size(); i++) {
            Document doc = retrievedDocs.get(i);
            String source = doc.getMetadata().getOrDefault("source", "未知来源").toString();
            sb.append(String.format("[参考%d - 来源: %s]\n%s\n\n", i + 1, source, doc.getText()));
        }

        sb.append("=== 用户问题 ===\n");
        sb.append(userQuery);
        sb.append("\n\n请用中文回答，并在回答中引用参考资料的编号。");

        return sb.toString();
    }

    @Override
    public String name() {
        return "rag-qa-agent";
    }

    @Override
    protected String getOrchestratorName() {
        return "RagAgentOrchestrator";
    }
}
