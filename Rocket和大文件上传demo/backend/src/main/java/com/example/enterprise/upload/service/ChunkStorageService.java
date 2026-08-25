package com.example.enterprise.upload.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.enterprise.common.BusinessException;
import com.example.enterprise.config.EnterpriseProperties;
import com.example.enterprise.upload.entity.UploadFileRecord;
import com.example.enterprise.upload.entity.UploadProgress;
import com.example.enterprise.upload.mapper.UploadFileRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 分片文件存储服务
 * <p>
 * 存储策略:
 * - 分片文件: 文件系统 upload/{identifier}/chunks/xxx.part
 * - 合并文件: 文件系统 upload/{identifier}/merged/xxx.xlsx
 * - 元数据:   MySQL (t_upload_file) 持久化, 支持多实例共享断点续传状态
 */
@Slf4j
@Service
public class ChunkStorageService {

    @Resource
    private EnterpriseProperties properties;

    @Resource
    private UploadFileRecordMapper uploadFileRecordMapper;

    /** 状态常量 */
    private static final int STATUS_UPLOADING = 0;
    private static final int STATUS_MERGED = 1;
    private static final int STATUS_FAILED = 2;

    /**
     * 校验文件标识, 防止目录穿越等安全问题
     * 只允许字母、数字、中划线、下划线, 长度 8-64
     */
    private String validateIdentifier(String identifier) {
        if (identifier == null || !identifier.matches("[a-zA-Z0-9_\\-]{8,64}")) {
            throw new BusinessException("非法的文件标识(identifier)");
        }
        return identifier;
    }

    /**
     * 获取文件分片目录
     */
    private Path getChunkDir(String identifier) {
        return Paths.get(properties.getUploadDir(), validateIdentifier(identifier), "chunks");
    }

    /**
     * 获取合并文件目录
     */
    private Path getMergedDir(String identifier) {
        return Paths.get(properties.getUploadDir(), validateIdentifier(identifier), "merged");
    }

    /**
     * 保存单个分片
     *
     * @param identifier   文件标识
     * @param chunkNumber  分片序号
     * @param totalChunks  分片总数
     * @param file         分片文件
     * @param filename     原始文件名
     * @param totalSize    文件总大小
     * @return 是否已完整上传(所有分片都齐了)
     */
    public synchronized boolean saveChunk(String identifier, int chunkNumber, int totalChunks,
                                          MultipartFile file, String filename,
                                          long totalSize, long chunkSize) {
        try {
            Path chunkDir = getChunkDir(identifier);
            Files.createDirectories(chunkDir);

            Path chunkFile = chunkDir.resolve(chunkNumber + ".part");
            // 若分片已存在则跳过(重复上传/续传场景)
            if (!Files.exists(chunkFile)) {
                file.transferTo(chunkFile.toFile());
            }
            log.info("[分片上传] 保存分片成功 -> identifier={}, chunk={}/{}, size={}",
                    identifier, chunkNumber + 1, totalChunks, file.getSize());

            // 更新数据库元数据
            updateRecordAfterChunk(identifier, filename, totalSize, totalChunks, chunkSize);

            return isAllChunksUploaded(identifier, totalChunks);
        } catch (IOException e) {
            throw new BusinessException("分片保存失败: " + e.getMessage());
        }
    }

    /**
     * 分片上传后更新数据库元数据记录
     */
    private void updateRecordAfterChunk(String identifier, String filename, long totalSize,
                                        int totalChunks, long chunkSize) {
        UploadFileRecord record = queryByIdentifier(identifier);
        if (record == null) {
            // 首次上传, 插入记录
            record = new UploadFileRecord();
            record.setIdentifier(identifier);
            record.setFilename(filename);
            record.setTotalSize(totalSize);
            record.setTotalChunks(totalChunks);
            record.setChunkSize(chunkSize);
            record.setUploadedChunks(1);
            record.setStatus(STATUS_UPLOADING);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            uploadFileRecordMapper.insert(record);
        } else {
            // 已存在, 更新已上传分片数量
            record.setUploadedChunks(countChunksOnDisk(identifier));
            record.setUpdateTime(LocalDateTime.now());
            uploadFileRecordMapper.updateById(record);
        }
    }

    /**
     * 统计磁盘上已存在的分片数(只统计 .part 分片文件)
     */
    private int countChunksOnDisk(String identifier) {
        Path chunkDir = getChunkDir(identifier);
        if (!Files.exists(chunkDir)) {
            return 0;
        }
        try (Stream<Path> stream = Files.list(chunkDir)) {
            return (int) stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".part"))
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 根据 identifier 查询记录
     */
    private UploadFileRecord queryByIdentifier(String identifier) {
        return uploadFileRecordMapper.selectOne(
                new LambdaQueryWrapper<UploadFileRecord>()
                        .eq(UploadFileRecord::getIdentifier, identifier)
                        .last("LIMIT 1"));
    }

    /**
     * 判断所有分片是否都已上传(只统计 .part 分片文件)
     */
    private boolean isAllChunksUploaded(String identifier, int totalChunks) {
        return countChunksOnDisk(identifier) >= totalChunks;
    }

