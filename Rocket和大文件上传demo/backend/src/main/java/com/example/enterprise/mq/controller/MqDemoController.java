package com.example.enterprise.mq.controller;

import com.example.enterprise.common.Result;
import com.example.enterprise.mq.service.MqDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * RocketMQ 五大特性演示接口
 */
@RestController
@RequestMapping("/api/mq")
public class MqDemoController {

    @Resource
    private MqDemoService mqDemoService;

    /** 1. 幂等性演示: 发送订单消息(含模拟重复) */
    @GetMapping("/idempotent")
    public Result<String> idempotent(@RequestParam(required = false) String orderId) {
        mqDemoService.demonstrateIdempotent(orderId);
        return Result.success("幂等性演示消息已发送(含重复消息), 请查看后端日志确认只处理一次", orderId);
    }

    /** 2. 顺序性演示: 订单状态流转消息按序发送 */
    @GetMapping("/order")
    public Result<String> order(@RequestParam(required = false) String orderId) {
        mqDemoService.demonstrateOrder(orderId);
        return Result.success("顺序性演示消息已按序发送(创建->支付->发货->完成), 请查看日志确认消费顺序", orderId);
    }

    /** 3. 可靠性演示: 同步可靠消息 + 消费重试 */
    @GetMapping("/reliable")
    public Result<String> reliable(@RequestParam(required = false) String bizId) {
        mqDemoService.demonstrateReliable(bizId);
        return Result.success("可靠性演示消息已发送, 消费端前两次失败重试, 第三次成功, 请查看日志", bizId);
    }

    /** 4. 延迟队列演示: 发送延迟消息 */
    @GetMapping("/delay")
    public Result<String> delay(@RequestParam(required = false) String businessId,
                                @RequestParam(required = false) Integer delayLevel) {
        mqDemoService.demonstrateDelay(businessId, delayLevel);
        return Result.success("延迟消息已发送, 请查看日志观察实际延迟时间", businessId);
    }

    /** 5. 死信队列演示: 发送持续失败的消息触发 DLQ */
    @GetMapping("/dlx")
    public Result<String> dlx(@RequestParam(required = false) String bizId) {
        mqDemoService.demonstrateDlx(bizId);
        return Result.success("死信演示消息已发送, 重试耗尽后进入死信队列, 请查看日志", bizId);
    }
}
