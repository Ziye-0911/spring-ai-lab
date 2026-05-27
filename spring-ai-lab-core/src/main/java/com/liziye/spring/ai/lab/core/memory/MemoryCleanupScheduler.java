package com.liziye.spring.ai.lab.core.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 对话记忆定时清理器。
 *
 * <p>定时扫描并移除过期的会话，释放内存。
 * 仅当 {@link InMemoryConversationMemory} Bean 存在时生效。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnBean(InMemoryConversationMemory.class)
public class MemoryCleanupScheduler {

    private final InMemoryConversationMemory memoryManager;

    public MemoryCleanupScheduler(InMemoryConversationMemory memoryManager) {
        this.memoryManager = memoryManager;
    }

    @Scheduled(fixedRateString = "${spring.ai.lab.memory.cleanup-interval-minutes:60}",
            initialDelayString = "${spring.ai.lab.memory.cleanup-interval-minutes:60}",
            timeUnit = TimeUnit.MINUTES)
    public void cleanupExpiredConversations() {
        memoryManager.scheduledCleanup();
    }
}
