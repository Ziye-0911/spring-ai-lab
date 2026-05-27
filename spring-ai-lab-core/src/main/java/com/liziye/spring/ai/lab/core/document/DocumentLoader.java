package com.liziye.spring.ai.lab.core.document;

import com.liziye.spring.ai.lab.core.exception.DocumentLoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 文档加载器接口 — 将各种格式的文档加载为 Spring AI Document 对象。
 *
 * <p>框架通过 SPI 机制自动发现所有 {@link DocumentLoader} 实现，
 * 根据文件扩展名选择合适的加载器。
 *
 * @author liziye
 * @since 1.0.0
 */
public interface DocumentLoader {

    Logger log = LoggerFactory.getLogger(DocumentLoader.class);

    /** 该加载器支持的文件扩展名列表 */
    String[] supportedExtensions();

    /**
     * 加载指定路径的文档文件。
     *
     * @param path 文档文件路径
     * @return 加载的 Document 列表
     * @throws DocumentLoadException 加载失败时抛出
     */
    List<Document> load(String path) throws DocumentLoadException;

    /**
     * 直接加载 File 对象。
     *
     * @param file 文档文件
     * @return 加载的 Document 列表
     * @throws DocumentLoadException 加载失败时抛出
     */
    default List<Document> load(File file) throws DocumentLoadException {
        return load(file.getAbsolutePath());
    }

    /**
     * 从输入流加载（适用于网络流、内存数据、临时文件等场景）。
     *
     * @param inputStream 输入流
     * @param extension   文件扩展名（如 "pdf"、"docx"），用于确定格式
     * @return 加载的 Document 列表
     * @throws DocumentLoadException 加载失败时抛出
     */
    List<Document> load(InputStream inputStream, String extension) throws DocumentLoadException;

    /**
     * 批量加载多个文档文件。
     *
     * @param paths 文件路径列表
     * @return 所有文件加载的 Document 列表（扁平化）
     * @throws DocumentLoadException 加载失败时抛出
     */
    default List<Document> loadBatch(List<String> paths) throws DocumentLoadException {
        List<Document> result = new ArrayList<>();
        for (String path : paths) {
            try {
                result.addAll(load(path));
            } catch (DocumentLoadException e) {
                // 默认行为：记录日志后继续（SKIP 策略）
                log.warn("Failed to load document: {}, error: {}", path, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 异步加载单个文档。
     *
     * @param path 文件路径
     * @return CompletableFuture，完成后返回 Document 列表
     */
    default CompletableFuture<List<Document>> loadAsync(String path) {
        return CompletableFuture.supplyAsync(() -> load(path));
    }

    /**
     * 异步批量加载（并行）。
     *
     * @param paths 文件路径列表
     * @return CompletableFuture，所有文件加载完成后合并返回
     */
    default CompletableFuture<List<Document>> loadBatchAsync(List<String> paths) {
        List<CompletableFuture<List<Document>>> futures = paths.stream()
                .map(this::loadAsync)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .collect(Collectors.toList()));
    }

    /** 判断该加载器是否能处理指定文件 */
    default boolean supports(String path) {
        String ext = getFileExtension(path).toLowerCase();
        return Arrays.asList(supportedExtensions()).contains(ext);
    }

    /** 提取文件扩展名 */
    default String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return path.substring(lastDot + 1);
    }
}
