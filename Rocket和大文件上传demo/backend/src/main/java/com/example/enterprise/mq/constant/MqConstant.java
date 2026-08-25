package com.example.enterprise.mq.constant;

/**
 * RocketMQ Topic / 消费组常量定义
 * <p>
 * 说明:
 * - Topic 需在 RocketMQ 中预先创建(建议 8 分区), 或用 Producer 自动创建(需开启 autoCreateTopicEnable)
 * - 延迟队列与死信队列都在同一 Topic 上通过消息属性区分
 */
public final class MqConstant {

    private MqConstant() {
    }

    /**
     * 幂等性演示: 订单 Topic
     * 同一个订单可能产生重复消息, 消费端需要幂等处理
     */
    public static final String TOPIC_IDEMPOTENT = "demo_idempotent_topic";

    /**
     * 顺序性演示: 订单 Topic
     * 一个订单的 创建->支付->发货->完成 状态流转消息需按顺序消费
     */
    public static final String TOPIC_ORDER = "demo_order_topic";

    /**
     * 可靠性演示: 可靠消息 Topic
     */
    public static final String TOPIC_RELIABLE = "demo_reliable_topic";

    /**
     * 延迟队列 / 死信队列演示 Topic
     */
    public static final String TOPIC_DELAY_DLQ = "demo_delay_topic";

    /**
     * 顺序消息消费组(需保证单队列顺序)
     */
    public static final String GROUP_ORDER_CONSUMER = "demo-order-consumer-group";

    /**
     * 普通消息消费组
     */
    public static final String GROUP_COMMON_CONSUMER = "demo-common-consumer-group";

    /**
     * 延迟消息消费组
     */
    public static final String GROUP_DELAY_CONSUMER = "demo-delay-consumer-group";

    /**
     * 死信消息消费组
     */
    public static final String GROUP_DLQ_CONSUMER = "demo-dlq-consumer-group";

    /**
     * 消息业务类型 Tags
     */
    public static final String TAG_ORDER_CREATE = "order_create";
    public static final String TAG_ORDER_PAY = "order_pay";
    public static final String TAG_ORDER_SHIP = "order_ship";
    public static final String TAG_ORDER_COMPLETE = "order_complete";
    public static final String TAG_TRANSFER = "transfer";
    public static final String TAG_DELAY = "delay";
}
