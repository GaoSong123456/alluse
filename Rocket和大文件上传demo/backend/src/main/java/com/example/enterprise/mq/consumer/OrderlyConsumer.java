package com.example.enterprise.mq.consumer;

import com.example.enterprise.mq.constant.MqConstant;
import com.example.enterprise.mq.entity.MessageBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 顺序消息消费者
 * <p>
 * 关键点:
 * - consumeMode = CONSUME_MODE_ORDERLY: 会串行消费同一 MessageQueue 的消息
 * - 生产者使用 syncSendOrderly(topic, msg, orderId) 保证同一订单进入同一队列
 * - MessageModel 使用 CLUSTERING(默认, 集群模式)
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_ORDER,
        consumerGroup = MqConstant.GROUP_ORDER_CONSUMER,
        consumeMode = ConsumeMode.CONSUME_MODE_ORDERLY,
        messageModel = MessageModel.CLUSTERING,
        selectorExpression = "*"
)
public class OrderlyConsumer implements RocketMQListener<String> {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(String json) {
        try {
            MessageBody body = objectMapper.readValue(json, MessageBody.class);
            String bizKey = body.getBizKey();
            String tag = body.getBizType();
            // 模拟业务耗时
            long cost = (long) (Math.random() * 200);
            Thread.sleep(cost);
            log.info("[顺序消费] orderId={}, 阶段={}, 单条耗时={}ms", bizKey, tag, cost);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("[顺序消费] 异常", e);
        }
    }
}
