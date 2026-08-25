<template>
  <div class="mq-page">
    <el-alert type="success" :closable="false" show-icon
      title="RocketMQ 五大特性企业级演示 — 发送消息后请查看后端控制台日志观察消费效果" />

    <!-- 1. 幂等性 -->
    <el-card class="card">
      <div slot="header" class="card-header">
        <span class="tag">特性一</span> 幂等性保障
        <el-tag size="mini" type="info">消费者通过 msgId 去重</el-tag>
      </div>
      <p class="desc">模拟订单消息被重复发送, 消费者只处理一次。请观察后端日志: 重复消息被忽略。</p>
      <el-form inline>
        <el-form-item label="订单号">
          <el-input v-model="idempotentOrderId" placeholder="留空自动生成" clearable style="width:220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading.idempotent" @click="runIdempotent">发送幂等测试消息</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 2. 顺序性 -->
    <el-card class="card">
      <div slot="header" class="card-header">
        <span class="tag">特性二</span> 消息顺序性
        <el-tag size="mini" type="info">顺序消息 + 单队列串行消费</el-tag>
      </div>
      <p class="desc">同一订单按 创建→支付→发货→完成 顺序发送, 消费端将按序执行。即使单条耗时不同, 顺序依然保持。</p>
      <el-form inline>
        <el-form-item label="订单号">
          <el-input v-model="orderId" placeholder="留空自动生成" clearable style="width:220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading.order" @click="runOrder">发送顺序消息</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 3. 可靠性 -->
    <el-card class="card">
      <div slot="header" class="card-header">
        <span class="tag">特性三</span> 可靠性保障
        <el-tag size="mini" type="info">同步发送 + 失败重试</el-tag>
      </div>
      <p class="desc">消息可靠投递, 消费者前两次处理失败自动重试, 第三次成功。观察日志中的重试过程。</p>
      <el-form inline>
        <el-form-item label="业务ID">
          <el-input v-model="reliableBizId" placeholder="留空自动生成" clearable style="width:220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading.reliable" @click="runReliable">发送可靠消息</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 4. 延迟队列 -->
    <el-card class="card">
      <div slot="header" class="card-header">
        <span class="tag">特性四</span> 延迟队列
        <el-tag size="mini" type="info">订单超时关闭等场景</el-tag>
      </div>
      <p class="desc">消息延迟指定时间后才被消费。选择延迟级别后发送, 观察日志中的实际延迟时间。</p>
      <el-form inline>
        <el-form-item label="业务ID">
          <el-input v-model="delayBusinessId" placeholder="留空自动生成" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="延迟级别">
          <el-select v-model="delayLevel" style="width:200px">
            <el-option v-for="(label, idx) in delayLabels" :key="idx" :label="label" :value="idx + 1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading.delay" @click="runDelay">发送延迟消息</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 5. 死信队列 -->
    <el-card class="card">
      <div slot="header" class="card-header">
        <span class="tag">特性五</span> 死信队列
        <el-tag size="mini" type="danger">重试耗尽进入 DLQ</el-tag>
      </div>
      <p class="desc">发送一条持续失败的消息, 重试耗尽后自动进入死信队列, 由 DLQ 消费者捕获记录(需人工介入)。</p>
      <el-form inline>
        <el-form-item label="业务ID">
          <el-input v-model="dlxBizId" placeholder="留空自动生成" clearable style="width:220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" :loading="loading.dlx" @click="runDlx">发送死信演示消息</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志说明 -->
    <el-card class="card tip-card">
      <div slot="header">📌 查看演示效果</div>
      <p>所有消费效果均输出在后端控制台日志中。请保持后端(SpringBoot)运行并关注日志, 关键字:</p>
      <ul>
        <li>幂等: <code>[幂等] 检测到重复消息, 已忽略</code></li>
        <li>顺序: <code>[顺序消费] orderId=..., 阶段=...</code></li>
        <li>可靠: <code>[可靠] 第 N 次消费失败/成功</code></li>
        <li>延迟: <code>[延迟消费] 实际延迟=Xms</code></li>
        <li>死信: <code>[死信消费] 收到死信消息</code></li>
      </ul>
    </el-card>
  </div>
</template>

<script>
import { mqApi } from '../api/mq'

export default {
  name: 'MqDemo',
  data () {
    return {
      idempotentOrderId: '',
      orderId: '',
      reliableBizId: '',
      delayBusinessId: '',
      delayLevel: 3,
      dlxBizId: '',
      delayLabels: ['1s', '5s', '10s', '30s', '1m', '2m', '3m', '4m', '5m', '6m', '7m', '8m', '9m', '10m', '20m', '30m', '1h', '2h'],
      loading: { idempotent: false, order: false, reliable: false, delay: false, dlx: false }
    }
  },
  methods: {
    async runIdempotent () {
      this.loading.idempotent = true
      try {
        const res = await mqApi.idempotent(this.idempotentOrderId)
        this.$message.success(res.message)
      } catch (e) {
        this.$message.error(e.message)
      } finally {
        this.loading.idempotent = false
      }
    },
    async runOrder () {
      this.loading.order = true
      try {
        const res = await mqApi.order(this.orderId)
        this.$message.success(res.message)
      } catch (e) {
        this.$message.error(e.message)
      } finally {
        this.loading.order = false
      }
    },
    async runReliable () {
      this.loading.reliable = true
      try {
        const res = await mqApi.reliable(this.reliableBizId)
        this.$message.success(res.message)
      } catch (e) {
        this.$message.error(e.message)
      } finally {
        this.loading.reliable = false
      }
    },
    async runDelay () {
      this.loading.delay = true
      try {
        const res = await mqApi.delay(this.delayBusinessId, this.delayLevel)
        this.$message.success(res.message)
      } catch (e) {
        this.$message.error(e.message)
      } finally {
        this.loading.delay = false
      }
    },
    async runDlx () {
      this.loading.dlx = true
      try {
        const res = await mqApi.dlx(this.dlxBizId)
        this.$message.success(res.message)
      } catch (e) {
        this.$message.error(e.message)
      } finally {
        this.loading.dlx = false
      }
    }
  }
}
</script>

<style scoped>
.card { margin-bottom: 20px; }
.card-header { font-weight: bold; font-size: 16px; }
.card-header .tag {
  display: inline-block;
  background: #409EFF;
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 8px;
}
.desc { color: #666; margin-bottom: 12px; font-size: 13px; }
.tip-card ul { padding-left: 20px; line-height: 1.8; }
.tip-card code { background: #f4f4f5; padding: 2px 6px; border-radius: 3px; color: #c7254e; }
</style>
