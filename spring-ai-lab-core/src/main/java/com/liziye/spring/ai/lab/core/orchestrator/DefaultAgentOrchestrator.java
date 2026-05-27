package com.liziye.spring.ai.lab.core.orchestrator;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.model.AgentContext;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.ModelProviderManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 默认编排器实现 — 最简单的对话模式。
 *
 * <p>直接将用户输入发给 {@link ChatClient}，不做任何检索或复杂逻辑。
 * 适合作为简单对话场景的编排器，或作为自定义编排器的参考基线。
 *
 * @author liziye
 * @since 1.0.0
 */
public class DefaultAgentOrchestrator extends BaseOrchestrator<AgentContext> {

    public DefaultAgentOrchestrator(ModelProviderManager modelManager,
                                     ConversationMemory memory,
                                     List<Advisor> advisors,
                                     TokenMetrics tokenMetrics,
                                     LatencyMetrics latencyMetrics) {
        super(modelManager, memory, advisors, tokenMetrics, latencyMetrics);
    }

    @Override
    protected AgentResponse doExecute(ChatClient chatClient, String userInput, AgentContext context) {
        String responseContent = chatClient.prompt()
                .user(userInput)
                .call()
                .content();

        return AgentResponse.builder()
                .content(responseContent)
                .conversationId(context.getConversationId())
                .build();
    }

    @Override
    public Flux<AgentResponse> executeStream(String conversationId, String userInput, AgentContext context) {
        return Flux.just(execute(conversationId, userInput, context));
    }

    @Override
    public String name() {
        return "default";
    }
}
