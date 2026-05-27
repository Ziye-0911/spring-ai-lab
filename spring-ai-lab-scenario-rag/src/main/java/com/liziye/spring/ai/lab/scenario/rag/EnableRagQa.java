package com.liziye.spring.ai.lab.scenario.rag;

import com.liziye.spring.ai.lab.core.LabAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 RAG 知识库问答场景。
 *
 * <p>在 Spring Boot 应用启动类添加此注解，自动装配 RAG 知识库问答组件：
 * 文档上传、向量化存储、相似度检索、增强问答。
 *
 * <p>自动导入 {@link RagQaAutoConfiguration} 和核心模块的 {@link com.liziye.spring.ai.lab.core.LabAutoConfiguration}。
 *
 * @author liziye
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({RagQaAutoConfiguration.class, LabAutoConfiguration.class})
public @interface EnableRagQa {
}
