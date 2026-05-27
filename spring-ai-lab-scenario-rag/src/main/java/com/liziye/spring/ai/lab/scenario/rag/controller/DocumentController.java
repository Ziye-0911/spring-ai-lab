package com.liziye.spring.ai.lab.scenario.rag.controller;

import com.liziye.spring.ai.lab.core.model.ApiResult;
import com.liziye.spring.ai.lab.scenario.rag.pipeline.EtlPipeline;
import com.liziye.spring.ai.lab.scenario.rag.pipeline.EtlPipelineMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档管理 REST 控制器。
 *
 * <p>提供文档上传和 ETL 进度查询接口：
 * <ul>
 *   <li>POST /api/documents/upload — 上传文档并触发 ETL 处理</li>
 *   <li>GET /api/documents/progress — 获取 ETL 处理进度</li>
 * </ul>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final EtlPipeline etlPipeline;

    /**
     * 上传文档并触发 ETL 处理。
     *
     * <p>接收多个文件，保存为临时文件后提交到 {@link EtlPipeline} 进行处理。
     * 返回处理结果包括已处理文件数、失败文件数、总块数和耗时。
     *
     * @param files 上传的文件列表
     * @return 处理结果（已处理文件数、失败文件数、总块数、耗时）
     */
    @PostMapping("/upload")
    public ApiResult<Map<String, Object>> upload(@RequestParam("files") List<MultipartFile> files) {
        int totalChunks = 0;

        List<File> tempFiles = files.stream().map(mf -> {
            try {
                Path tempFile = Files.createTempFile("rag-upload-", "-" + mf.getOriginalFilename());
                mf.transferTo(tempFile.toFile());
                return tempFile.toFile();
            } catch (IOException e) {
                log.error("[UPLOAD] Failed to save file {}", mf.getOriginalFilename(), e);
                return null;
            }
        }).filter(f -> f != null).collect(Collectors.toList());

        try {
            totalChunks = etlPipeline.processFiles(tempFiles);
        } finally {
            tempFiles.forEach(File::delete);
        }

        EtlPipelineMonitor monitor = etlPipeline.getMonitor();
        return ApiResult.success(Map.of(
                "processedFiles", monitor.getProcessedFiles().get(),
                "failedFiles", monitor.getFailedFiles().get(),
                "totalChunks", totalChunks,
                "elapsedMs", monitor.getElapsedMs()
        ));
    }

    /**
     * 获取 ETL 处理进度。
     *
     * @return 当前 ETL 进度信息（进度百分比、已处理、失败、总文件数、总块数、耗时）
     */
    @GetMapping("/progress")
    public ApiResult<Map<String, Object>> progress() {
        EtlPipelineMonitor monitor = etlPipeline.getMonitor();
        return ApiResult.success(Map.of(
                "progress", monitor.getProgress(),
                "processed", monitor.getProcessedFiles().get(),
                "failed", monitor.getFailedFiles().get(),
                "total", monitor.getTotalFiles().get(),
                "totalChunks", monitor.getTotalChunks().get(),
                "elapsedMs", monitor.getElapsedMs()
        ));
    }
}
