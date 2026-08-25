import request from './request'

/**
 * RocketMQ 五大特性演示接口
 */
export const mqApi = {
  // 1. 幂等性
  idempotent (orderId) {
    return request.get('/mq/idempotent', { params: { orderId } })
  },
  // 2. 顺序性
  order (orderId) {
    return request.get('/mq/order', { params: { orderId } })
  },
  // 3. 可靠性
  reliable (bizId) {
    return request.get('/mq/reliable', { params: { bizId } })
  },
  // 4. 延迟队列
  delay (businessId, delayLevel) {
    return request.get('/mq/delay', { params: { businessId, delayLevel } })
  },
  // 5. 死信队列
  dlx (bizId) {
    return request.get('/mq/dlx', { params: { bizId } })
  }
}
