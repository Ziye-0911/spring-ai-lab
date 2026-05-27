package com.liziye.spring.ai.lab.scenario.rag.pipeline;

import com.liziye.spring.ai.lab.core.document.ChunkStrategy;
import com.liziye.spring.ai.lab.core.document.DocumentLoadFailureStrategy;
import com.liziye.spring.ai.lab.core.document.DocumentLoader;
import com.liziye.spring.ai.lab.core.observation.DocumentMetrics;
import com.liziye.spring.ai.lab.document.chunk.FixedSizeChunkStrategy;
import com.liziye.spring.ai.lab.document.chunk.ParagraphChunkStrategy;
import com.liziye.spring.ai.lab.document.loader.*;
import com.liziye.spring.ai.lab.scenario.rag.RagQaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ETL 管道。
 *
 * <p>负责文档处理的完整流程：文档加载 → 切分 → 向量化 → 向量存储入库。
 * 支持多种文档格式（PDF、TXT、Markdown、Word），
 * 按文件扩展名自动选择合适的加载器和切分策略。
 *
 * <p>支持单文件处理（{@link #processFile(File)}）和批量并行处理（{@link #processFiles(List)}），
 * 同时提供向量相似度检索（{@link #search(String, int, double)}）。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class EtlPipeline {

    private final List<DocumentLoader> loaders;
    private final Map<String, ChunkStrategy> chunkStrategies;
    private final VectorStore vectorStore;
    private final RagQaProperties properties;
    private final DocumentLoadFailureStrategy failureStrategy;
    private final DocumentMetrics documentMetrics;
    private final EtlPipelineMonitor monitor;

    public EtlPipeline(VectorStore vectorStore,
                       RagQaProperties properties,
                       DocumentMetrics documentMetrics) {
        this.vectorStore = vectorStore;
        this.properties = properties;
        this.documentMetrics = documentMetrics;
        this.failureStrategy = DocumentLoadFailureStrategy.SKIP;
        this.monitor = new EtlPipelineMonitor();

        // 注册默认加载器
        this.loaders = List.of(
                new PdfDocumentLoader(),
                new TxtDocumentLoader(),
                new MarkdownDocumentLoader(),
                new WordDocumentLoader()
        );

        // 注册切分策略
        this.chunkStrategies = new HashMap<>();
        this.chunkStrategies.put("pdf", new ParagraphChunkStrategy(
                properties.getChunk().getPdfChunkSize(),
                properties.getChunk().getDefaultOverlap()));
        this.chunkStrategies.put("txt", new FixedSizeChunkStrategy(
                properties.getChunk().getTextChunkSize(),
                properties.getChunk().getDefaultOverlap()));
        this.chunkStrategies.put("md", new FixedSizeChunkStrategy(
                properties.getChunk().getTextChunkSize(),
                properties.getChunk().getDefaultOverlap()));
        this.chunkStrategies.put("docx", new ParagraphChunkStrategy(
                properties.getChunk().getDefaultChunkSize(),
                properties.getChunk().getDefaultOverlap()));
    }

    /**
     * 处理单个文件：加载 → 切分 → 入库。
     *
     * @param file 输入文件
     * @return 入库的文档块数量
     */
    public int processFile(File file) {
        String fileName = file.getName();
        String extension = getExtension(fileName);

        log.info("[ETL] Processing file: {}", fileName);

        try {
            // 1. 加载文档
            DocumentLoader loader = findLoader(extension);
            if (loader == null) {
                log.warn("[ETL] No loader for extension: {}", extension);
                return 0;
            }

            List<Document> documents;
            try (InputStream is = new FileInputStream(file)) {
                documents = loader.load(is, extension);
            }

            if (documents == null || documents.isEmpty()) {
                log.warn("[ETL] Empty document: {}", fileName);
                return 0;
            }

            // 2. 切分文档
            ChunkStrategy chunker = chunkStrategies.getOrDefault(extension,
                    new FixedSizeChunkStrategy(properties.getChunk().getDefaultChunkSize(),
                            properties.getChunk().getDefaultOverlap()));

            List<Document> chunks = new ArrayList<>();
            for (Document doc : documents) {
                chunks.addAll(chunker.chunk(doc));
            }

            log.info("[ETL] File={} chunks={}", fileName, chunks.size());

            // 3. 向量化入库
            if (!chunks.isEmpty()) {
                vectorStore.add(chunks);
                documentMetrics.recordVectorStored(true);
                documentMetrics.addEtlTime(0L);
            }

            monitor.incrementProcessed();
            monitor.incrementChunks(chunks.size());
            return chunks.size();

        } catch (Exception e) {
            log.error("[ETL] Failed to process file: {}", fileName, e);
            documentMetrics.recordVectorStored(false);

            switch (failureStrategy) {
                case SKIP:
                    log.warn("[ETL] Skipping failed file: {}", fileName);
                    break;
                case RETRY:
                    log.warn("[ETL] File failed, retry once: {}", fileName);
                    try {
                        return processFile(file);
                    } catch (Exception ex) {
                        log.error("[ETL] Retry also failed for: {}", fileName);
                        // fall through to SKIP
                    }
                    break;
                case ABORT:
                    throw new RuntimeException("ETL pipeline aborted on file: " + fileName, e);
            }

            monitor.incrementFailed();
            return 0;
        }
    }

    /**
     * 批量并行处理文件。
     *
     * @param files 文件列表
     * @return 总入库块数
     */
    public int processFiles(List<File> files) {
        log.info("[ETL] Batch processing {} files", files.size());
        monitor.reset(files.size());

        ExecutorService executor = Executors.newFixedThreadPool(
                properties.getEtl().getParallelThreads());

        try {
            List<CompletableFuture<Integer>> futures = files.stream()
                    .map(file -> CompletableFuture.supplyAsync(() -> processFile(file), executor))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .mapToInt(Integer::intValue)
                    .sum();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 检索相关文档。
     *
     * <p>通过 {@code VectorStore} 进行向量相似度检索。
     *
     * @param query     查询文本
     * @param topK      返回数量
     * @param threshold 相似度阈值
     * @return 按相似度排序的相关文档列表，如果 VectorStore 未配置则返回空列表
     */
    public List<Document> search(String query, int topK, double threshold) {
        // 使用 VectorStore 进行相似度检索
        if (vectorStore == null) {
            log.warn("[ETL] VectorStore not configured, returning empty result");
            return List.of();
        }

        org.springframework.ai.vectorstore.SearchRequest request =
                org.springframework.ai.vectorstore.SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .build();

        return vectorStore.similaritySearch(request);
    }

    public EtlPipelineMonitor getMonitor() {
        return monitor;
    }

    private DocumentLoader findLoader(String extension) {
        return loaders.stream()
                .filter(l -> java.util.Arrays.asList(l.supportedExtensions()).contains(extension))
                .findFirst()
                .orElse(null);
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }
}
