-- ======================================================
-- 企业级 Demo 数据库初始化脚本 (MySQL 8.0)
-- 数据库: enterprise_demo
-- ======================================================

CREATE DATABASE IF NOT EXISTS enterprise_demo
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE enterprise_demo;

-- ------------------------------------------------------
-- 分片上传文件元数据表
-- 用于断点续传: 记录文件上传任务及已上传分片数量
-- ------------------------------------------------------
DROP TABLE IF EXISTS t_upload_file;
CREATE TABLE t_upload_file (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    identifier      VARCHAR(64)  NOT NULL COMMENT '文件唯一标识(前端MD5)',
    filename        VARCHAR(255) NOT NULL COMMENT '原始文件名',
    total_size      BIGINT       NOT NULL DEFAULT 0 COMMENT '文件总大小(字节)',
    total_chunks    INT          NOT NULL DEFAULT 0 COMMENT '分片总数',
    chunk_size      BIGINT       NOT NULL DEFAULT 0 COMMENT '分片大小(字节)',
    uploaded_chunks INT          NOT NULL DEFAULT 0 COMMENT '已上传分片数',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=上传中 1=已合并 2=失败',
    file_url        VARCHAR(500) DEFAULT NULL COMMENT '合并后文件访问路径',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_identifier (identifier)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='分片上传文件元数据表';

