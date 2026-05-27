package com.liziye.spring.ai.lab.scenario.codereview.git;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Git Diff 解析器。
 *
 * <p>解析 unified diff 格式，提取变更文件和代码片段。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class GitDiffParser {

    private static final Pattern DIFF_FILE_PATTERN = Pattern.compile(
            "^diff --git a/(.+) b/(.+)$");
    private static final Pattern INDEX_PATTERN = Pattern.compile(
            "^index ([0-9a-f]+)\\.\\.([0-9a-f]+).*$");
    private static final Pattern HUNK_HEADER_PATTERN = Pattern.compile(
            "^@@ -(\\d+),?(\\d+)? \\+(\\d+),?(\\d+)? @@.*$");

    /**
     * 解析 Diff 文本为结构化变更列表。
     *
     * @param diffText unified diff 格式文本
     * @return 文件变更列表
     */
    public List<FileChange> parse(String diffText) {
        List<FileChange> changes = new ArrayList<>();

        if (diffText == null || diffText.trim().isEmpty()) {
            return changes;
        }

        String[] lines = diffText.split("\n");
        FileChange currentFile = null;

        for (String line : lines) {
            // 匹配文件头: diff --git a/xxx b/xxx
            Matcher fileMatcher = DIFF_FILE_PATTERN.matcher(line);
            if (fileMatcher.find()) {
                if (currentFile != null) {
                    changes.add(currentFile);
                }
                currentFile = new FileChange();
                currentFile.setFilePath(fileMatcher.group(2));
                continue;
            }

            if (currentFile == null) continue;

            // 添加行
            currentFile.getLines().add(line);

            // 统计变更
            if (line.startsWith("+") && !line.startsWith("+++")) {
                currentFile.setAdditions(currentFile.getAdditions() + 1);
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                currentFile.setDeletions(currentFile.getDeletions() + 1);
            }
        }

        if (currentFile != null) {
            changes.add(currentFile);
        }

        log.info("[DIFF] Parsed {} files with changes", changes.size());
        return changes;
    }

    /**
     * 从 Diff 中提取指定文件的变更摘要。
     */
    public String extractSummary(List<FileChange> changes) {
        return changes.stream()
                .map(fc -> String.format("- %s (+%d/-%d)",
                        fc.getFilePath(), fc.getAdditions(), fc.getDeletions()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 文件变更 POJO。
     *
     * <p>封装单个文件的变更信息，包括文件路径、增删行数和原始行数据。
     */
    public static class FileChange {
        private String filePath;
        private int additions = 0;
        private int deletions = 0;
        private List<String> lines = new ArrayList<>();

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public int getAdditions() { return additions; }
        public void setAdditions(int additions) { this.additions = additions; }
        public int getDeletions() { return deletions; }
        public void setDeletions(int deletions) { this.deletions = deletions; }
        public List<String> getLines() { return lines; }
        public void setLines(List<String> lines) { this.lines = lines; }

        public String getChangeSummary() {
            return filePath + " (+" + additions + "/-" + deletions + ")";
        }
    }
}
