package com.example.enterprise.upload.controller;

import com.example.enterprise.common.Result;
import com.example.enterprise.upload.entity.UploadProgress;
import com.example.enterprise.upload.service.ChunkStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 大文件分片上传 + 断点续传接口
 * <p>
 * 流程:
 * 1. 前端计算文件 MD5 作为 identifier
 * 2. 前端调用 /check 查询已上传分片(实现断点续传)
 * 3. 前端逐个上传分片到 /upload
 * 4. 前端调用 /merge 合并所有分片
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
public class ChunkUploadController {

    @Resource
    private ChunkStorageService chunkStorageService;

    /**
     * 上传单个分片
     *
     * @param file         分片文件
     * @param identifier   文件标识(MD5)
     * @param chunkNumber  分片序号(从 0 开始)
     * @param totalChunks  分片总数
     * @param filename     原始文件名
     * @param totalSize    文件总大小
     * @return 返回是否已传完
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadChunk(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("identifier") String identifier,
                                                   @RequestParam("chunkNumber") Integer chunkNumber,
                                                   @RequestParam("totalChunks") Integer totalChunks,
                                                   @RequestParam("filename") String filename,
                                                   @RequestParam(value = "totalSize", required = false) Long totalSize,
                                                   @RequestParam(value = "chunkSize", required = false) Long chunkSize) {
        // 参数校验: 分片序号与总数必须合法
        if (identifier == null || identifier.length() < 8 || identifier.length() > 64) {
            throw new IllegalArgumentException("非法的文件标识");
        }
        if (chunkNumber == null || totalChunks == null
                || chunkNumber < 0 || totalChunks <= 0 || chunkNumber >= totalChunks) {
            throw new IllegalArgumentException("非法的分片参数");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("分片文件不能为空");
        }

        long size = totalSize == null ? 0 : totalSize;
        long cSize = chunkSize == null ? 0 : chunkSize;
        boolean allUploaded = chunkStorageService.saveChunk(
                identifier, chunkNumber, totalChunks, file, filename, size, cSize);

        Map<String, Object> data = new HashMap<>();
        data.put("chunkNumber", chunkNumber);
        data.put("allUploaded", allUploaded);
        return Result.success("第 " + (chunkNumber + 1) + "/" + totalChunks + " 个分片上传成功", data);
    }

    /**
     * 查询上传进度(断点续传: 返回已上传分片)
     */
    @GetMapping("/check")
    public Result<UploadProgress> check(@RequestParam("identifier") String identifier,
                                        @RequestParam("filename") String filename,
                                        @RequestParam("totalSize") Long totalSize,
                                        @RequestParam("totalChunks") Integer totalChunks,
                                        @RequestParam(value = "chunkSize", required = false) Long chunkSize) {
        long size = chunkSize == null ? 0 : chunkSize;
        UploadProgress progress = chunkStorageService.getProgress(
                identifier, filename, totalSize, totalChunks, size);
        return Result.success(progress);
    }

    /**
     * 合并分片
     */
    @PostMapping("/merge")
    public Result<Map<String, Object>> merge(@RequestParam("identifier") String identifier,
                                             @RequestParam("filename") String filename,
                                             @RequestParam("totalChunks") Integer totalChunks) {
        String url = chunkStorageService.mergeChunks(identifier, totalChunks, filename);
        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("downloadUrl", url);
        return Result.success("文件合并成功", data);
    }
}
