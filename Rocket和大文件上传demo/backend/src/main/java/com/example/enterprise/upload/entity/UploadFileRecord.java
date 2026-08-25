package com.example.enterprise.upload.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分片上传文件元数据 (MySQL 持久化)
 * <p>
 * 记录一次完整的文件上传任务, 用于断点续传时查询已上传分片, 并支持跨实例共享状态。
 */
@Data
@TableName("t_upload_file")
public class UploadFileRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文件唯一标识(前端 MD5) */
    private String identifier;

    /** 原始文件名 */
    private String filename;

    /** 文件总大小(字节) */
    private Long totalSize;

    /** 分片总数 */
    private Integer totalChunks;

    /** 分片大小(字节) */
    private Long chunkSize;

    /** 已上传分片数 */
    private Integer uploadedChunks;

    /** 状态: 0=上传中 1=已合并 2=失败 */
    private Integer status;

    /** 合并后文件访问路径 */
    private String fileUrl;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
