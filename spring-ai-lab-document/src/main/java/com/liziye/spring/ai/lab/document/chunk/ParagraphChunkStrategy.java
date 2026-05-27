package com.liziye.spring.ai.lab.document.chunk;

import com.liziye.spring.ai.lab.core.document.ChunkStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按段落切分策略。
 *
 * <p>尽量保持段落完整性进行切分，按双换行/单换行识别段落边界。
 * 对于超大段落会回退到 {@link FixedSizeChunkStrategy} 进行二次切分。
 *
 * <p>适用场景：需要保留文档语义结构的场景。
 *
 * <p>实现 {@link com.liziye.spring.ai.lab.core.document.ChunkStrategy} 接口。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class ParagraphChunkStrategy implements ChunkStrategy {

    private static final int DEFAULT_CHUNK_SIZE = 600;
    private static final int DEFAULT_OVERLAP_SIZE = 80;

    private final int chunkSize;
    private final int overlapSize;

    public ParagraphChunkStrategy() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    public ParagraphChunkStrategy(int chunkSize, int overlapSize) {
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

        // 按段落切分（双换行、单换行）
        String[] paragraphs = content.split("\\n\\s*\\n|(?<=\\n)(?=[\\u4e00-\\u9fff])");

        List<Document> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int actualOverlap = Math.min(overlapSize, chunkSize / 2);

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            // 如果当前段落太大，单独切分
            if (trimmed.length() > chunkSize * 2) {
                // 先保存当前块
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(document, currentChunk.toString().trim(), chunkIndex++, chunkSize, actualOverlap));
                    currentChunk.setLength(0);
                }
                // 对大段落做固定大小切分
                FixedSizeChunkStrategy fixedChunker = new FixedSizeChunkStrategy(chunkSize, actualOverlap);
                Document subDoc = new Document(trimmed, new HashMap<>(document.getMetadata()));
                List<Document> subChunks = fixedChunker.chunk(subDoc, chunkSize, actualOverlap);
                for (Document subChunk : subChunks) {
                    subChunk.getMetadata().put("chunk_index", chunkIndex);
                    subChunk.getMetadata().put("chunk_strategy", name());
                    chunks.add(subChunk);
                    chunkIndex++;
                }
                continue;
            }

            // 尝试合并到当前块
            if (currentChunk.length() + trimmed.length() + 1 <= chunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(trimmed);
            } else {
                // 当前块已满，保存并开始新块
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(document, currentChunk.toString().trim(), chunkIndex++, chunkSize, actualOverlap));
                }
                currentChunk.setLength(0);
                currentChunk.append(trimmed);
            }
        }

        // 保存最后一块
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(document, currentChunk.toString().trim(), chunkIndex++, chunkSize, actualOverlap));
        }

        log.debug("Paragraph chunk: {} -> {} chunks (size={})",
                document.getId() != null ? document.getId() : "unknown",
                chunks.size(), chunkSize);

        return chunks;
    }

    private Document createChunk(Document source, String text, int index, int size, int overlap) {
        Map<String, Object> metadata = new HashMap<>(source.getMetadata());
        metadata.put("chunk_index", index);
        metadata.put("chunk_size", size);
        metadata.put("chunk_overlap", overlap);
        metadata.put("chunk_strategy", name());
        return new Document(text, metadata);
    }

    @Override
    public String name() {
        return "paragraph";
    }

    @Override
    public int defaultChunkSize() {
        return chunkSize;
    }

    @Override
    public int defaultOverlapSize() {
        return overlapSize;
    }
}
