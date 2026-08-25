package com.example.enterprise.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "enterprise")
public class EnterpriseProperties {

    /** 上传文件存储根目录 */
    private String uploadDir = "./upload";

    /** 分片大小(字节), 默认 5MB */
    private long chunkSize = 5 * 1024 * 1024;

    /** 延迟消息延迟级别(1s 5s 10s 30s 1m ... 2h) */
    private int delayLevel = 3;

    /** 是否模拟消息重复(用于幂等性演示) */
    private boolean simulateDuplicate = true;

    /** 幂等去重保留时间(秒), 默认 300 秒 */
    private long idempotentTtl = 300;
}