    /**
     * 合并所有分片为完整文件
     *
     * @param identifier  文件标识
     * @param totalChunks 分片总数
     * @param filename    原始文件名
     * @return 合并后文件访问路径
     */
    public synchronized String mergeChunks(String identifier, int totalChunks, String filename) {
        Path chunkDir = getChunkDir(identifier);
        Path mergedDir = getMergedDir(identifier);
        try {
            Files.createDirectories(mergedDir);
        } catch (IOException e) {
            throw new BusinessException("创建合并目录失败");
        }

        // 防止文件名穿越
        String safeName = Paths.get(filename).getFileName().toString();
        Path mergedFile = mergedDir.resolve(safeName);
        String fileUrl = "/files/" + identifier + "/merged/" + safeName;

        // 若已存在且大小正确则直接返回(幂等合并)
        if (Files.exists(mergedFile) && Files.size(mergedFile) > 0) {
            log.info("[分片合并] 文件已存在, 跳过合并 -> {}", mergedFile);
            updateRecordMerged(identifier, fileUrl);
            return fileUrl;
        }

        // 校验分片完整性
        if (!isAllChunksUploaded(identifier, totalChunks)) {
            throw new BusinessException("分片不完整, 无法合并, 请补齐缺失分片");
        }

        try (FileOutputStream fos = new FileOutputStream(mergedFile.toFile())) {
            // 按分片序号升序合并
            List<File> partFiles = Files.list(chunkDir)
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparingInt(p -> parseChunkNumber(p.getFileName().toString())))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            for (File part : partFiles) {
                Files.copy(part.toPath(), fos);
            }
            fos.flush();
        } catch (IOException e) {
            // 合并失败, 更新状态
            updateRecordStatus(identifier, STATUS_FAILED);
            throw new BusinessException("合并失败: " + e.getMessage());
        }

        // 合并完成后删除分片释放空间
        cleanChunks(chunkDir);
        // 更新数据库状态为已合并
        updateRecordMerged(identifier, fileUrl);

        log.info("[分片合并] 合并完成 -> {} , 大小={} bytes", mergedFile, mergedFile.toFile().length());
        return fileUrl;
    }

    /**
     * 更新记录为已合并状态
     */
    private void updateRecordMerged(String identifier, String fileUrl) {
        UploadFileRecord record = queryByIdentifier(identifier);
        if (record != null) {
            record.setStatus(STATUS_MERGED);
            record.setFileUrl(fileUrl);
            record.setUpdateTime(LocalDateTime.now());
            uploadFileRecordMapper.updateById(record);
        }
    }

    /**
     * 更新记录状态
     */
    private void updateRecordStatus(String identifier, int status) {
        UploadFileRecord record = queryByIdentifier(identifier);
        if (record != null) {
            record.setStatus(status);
            record.setUpdateTime(LocalDateTime.now());
            uploadFileRecordMapper.updateById(record);
        }
    }

    /**
     * 解析分片文件名中的序号, 非法则抛异常
     */
    private int parseChunkNumber(String fileName) {
        String num = fileName.replace(".part", "");
        if (!num.matches("\\d+")) {
            throw new BusinessException("非法的分片文件: " + fileName);
        }
        return Integer.parseInt(num);
    }

    /**
     * 查询上传进度(用于断点续传)
     *
     * @param identifier  文件标识
     * @param filename    原始文件名
     * @param totalSize   总大小
     * @param totalChunks 分片总数
     * @param chunkSize   分片大小
     */
    public UploadProgress getProgress(String identifier, String filename, long totalSize,
                                      int totalChunks, long chunkSize) {
        UploadProgress progress = new UploadProgress();
        progress.setIdentifier(identifier);
        progress.setFilename(filename);
        progress.setTotalSize(totalSize);
        progress.setTotalChunks(totalChunks);
        progress.setChunkSize(chunkSize);

        // 从数据库读取状态(跨实例共享)
        UploadFileRecord record = queryByIdentifier(identifier);
        if (record != null && record.getStatus() == STATUS_MERGED) {
            progress.setFinished(true);
            progress.setUploadedChunks(new ArrayList<>());
            return progress;
        }

        // 从文件系统读取已上传的具体分片序号
        List<Integer> uploaded = listUploadedChunks(identifier);
        progress.setUploadedChunks(uploaded);
        progress.setFinished(isAllChunksUploaded(identifier, totalChunks));
        return progress;
    }

    /**
     * 列出已上传的分片序号(只统计 .part 分片文件)
     */
    private List<Integer> listUploadedChunks(String identifier) {
        Path chunkDir = getChunkDir(identifier);
        List<Integer> uploaded = new ArrayList<>();
        if (Files.exists(chunkDir)) {
            try (Stream<Path> stream = Files.list(chunkDir)) {
                uploaded = stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".part"))
                        .map(p -> parseChunkNumber(p.getFileName().toString()))
                        .sorted()
                        .collect(Collectors.toList());
            } catch (IOException ignored) {
            }
        }
        return uploaded;
    }

    /**
     * 清理分片文件
     */
    private void cleanChunks(Path chunkDir) {
        try (Stream<Path> stream = Files.list(chunkDir)) {
            stream.forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
