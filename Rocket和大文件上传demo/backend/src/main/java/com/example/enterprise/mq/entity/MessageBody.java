package com.example.enterprise.mq.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * 消息体封装: 统一携带业务消息 ID 用于幂等判断
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageBody implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务消息唯一 ID(用于幂等) */
    private String msgId;

    /** 业务类型 */
    private String bizType;

    /** 业务主键(如订单号) */
    private String bizKey;

    /** 业务内容 */
    private Object payload;

    /** 发送时间戳 */
    private Long timestamp;

    /**
     * 构建带唯一 ID 的消息体
     */
    public static MessageBody of(String bizType, String bizKey, Object payload) {
        return MessageBody.builder()
                .msgId(UUID.randomUUID().toString().replace("-", ""))
                .bizType(bizType)
                .bizKey(bizKey)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
