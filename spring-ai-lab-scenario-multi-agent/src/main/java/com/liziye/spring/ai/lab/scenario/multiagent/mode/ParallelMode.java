package com.liziye.spring.ai.lab.scenario.multiagent.mode;

import com.liziye.spring.ai.lab.scenario.multiagent.model.MultiAgentContext;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 并行模式：多个 Agent 同时并行执行，最后合并结果。
 *
 * <p>适用场景：多维评估（安全性 + 性能 + 代码质量同时评估）、
 *          方案对比（多个 Agent 各自给出方案，然后投票/合并）。
 *
 * @author liziye
 * @since 1.0.0
 */
public class ParallelMode {

    /**
     * 并行执行多个 Agent。
     *
     * @param chatClient  {@link ChatClient} 实例
     * @param userInput   用户输入
     * @param context     多 Agent 上下文
     * @param agentRunner Agent 执行函数
     * @return 合并后的最终结果
     */
    public static String execute(ChatClient chatClient,
                                  String userInput,
                                  MultiAgentContext context,
                                  SequentialMode.AgentRunner agentRunner) {

        int agentCount = context.getAgentRoles().size();
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(agentCount, Runtime.getRuntime().availableProcessors()));
        List<Future<String>> futures = new ArrayList<>();

        for (MultiAgentContext.AgentRole role : context.getAgentRoles()) {
            String taskInput = buildParallelInput(userInput, role, context);
            futures.add(executor.submit(() -> agentRunner.run(chatClient, role, taskInput)));
        }

        // 收集结果
        StringBuilder allResults = new StringBuilder();
        for (int i = 0; i < futures.size(); i++) {
            try {
                String result = futures.get(i).get(context.getTaskTimeoutSeconds(), TimeUnit.SECONDS);
                allResults.append("## Agent ").append(i + 1)
                        .append(": ").append(context.getAgentRoles().get(i).getName())
                        .append("\n").append(result).append("\n\n");
            } catch (TimeoutException e) {
                allResults.append("## Agent ").append(i + 1)
                        .append(": ").append(context.getAgentRoles().get(i).getName())
                        .append("\n（执行超时）\n\n");
            } catch (Exception e) {
                allResults.append("## Agent ").append(i + 1)
                        .append(": ").append(context.getAgentRoles().get(i).getName())
                        .append("\n（执行失败: ").append(e.getMessage()).append("）\n\n");
            }
        }
        executor.shutdown();

        // 生成综合总结
        return generateSynthesis(chatClient, allResults.toString(), userInput, context);
    }

    private static String buildParallelInput(String userInput,
                                              MultiAgentContext.AgentRole role,
                                              MultiAgentContext context) {
        return "## 任务\n\n"
                + "请从【" + role.getName() + "】的视角分析以下需求：\n\n"
                + userInput + "\n\n"
                + "你的角色: " + role.getName() + " - " + role.getDescription() + "\n"
                + "请用中文给出专业分析。";
    }

    private static String generateSynthesis(ChatClient chatClient,
                                             String allResults,
                                             String originalInput,
                                             MultiAgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(allResults);

        String prompt = "请基于以下多个专家 Agent 独立给出的分析结果，进行综合对比和总结：\n\n"
                + "### 原始需求\n" + originalInput + "\n\n"
                + "### 各 Agent 独立分析\n" + allResults + "\n\n"
                + "请对比各专家观点，找出共识和分歧，给出最终建议。";

        try {
            String synthesis = chatClient.prompt().user(prompt).call().content();
            sb.append("## 综合总结\n").append(synthesis);
        } catch (Exception e) {
            sb.append("## 综合总结\n（生成失败：").append(e.getMessage()).append("）");
        }

        return sb.toString();
    }
}
