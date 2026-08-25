package com.example.enterprise.mq.consumer;

import com.example.enterprise.mq.constant.MqConstant;
import com.example.enterprise.mq.entity.MessageBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;

/**
 * 可靠性演示消费者
 * <p>
 * 演示消息的可靠性(至少一次投递 + 消费失败重试):
 * - Producer 使用同步发送 + 失败重试(retry-times-when-send-failed=3)
 * - Consumer 消费失败抛出异常时, RocketMQ 会按重试策略重新投递
 * - 达到最大重试次数后消息进入死信队列
 * <p>
 * 使用 Redis INCR 记录每个消息的尝试次数, 支持分布式多实例共享重试状态。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_RELIABLE,
        consumerGroup = MqConstant.GROUP_COMMON_CONSUMER,
        selectorExpression = MqConstant.TAG_TRANSFER
)
public class ReliableConsumer implements RocketMQListener<String> {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 重试计数键前缀 */
    private static final String RETRY_KEY_PREFIX = "reliable:retry:";
    /** 重试计数保留时间(与消费重试窗口匹配) */
    private static final Duration RETRY_TTL = Duration.ofMinutes(10);

    @Override
    public void onMessage(String json) {
        try {
            MessageBody body = objectMapper.readValue(json, MessageBody.class);
            String msgId = body.getMsgId();
            String bizKey = body.getBizKey();

            // 使用 Redis INCR 原子自增, 支持分布式共享
            String key = RETRY_KEY_PREFIX + msgId;
            Long attempt = stringRedisTemplate.opsForValue().increment(key);
            // 首次设置过期时间
            if (attempt != null && attempt == 1L) {
                stringRedisTemplate.expire(key, RETRY_TTL);
            }
            int attemptInt = attempt == null ? 1 : attempt.intValue();

            // 死信演示: payload 含前缀 "payload=DLX_DEMO" 时持续失败, 重试耗尽进入 DLQ
            if (String.valueOf(body.getPayload()).startsWith("payload=DLX_DEMO")) {
                log.warn("[可靠] 死信演示: 第 {} 次消费失败(持续失败直到进入 DLQ) -> msgId={}, bizKey={}",
                        attemptInt, msgId, bizKey);
                throw new IllegalStateException("死信演示: 消费持续失败");
            }

            // 模拟: 前 2 次处理失败, 触发重试; 第 3 次成功
            if (attemptInt < 3) {
                log.warn("[可靠] 第 {} 次消费失败, 将触发重试 -> msgId={}, bizKey={}", attemptInt, msgId, bizKey);
                throw new IllegalStateException("模拟消费失败, 触发重试 attempt=" + attemptInt);
            }

            log.info("[可靠] 第 {} 次消费成功 -> msgId={}, bizKey={}, payload={}",
                    attemptInt, msgId, bizKey, body.getPayload());
            // 消费成功, 清理计数
            stringRedisTemplate.delete(key);
        } catch (RuntimeException e) {
            // 业务/死信演示异常: 抛出让 RocketMQ 重试
            throw e;
        } catch (Exception e) {
            // 解析等其他异常: 包装为 RuntimeException 抛出让 RocketMQ 重试
            log.error("[可靠] 消费失败, 将由 RocketMQ 重试: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
