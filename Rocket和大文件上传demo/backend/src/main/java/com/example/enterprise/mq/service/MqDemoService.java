package com.example.enterprise.mq.service;

import com.example.enterprise.common.BusinessException;
import com.example.enterprise.config.EnterpriseProperties;
import com.example.enterprise.mq.constant.MqConstant;
import com.example.enterprise.mq.entity.MessageBody;
import com.example.enterprise.mq.producer.MqProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * RocketMQ 五大特性演示业务服务
 */
@Slf4j
@Service
public class MqDemoService {

    @Resource
    private MqProducerService producerService;

    @Resource
    private EnterpriseProperties properties;

    /**
     * 1. 幂等性演示
     * 发送订单消息, 并模拟重复发送(同一 bizKey 发送两次), 消费端通过 msgId 去重。
     *
     * @param orderId 订单号
     */
    public void demonstrateIdempotent(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        }
        log.info("========== 开始幂等性演示, 订单号: {} ==========", orderId);

        // 发送第一条消息
        MessageBody msg1 = MessageBody.of("订单创建", orderId, "创建订单金额100元");
        producerService.sendSync(MqConstant.TOPIC_IDEMPOTENT, null, msg1);

        if (properties.isSimulateDuplicate()) {
            // 模拟网络重发: 复用同一个 msgId 再次发送(生产环境由 MQ 重投或生产者重试造成)
            log.info("模拟重复消息: 使用相同 msgId={} 再次发送", msg1.getMsgId());
            producerService.sendSync(MqConstant.TOPIC_IDEMPOTENT, null, msg1);
        } else {
            // 发送一条不同 msgId 但相同 bizKey 的消息(业务上也算重复)
            MessageBody msg2 = MessageBody.of("订单创建", orderId, "创建订单金额100元(重发)");
            producerService.sendSync(MqConstant.TOPIC_IDEMPOTENT, null, msg2);
        }
    }

    /**
     * 2. 消息顺序性演示
     * 对一个订单按 创建->支付->发货->完成 顺序发送, 消费端按序执行。
     *
     * @param orderId 订单号
     */
    public void demonstrateOrder(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        }
        log.info("========== 开始顺序性演示, 订单号: {} ==========", orderId);

        // 同一 hashKey(订单号) 保证进入同一队列, 消费端 CONSUME_MODE_ORDERLY 保证顺序
        producerService.sendOrderly(MqConstant.TOPIC_ORDER, MqConstant.TAG_ORDER_CREATE,
                MessageBody.of("创建订单", orderId, "创建订单"), orderId);
        producerService.sendOrderly(MqConstant.TOPIC_ORDER, MqConstant.TAG_ORDER_PAY,
                MessageBody.of("订单支付", orderId, "支付成功"), orderId);
        producerService.sendOrderly(MqConstant.TOPIC_ORDER, MqConstant.TAG_ORDER_SHIP,
                MessageBody.of("订单发货", orderId, "已发货"), orderId);
        producerService.sendOrderly(MqConstant.TOPIC_ORDER, MqConstant.TAG_ORDER_COMPLETE,
                MessageBody.of("订单完成", orderId, "确认收货完成"), orderId);
    }

    /**
     * 3. 可靠性演示
     * 发送同步可靠消息, 消费端前两次失败触发重试, 第三次成功。
     *
     * @param bizId 业务ID
     */
    public void demonstrateReliable(String bizId) {
        if (bizId == null || bizId.isEmpty()) {
            bizId = "BIZ-" + UUID.randomUUID().toString().substring(0, 8);
        }
        log.info("========== 开始可靠性演示, 业务ID: {} ==========", bizId);
        MessageBody msg = MessageBody.of("转账", bizId, "转账100元");
        // 同步发送 + 失败自动重试, 保证消息可靠投递
        producerService.sendSync(MqConstant.TOPIC_RELIABLE, MqConstant.TAG_TRANSFER, msg);
    }

    /**
     * 4. 延迟队列演示
     * 发送延迟消息。
     *
     * @param businessId 业务ID(如订单号)
     * @param delayLevel 延迟级别(1-18)
     */
    public void demonstrateDelay(String businessId, Integer delayLevel) {
        if (businessId == null || businessId.isEmpty()) {
            businessId = "ORDER-" + UUID.randomUUID().toString().substring(0, 8);
        }
        int level = delayLevel == null ? properties.getDelayLevel() : delayLevel;
        if (level < 1 || level > 18) {
            throw new BusinessException("延迟级别必须在 1-18 之间 (1s 5s 10s 30s 1m 2m ... 2h)");
        }
        log.info("========== 开始延迟队列演示, 业务ID: {}, 延迟级别: {} ==========", businessId, level);
        MessageBody msg = MessageBody.of("订单超时关闭", businessId, "超过30分钟未支付, 自动关闭订单");
        producerService.sendDelay(MqConstant.TOPIC_DELAY_DLQ, MqConstant.TAG_DELAY, msg, level);
    }

    /**
     * 5. 死信队列演示
     * 发送一条永远消费失败的消息(通过 payload 标记), 重试耗尽后进入死信队列。
     *
     * @param bizId 业务ID
     */
    public void demonstrateDlx(String bizId) {
        if (bizId == null || bizId.isEmpty()) {
            bizId = "DLX-" + UUID.randomUUID().toString().substring(0, 8);
        }
        log.info("========== 开始死信队列演示, 业务ID: {} ==========", bizId);
        // 标记为"死信演示", 消费者检测到后持续抛异常直至进入 DLQ
        MessageBody msg = MessageBody.of("死信演示", bizId, "payload=DLX_DEMO, 消费会一直失败");
        producerService.sendSync(MqConstant.TOPIC_RELIABLE, MqConstant.TAG_TRANSFER, msg);
    }
}
