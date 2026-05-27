package com.liziye.spring.ai.lab.core.routing;

/**
 * 模型路由策略 — 决定每次请求使用哪个模型。
 *
 * <p>支持多种路由方式：
 * <ol>
 *   <li>请求头指定：{@code X-AI-Model: ollama}</li>
 *   <li>配置指定：{@code spring.ai.lab.model.default-provider=openai}</li>
 *   <li>自动路由：按 Token 消耗、延迟、成本自动选择</li>
 * </ol>
 *
 * @author liziye
 * @since 1.0.0
 */
public interface ModelRouter {

    /**
     * 根据请求上下文决定使用哪个模型。
     *
     * @param context 包含请求头、历史配置等
     * @return 模型名称
     */
    String route(ModelRoutingContext context);
}
