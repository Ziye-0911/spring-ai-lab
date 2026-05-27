package com.liziye.spring.ai.lab.core.tool;

import java.util.Map;
import java.util.Set;

/**
 * 工具注册中心接口。
 *
 * <p>提供自定义的动态注册/注销能力。
 *
 * <p>注意：Spring AI 1.1.5 对 tool 相关 API 进行了重构，
 * 原 {@code ToolRegistrar} / {@code ToolRegistration} 接口已移除或重命名，
 * 此处定义独立的工具注册抽象。
 *
 * @author liziye
 * @since 1.0.0
 */
public interface ToolRegistry {

    /**
     * 注册一个工具对象（扫描其 @Tool 注解的方法）。
     *
     * @param name     工具名称（全局唯一）
     * @param toolBean 包含 @Tool 方法的 Spring Bean
     */
    void register(String name, Object toolBean);

    /**
     * 以元数据格式注册工具。
     * 适用于通过 MCP 等协议远程注册的工具。
     *
     * @param name         工具名称
     * @param metadata     工具元数据（描述、参数等）
     */
    void register(String name, Map<String, Object> metadata);

    /**
     * 查找指定名称的工具元数据。
     *
     * @param name 工具名称
     * @return 工具元数据，未找到返回 null
     */
    Map<String, Object> getTool(String name);

    /**
     * 移除工具。
     *
     * @param name 工具名称
     */
    void unregister(String name);

    /**
     * 获取所有工具名称列表。
     *
     * @return 工具名称集合
     */
    Set<String> listToolNames();

    /**
     * 获取工具数量。
     *
     * @return 已注册工具数量
     */
    int getToolCount();
}
