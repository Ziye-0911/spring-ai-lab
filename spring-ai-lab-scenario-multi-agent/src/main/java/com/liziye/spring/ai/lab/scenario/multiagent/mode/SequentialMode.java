package com.liziye.spring.ai.lab.scenario.multiagent.mode;

import com.liziye.spring.ai.lab.scenario.multiagent.model.MultiAgentContext;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 顺序模式：Agent 按顺序依次执行，后一个 Agent 可参考前一个的输出。
 *
 * <p>适用场景：需求分析 → 架构设计 → 编码实现 → 代码审查。
 *
 * @author liziye
 * @since 1.0.0
 */
public class SequentialMode {

    /**
     * 顺序执行多个 Agent。
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
                                  AgentRunner agentRunner) {

        StringBuilder finalResult = new StringBuilder();
        String previousOutput = userInput;

        for (int i = 0; i < context.getAgentRoles().size(); i++) {
            MultiAgentContext.AgentRole role = context.getAgentRoles().get(i);

            // 构造此 Agent 的输入（包含上一个 Agent 的结果）
            String taskInput = buildSequentialInput(previousOutput, role, i, context);

            // 执行
            String result = agentRunner.run(chatClient, role, taskInput);

            finalResult.append("## Agent ").append(i + 1)
                    .append(": ").append(role.getName()).append("\n")
                    .append(result).append("\n\n");

            previousOutput = result;
        }

        // 生成最终总结
        String summary = generateSummary(chatClient, finalResult.toString(), userInput, context);
        finalResult.append("## 最终总结\n").append(summary);

        return finalResult.toString();
    }

    private static String buildSequentialInput(String previousOutput,
                                                MultiAgentContext.AgentRole role,
                                                int index,
                                                MultiAgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 任务\n\n");

        if (index == 0) {
            sb.append("请根据以下用户需求，从【").append(role.getName()).append("】的视角进行分析：\n\n");
            sb.append(previousOutput);
        } else {
            sb.append("请基于前一个 Agent 的分析结果，从【").append(role.getName()).append("】的视角进行补充和完善：\n\n");
            sb.append("=== 前序分析 ===\n").append(truncate(previousOutput, 2000)).append("\n");
        }

        sb.append("\n\n你的角色: ").append(role.getName()).append(" - ").append(role.getDescription());
        sb.append("\n请用中文给出专业分析。");
        return sb.toString();
    }

    private static String generateSummary(ChatClient chatClient,
                                           String allResults,
                                           String originalInput,
                                           MultiAgentContext context) {
        String prompt = "请基于以下多个专家 Agent 的分析结果，给出综合总结和建议：\n\n"
                + "### 原始需求\n" + originalInput + "\n\n"
                + "### 各 Agent 分析\n" + allResults + "\n\n"
                + "请用中文总结关键发现并给出最终建议。";

        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            return "（总结生成失败：" + e.getMessage() + "）";
        }
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...(truncated)";
    }

    /**
     * Agent 执行器函数式接口。
     *
     * <p>定义单个 Agent 的执行契约，各协作模式通过此接口调度 Agent。
     */
    @FunctionalInterface
    public interface AgentRunner {
        /**
         * 执行单个 Agent 任务。
         *
         * @param chatClient {@link ChatClient} 实例
         * @param role       Agent 角色定义
         * @param taskInput  任务输入
         * @return Agent 执行结果
         */
        String run(ChatClient chatClient, MultiAgentContext.AgentRole role, String taskInput);
    }
}
