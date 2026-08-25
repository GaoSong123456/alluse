package com.example.enterprise.mq.consumer;

import com.example.enterprise.mq.constant.MqConstant;
import com.example.enterprise.mq.entity.MessageBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 死信队列消费者
 * <p>
 * 死信队列(DLQ)机制:
 * - 消费组重试达到最大次数后, 消息进入该消费组的死信主题 %DLQ%消费组名
 * - 死信消息需要人工介入或二次补偿处理, 本类监听并记录死信消息
 * <p>
 * 注意: 监听主题为 "%DLQ%consumerGroupName", 若消息重试后进入死信队列, 会被本消费者捕获。
 * (RocketMQ 4.x 使用 %DLQ%分组名, 5.x 结构略有不同, 本示例按 4.x 标准)
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "%DLQ%" + MqConstant.GROUP_COMMON_CONSUMER,
        consumerGroup = MqConstant.GROUP_DLQ_CONSUMER,
        selectorExpression = "*"
)
public class DlxConsumer implements RocketMQListener<String> {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(String json) {
        try {
            MessageBody body = objectMapper.readValue(json, MessageBody.class);
            log.error("[死信消费] 收到死信消息, 需人工介入处理 -> msgId={}, bizKey={}, payload={}",
                    body.getMsgId(), body.getBizKey(), body.getPayload());
        } catch (Exception e) {
            log.error("[死信消费] 解析异常, 原始消息: {}", json, e);
        }
    }
}
