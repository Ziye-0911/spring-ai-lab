package com.liziye.spring.ai.lab.core.security;

import com.liziye.spring.ai.lab.core.model.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 限流拦截器 — 在 Controller 层拦截请求并检查令牌。
 *
 * <p>根据请求路径自动选择对应的限流器（通过 {@link RateLimiter} Bean 名称匹配）。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final SecurityProperties securityProperties;

    @Autowired(required = false)
    private Map<String, RateLimiter> rateLimiters;

    public RateLimitInterceptor(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {

        if (!securityProperties.getRateLimit().isEnabled()) {
            return true;
        }

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);
        String key = clientIp + ":" + path;

        // 1. 全局限流
        RateLimiter globalLimiter = findLimiter("globalRateLimiter");
        if (globalLimiter != null && !globalLimiter.tryAcquire(clientIp)) {
            return reject(response, "全局限流");
        }

        // 2. 场景限流
        RateLimiter scenarioLimiter = findScenarioLimiter(path);
        if (scenarioLimiter != null && !scenarioLimiter.tryAcquire(key)) {
            return reject(response, "场景限流: " + path);
        }

        return true;
    }

    private RateLimiter findScenarioLimiter(String path) {
        if (rateLimiters == null) return null;

        if (path.contains("/chat")) return findLimiter("chatRateLimiter");
        if (path.contains("/rag")) return findLimiter("ragRateLimiter");
        if (path.contains("/data")) return findLimiter("dataAnalysisRateLimiter");

        return null;
    }

    private RateLimiter findLimiter(String beanName) {
        if (rateLimiters == null) return null;
        return rateLimiters.get(beanName);
    }

    private boolean reject(HttpServletResponse response, String reason) throws Exception {
        log.warn("[RATE-LIMIT] {} triggered", reason);
        response.setStatus(securityProperties.getRateLimit().getTooManyRequestsStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":429,\"message\":\"" +
                securityProperties.getRateLimit().getTooManyRequestsMessage() +
                "\",\"data\":null}");
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
