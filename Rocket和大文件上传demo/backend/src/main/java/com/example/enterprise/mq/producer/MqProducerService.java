package com.example.enterprise.mq.producer;

import com.example.enterprise.mq.entity.MessageBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 消息生产者封装
 * <p>
 * 统一封装: 普通消息 / 同步可靠消息 / 顺序消息 / 延迟消息
 */
@Slf4j
@Service
public class MqProducerService {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /** 单例 ObjectMapper, 避免频繁创建 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送普通同步消息 (默认可靠投递, 失败会抛异常)
     *
     * @param topic   主题
     * @param tag     标签
     * @param message 消息体
     */
    public void sendSync(String topic, String tag, MessageBody message) {
        String destination = buildDestination(topic, tag);
        Message<String> msg = buildMessage(message);
        rocketMQTemplate.syncSend(destination, msg);
        log.info("[MQ-发送] 同步消息成功 -> destination={}, msgId={}", destination, message.getMsgId());
    }

    /**
     * 发送顺序消息
     * <p>
     * 通过 hashKey(如订单号) 选择固定的 MessageQueue, 保证同一订单的消息按序进入同一队列
     *
     * @param topic   主题
     * @param tag     标签
     * @param message 消息体
     * @param hashKey 分区键(如订单号)
     */
    public void sendOrderly(String topic, String tag, MessageBody message, String hashKey) {
        String destination = buildDestination(topic, tag);
        Message<String> msg = buildMessage(message);
        rocketMQTemplate.syncSendOrderly(destination, msg, hashKey);
        log.info("[MQ-发送] 顺序消息成功 -> destination={}, hashKey={}, msgId={}", destination, hashKey, message.getMsgId());
    }

    /**
     * 发送延迟消息
     * <p>
     * RocketMQ 延迟级别: 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
     *
     * @param topic      主题
     * @param tag        标签
     * @param message    消息体
     * @param delayLevel 延迟级别(1-18)
     */
    public void sendDelay(String topic, String tag, MessageBody message, int delayLevel) {
        String destination = buildDestination(topic, tag);
        Message<String> msg = buildMessage(message);
        rocketMQTemplate.syncSend(destination, msg, 3000, delayLevel);
        log.info("[MQ-发送] 延迟消息成功 -> destination={}, delayLevel={}, msgId={}", destination, delayLevel, message.getMsgId());
    }

    /**
     * 发送异步可靠消息(带回调)
     */
    public void sendAsync(String topic, String tag, MessageBody message) {
        String destination = buildDestination(topic, tag);
        Message<String> msg = buildMessage(message);
        rocketMQTemplate.asyncSend(destination, msg, new org.apache.rocketmq.client.producer.SendCallback() {
            @Override
            public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                log.info("[MQ-发送] 异步消息成功 -> destination={}, msgId={}", destination, message.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                log.error("[MQ-发送] 异步消息失败 -> destination={}, msgId={}", destination, message.getMsgId(), e);
            }
        });
    }

    /**
     * 构造 destination = topic:tag
     */
    private String buildDestination(String topic, String tag) {
        return tag == null || tag.isEmpty() ? topic : topic + ":" + tag;
    }

    /**
     * 构建带唯一 ID 的消息体
     */
    private Message<String> buildMessage(MessageBody message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            return MessageBuilder.withPayload(json)
                    .setHeader("msgId", message.getMsgId())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("消息序列化失败", e);
        }
    }
}
