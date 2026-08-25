package com.example.enterprise.upload.entity;

import lombok.Data;

/**
 * 分片上传请求参数
 */
@Data
public class ChunkUploadDTO {

    /** 文件唯一标识(前端计算 MD5) */
    private String identifier;

    /** 当前分片序号(从 0 开始) */
    private Integer chunkNumber;

    /** 分片总数 */
    private Integer totalChunks;

    /** 原始文件名 */
    private String filename;

    /** 文件总大小 */
    private Long totalSize;

    /** 分片大小 */
    private Long chunkSize;
}
