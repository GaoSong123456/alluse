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
 * 延迟队列消费者
 * <p>
 * 场景: 订单超时未支付自动关闭、定时任务化处理。
 * Producer 发送时指定 delayLevel, Broker 会延迟到指定时间后再投递。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_DELAY_DLQ,
        consumerGroup = MqConstant.GROUP_DELAY_CONSUMER,
        selectorExpression = MqConstant.TAG_DELAY
)
public class DelayConsumer implements RocketMQListener<String> {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(String json) {
        try {
            MessageBody body = objectMapper.readValue(json, MessageBody.class);
            long now = System.currentTimeMillis();
            long delay = now - body.getTimestamp();
            log.info("[延迟消费] 收到延迟消息 -> bizKey={}, payload={}, 实际延迟={}ms, 当前时间={}",
                    body.getBizKey(), body.getPayload(), delay, now);
        } catch (Exception e) {
            log.error("[延迟消费] 异常", e);
        }
    }
}
