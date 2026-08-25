package com.example.enterprise.mq.idempotent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 幂等存储组件 (基于 Redis)
 * <p>
 * 企业级实现: 使用 Redis SETNX (setIfAbsent) + 过期时间实现分布式幂等。
 * <p>
 * 核心命令:
 *   SET msgId 1 NX EX ttl  -> 返回 true 表示首次处理
 *                          -> 返回 false 表示已处理过(重复消息)
 * <p>
 * 利用 Redis 单线程原子性保证并发下的幂等判断安全, 且天然支持分布式。
 */
@Slf4j
@Component
public class IdempotentStorage {

    /** 幂等键前缀 */
    private static final String KEY_PREFIX = "idempotent:msg:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取幂等锁。
     *
     * @param msgId 消息唯一 ID
     * @param ttl   幂等保留时间(秒)
     * @return true: 首次处理, 可以继续; false: 已处理过(重复消息)
     */
    public boolean tryLock(String msgId, long ttl) {
        String key = KEY_PREFIX + msgId;
        // SET key value NX EX ttl: key 不存在时设置成功返回 true, 存在则返回 false
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, String.valueOf(System.currentTimeMillis()), ttl, TimeUnit.SECONDS);
        boolean first = Boolean.TRUE.equals(success);
        log.debug("[幂等] msgId={}, 首次={}", msgId, first);
        return first;
    }

    /**
     * 删除幂等记录(当业务处理失败时释放, 允许重试)
     */
    public void remove(String msgId) {
        stringRedisTemplate.delete(KEY_PREFIX + msgId);
    }
}
