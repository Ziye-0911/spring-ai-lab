package com.liziye.spring.ai.lab.core.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 默认模型路由实现 — 支持请求头/配置/降级三种路由方式。
 *
 * <p>优先级：
 * <ol>
 *   <li>请求头指定（最高优先级）</li>
 *   <li>Context 中配置的默认模型</li>
 *   <li>系统默认模型</li>
 * </ol>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class DefaultModelRouter implements ModelRouter {

    private final String defaultModelName;

    public DefaultModelRouter(String defaultModelName) {
        this.defaultModelName = defaultModelName;
    }

    @Override
    public String route(ModelRoutingContext context) {
        // 1. 请求头指定 → 最高优先级
        if (StringUtils.hasText(context.getRequestHeaderModel())) {
            log.debug("Routing via request header: {}", context.getRequestHeaderModel());
            return context.getRequestHeaderModel();
        }

        // 2. 配置中的默认模型
        if (StringUtils.hasText(context.getConfiguredDefaultModel())) {
            log.debug("Routing via configured default: {}", context.getConfiguredDefaultModel());
            return context.getConfiguredDefaultModel();
        }

        // 3. 系统默认
        log.debug("Routing via system default: {}", defaultModelName);
        return defaultModelName;
    }
}
