package com.example.enterprise.upload.entity;

import lombok.Data;

import java.util.List;

/**
 * 分片上传进度信息
 */
@Data
public class UploadProgress {

    /** 文件标识(MD5) */
    private String identifier;

    /** 原始文件名 */
    private String filename;

    /** 文件总大小 */
    private long totalSize;

    /** 分片总数 */
    private int totalChunks;

    /** 分片大小 */
    private long chunkSize;

    /** 已上传分片序号列表(断点续传依据) */
    private List<Integer> uploadedChunks;

    /** 是否已完成全部上传 */
    private boolean finished;
}
