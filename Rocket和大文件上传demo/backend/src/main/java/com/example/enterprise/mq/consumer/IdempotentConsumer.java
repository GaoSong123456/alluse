package com.example.enterprise.mq.consumer;

import com.example.enterprise.config.EnterpriseProperties;
import com.example.enterprise.mq.constant.MqConstant;
import com.example.enterprise.mq.entity.MessageBody;
import com.example.enterprise.mq.idempotent.IdempotentStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 幂等性演示消费者
 * <p>
 * 场景: 消息可能被重复投递(如生产者重试、Consumer 消费成功但 ACK 丢失)。
 * 通过 业务消息ID + IdempotentStorage 实现幂等, 保证业务只被执行一次。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_IDEMPOTENT,
        consumerGroup = MqConstant.GROUP_COMMON_CONSUMER,
        selectorExpression = "*"
)
public class IdempotentConsumer implements RocketMQListener<String> {

    @Resource
    private IdempotentStorage idempotentStorage;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private EnterpriseProperties properties;

    @Override
    public void onMessage(String json) {
        MessageBody body = null;
        try {
            body = objectMapper.readValue(json, MessageBody.class);
            String msgId = body.getMsgId();
            String bizKey = body.getBizKey();

            // 核心: 幂等判断, 保留时间由配置控制
            boolean first = idempotentStorage.tryLock(msgId, properties.getIdempotentTtl());
            if (!first) {
                log.warn("[幂等] 检测到重复消息, 已忽略 -> msgId={}, bizKey={}", msgId, bizKey);
                return;
            }

            // 幂等锁获取成功后, 必须保证: 业务成功保留锁(防止重试), 业务失败释放锁(允许重试)
            try {
                log.info("[幂等] 首次处理消息 -> msgId={}, bizKey={}, payload={}", msgId, bizKey, body.getPayload());

                // 模拟业务处理
                Thread.sleep(50);
                log.info("[幂等] 业务处理完成 -> bizKey={}", bizKey);
            } catch (Exception e) {
                // 业务处理失败: 释放幂等锁, 允许 RocketMQ 重试该消息
                idempotentStorage.remove(msgId);
                throw new RuntimeException("幂等业务处理失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            // 解析失败或业务失败, 抛出异常让 RocketMQ 重试
            log.error("[幂等] 消费失败, 将触发重试: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
