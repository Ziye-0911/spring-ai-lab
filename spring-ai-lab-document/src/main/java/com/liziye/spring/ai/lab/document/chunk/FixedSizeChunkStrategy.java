package com.liziye.spring.ai.lab.document.chunk;

import com.liziye.spring.ai.lab.core.document.ChunkStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 固定大小切分策略。
 *
 * <p>按指定字符数切分文档，支持重叠窗口。会在自然断点处（句号、换行）优化切分位置。
 *
 * <p>适用场景：通用文本，不关心文档结构。
 *
 * <p>实现 {@link com.liziye.spring.ai.lab.core.document.ChunkStrategy} 接口。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class FixedSizeChunkStrategy implements ChunkStrategy {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_OVERLAP_SIZE = 100;

    private final int chunkSize;
    private final int overlapSize;

    public FixedSizeChunkStrategy() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    public FixedSizeChunkStrategy(int chunkSize, int overlapSize) {
        this.chunkSize = chunkSize;
        this.overlapSize = Math.min(overlapSize, chunkSize / 2);
    }

    @Override
    public List<Document> chunk(Document document) {
        return chunk(document, chunkSize, overlapSize);
    }

    @Override
    public List<Document> chunk(Document document, int chunkSize, int overlapSize) {
        String content = document.getText();
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<Document> chunks = new ArrayList<>();
        int actualOverlap = Math.min(overlapSize, chunkSize / 2);
        int start = 0;
        int chunkIndex = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            // 尽量在自然断点处切分（句号、换行）
            if (end < content.length()) {
                int breakPoint = findBreakPoint(content, start, end);
                if (breakPoint > start + chunkSize / 2) {
                    end = breakPoint;
                }
            }

            String chunkText = content.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                Map<String, Object> metadata = new HashMap<>(document.getMetadata());
                metadata.put("chunk_index", chunkIndex);
                metadata.put("chunk_start", start);
                metadata.put("chunk_end", end);
                metadata.put("chunk_strategy", name());
                Document chunk = new Document(chunkText, metadata);
                chunks.add(chunk);
                chunkIndex++;
            }

            start = end - actualOverlap;
            if (start >= end) {
                start = end;
            }
        }

        log.debug("Fixed-size chunk: {} -> {} chunks (size={}, overlap={})",
                document.getId() != null ? document.getId() : "unknown",
                chunks.size(), chunkSize, actualOverlap);

        return chunks;
    }

    @Override
    public String name() {
        return "fixed-size";
    }

    @Override
    public int defaultChunkSize() {
        return chunkSize;
    }

    @Override
    public int defaultOverlapSize() {
        return overlapSize;
    }

    /**
     * 在指定范围内查找最佳断点（句号、换行）。
     */
    private int findBreakPoint(String content, int start, int end) {
        // 从后往前找断点
        for (int i = end - 1; i > start + (end - start) / 2; i--) {
            char c = content.charAt(i);
            if (c == '\n' || c == '\r' || c == '。' || c == '！' || c == '？' || c == '.') {
                return i + 1;
            }
        }
        return end;
    }
}
