package com.liziye.spring.ai.lab.scenario.codereview;

import com.liziye.spring.ai.lab.core.LabAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用代码审查场景。
 *
 * <p>将此注解添加到 Spring Boot 启动类上即可自动装配 Code Review 相关组件，
 * 包括 {@link CodeReviewAutoConfiguration} 和 {@link LabAutoConfiguration}。
 *
 * @author liziye
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({CodeReviewAutoConfiguration.class, LabAutoConfiguration.class})
public @interface EnableCodeReview {
}
