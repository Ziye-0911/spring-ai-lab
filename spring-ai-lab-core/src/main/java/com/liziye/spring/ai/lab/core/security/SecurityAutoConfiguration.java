package com.liziye.spring.ai.lab.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * 安全与限流自动配置。
 *
 * <p>仅当 {@code spring.ai.lab.security.rate-limit.enabled=true} 时激活。
 * 为不同场景预创建限流器实例。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "chatRateLimiter")
    @ConditionalOnProperty(prefix = "spring.ai.lab.security.rate-limit", name = "enabled",
            havingValue = "true")
    public RateLimiter chatRateLimiter(SecurityProperties securityProperties) {
        double permits = securityProperties.getRateLimit().getChatPermitsPerSecond();
        log.info("Chat RateLimiter initialized: {} permits/sec", permits);
        return new RateLimiter(permits);
    }

    @Bean
    @ConditionalOnMissingBean(name = "ragRateLimiter")
    @ConditionalOnProperty(prefix = "spring.ai.lab.security.rate-limit", name = "enabled",
            havingValue = "true")
    public RateLimiter ragRateLimiter(SecurityProperties securityProperties) {
        double permits = securityProperties.getRateLimit().getRagPermitsPerSecond();
        log.info("RAG RateLimiter initialized: {} permits/sec", permits);
        return new RateLimiter(permits);
    }

    @Bean
    @ConditionalOnMissingBean(name = "dataAnalysisRateLimiter")
    @ConditionalOnProperty(prefix = "spring.ai.lab.security.rate-limit", name = "enabled",
            havingValue = "true")
    public RateLimiter dataAnalysisRateLimiter(SecurityProperties securityProperties) {
        double permits = securityProperties.getRateLimit().getDataAnalysisPermitsPerSecond();
        log.info("DataAnalysis RateLimiter initialized: {} permits/sec", permits);
        return new RateLimiter(permits);
    }

    @Bean
    @ConditionalOnMissingBean(name = "globalRateLimiter")
    @ConditionalOnProperty(prefix = "spring.ai.lab.security.rate-limit", name = "enabled",
            havingValue = "true")
    public RateLimiter globalRateLimiter(SecurityProperties securityProperties) {
        double permits = securityProperties.getRateLimit().getGlobalPermitsPerSecond();
        log.info("Global RateLimiter initialized: {} permits/sec", permits);
        return new RateLimiter(permits);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitInterceptor rateLimitInterceptor(SecurityProperties securityProperties) {
        return new RateLimitInterceptor(securityProperties);
    }
}
