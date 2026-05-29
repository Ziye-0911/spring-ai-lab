package com.liziye.spring.ai.lab.document.chunk;

import com.liziye.spring.ai.lab.core.document.ChunkStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 语义感知切分策略。
 *
 * <p>先将文档拆分为句子，再通过相邻句子的字符 bigram Jaccard 相似度识别话题切换点，
 * 在语义边界处切分而不是机械地按固定长度截断。
 *
 * <p>核心算法：
 * <ol>
 *   <li>按句末标点（中英文）拆分句子</li>
 *   <li>逐句累积，当累计长度接近 chunkSize 时，计算下一句与当前块的相似度</li>
 *   <li>相似度低于阈值 → 话题切换 → 在此切分</li>
 *   <li>单个句子超长时回退 {@link FixedSizeChunkStrategy}</li>
 * </ol>
 *
 * <p>适用场景：技术文档、论文、知识库等需要保留语义连贯性的文本。
 *
 * <p>实现 {@link com.liziye.spring.ai.lab.core.document.ChunkStrategy} 接口。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class SemanticChunkStrategy implements ChunkStrategy {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP_SIZE = 50;

    /** 默认相似度阈值：低于该值视为话题切换 */
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;

    private final int chunkSize;
    private final int overlapSize;
    private final double similarityThreshold;

    /** 匹配句末标点：中文。！？… ｜ 英文 .!? 后跟空格或结尾 */
    private static final String SENTENCE_SPLIT_PATTERN =
            "(?<=[。！？…])(?=[^。！？…])|" +
            "(?<=[.!?])(?=\\s+|$)";

    public SemanticChunkStrategy() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE, DEFAULT_SIMILARITY_THRESHOLD);
    }

    public SemanticChunkStrategy(int chunkSize, int overlapSize) {
        this(chunkSize, overlapSize, DEFAULT_SIMILARITY_THRESHOLD);
    }

    public SemanticChunkStrategy(int chunkSize, int overlapSize, double similarityThreshold) {
        this.chunkSize = chunkSize;
        this.overlapSize = Math.min(overlapSize, chunkSize / 2);
        this.similarityThreshold = similarityThreshold;
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

        List<String> sentences = splitSentences(content);
        if (sentences.isEmpty()) {
            return List.of();
        }

        int actualOverlap = Math.min(overlapSize, chunkSize / 2);
        List<Document> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);

            // 单个句子超长，回退固定大小切分
            if (sentence.length() > chunkSize * 2) {
                if (currentChunk.length() > 0) {
                    chunks.add(buildChunk(document, currentChunk.toString().trim(),
                            chunkIndex++, chunkSize, actualOverlap));
                    currentChunk.setLength(0);
                }
                FixedSizeChunkStrategy fixed = new FixedSizeChunkStrategy(chunkSize, actualOverlap);
                Document sub = new Document(sentence, new HashMap<>(document.getMetadata()));
                for (Document subChunk : fixed.chunk(sub, chunkSize, actualOverlap)) {
                    subChunk.getMetadata().put("chunk_index", chunkIndex);
                    subChunk.getMetadata().put("chunk_strategy", name());
                    chunks.add(subChunk);
                    chunkIndex++;
                }
                continue;
            }

            boolean fits = currentChunk.length() + sentence.length() <= chunkSize;

            if (!fits && currentChunk.length() > 0) {
                // 当前块已满，检查下一句与当前块的语义相似度
                double similarity = computeBigramJaccard(currentChunk.toString(), sentence);

                if (similarity >= similarityThreshold) {
                    // 语义连贯：即使超了一点也并入
                    if (currentChunk.length() > 0) currentChunk.append(" ");
                    currentChunk.append(sentence);
                } else {
                    // 话题切换：保存当前块，新句子开新块
                    chunks.add(buildChunk(document, currentChunk.toString().trim(),
                            chunkIndex++, chunkSize, actualOverlap));
                    currentChunk.setLength(0);
                    currentChunk.append(sentence);
                }
            } else if (fits) {
                // 还有空间，直接追加
                if (currentChunk.length() > 0) currentChunk.append(" ");
                currentChunk.append(sentence);
            } else {
                // 当前块为空且句子装不下（理论上不应该到这里，因为上面已处理超长句）
                currentChunk.append(sentence);
            }
        }

        // 保存最后一块
        if (currentChunk.length() > 0) {
            chunks.add(buildChunk(document, currentChunk.toString().trim(),
                    chunkIndex++, chunkSize, actualOverlap));
        }

        // 合并小块：如果最后两个 chunk 合并后仍不超过 chunkSize，则合并
        chunks = mergeSmallChunks(chunks, chunkSize);

        log.debug("Semantic chunk: {} -> {} chunks (size={}, threshold={})",
                document.getId() != null ? document.getId() : "unknown",
                chunks.size(), chunkSize, similarityThreshold);

        return chunks;
    }

    @Override
    public String name() {
        return "semantic";
    }

    @Override
    public int defaultChunkSize() {
        return chunkSize;
    }

    @Override
    public int defaultOverlapSize() {
        return overlapSize;
    }

    // ==================== 内部方法 ====================

    /**
     * 将文本拆分为句子列表。
     */
    static List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] parts = text.split(SENTENCE_SPLIT_PATTERN);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    /**
     * 计算两段文本的字符 bigram Jaccard 相似度。
     *
     * <p>对中英文均适用：中文按字拆分 bigram，英文按单词内的字母组合。
     *
     * @return 0.0 ~ 1.0 之间的相似度
     */
    static double computeBigramJaccard(String textA, String textB) {
        Set<String> bigramsA = extractBigrams(textA);
        Set<String> bigramsB = extractBigrams(textB);

        if (bigramsA.isEmpty() || bigramsB.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(bigramsA);
        intersection.retainAll(bigramsB);

        Set<String> union = new HashSet<>(bigramsA);
        union.addAll(bigramsB);

        return (double) intersection.size() / union.size();
    }

    /**
     * 提取文本中的字符 bigram 集合。
     */
    private static Set<String> extractBigrams(String text) {
        Set<String> bigrams = new HashSet<>();
        if (text == null || text.length() < 2) {
            return bigrams;
        }
        for (int i = 0; i < text.length() - 1; i++) {
            char c1 = text.charAt(i);
            char c2 = text.charAt(i + 1);
            // 跳过空格和纯标点组合，只保留有意义字符对的 bigram
            if (!Character.isWhitespace(c1) && !Character.isWhitespace(c2)) {
                bigrams.add("" + c1 + c2);
            }
        }
        return bigrams;
    }

    /**
     * 合并末尾的小 chunk，避免产生太碎的片段。
     */
    private List<Document> mergeSmallChunks(List<Document> chunks, int maxSize) {
        if (chunks.size() <= 1) return chunks;

        List<Document> merged = new ArrayList<>();
        Document pending = chunks.get(0);

        for (int i = 1; i < chunks.size(); i++) {
            Document current = chunks.get(i);
            if (pending.getText().length() + current.getText().length() <= maxSize) {
                // 合并
                String combined = pending.getText() + " " + current.getText();
                Map<String, Object> meta = new HashMap<>(pending.getMetadata());
                meta.put("chunk_size", maxSize);
                meta.put("chunk_overlap", overlapSize);
                meta.put("chunk_strategy", name());
                pending = new Document(combined, meta);
            } else {
                merged.add(pending);
                pending = current;
            }
        }
        merged.add(pending);

        // 重新编号
        for (int i = 0; i < merged.size(); i++) {
            merged.get(i).getMetadata().put("chunk_index", i);
        }

        return merged;
    }

    private Document buildChunk(Document source, String text, int index,
                                 int size, int overlap) {
        Map<String, Object> metadata = new HashMap<>(source.getMetadata());
        metadata.put("chunk_index", index);
        metadata.put("chunk_size", size);
        metadata.put("chunk_overlap", overlap);
        metadata.put("chunk_strategy", name());
        return new Document(text, metadata);
    }
}
