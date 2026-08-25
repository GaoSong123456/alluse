# 企业级 SpringBoot + Vue2 前后端分离 Demo

一个面向企业级实践的示例项目，实现了两大核心能力：

1. **RocketMQ 消息五大特性**：幂等性保障、消息顺序性、可靠性、延迟队列、死信队列
2. **大文件 Excel 分片上传 + 断点续传**

---

## 一、技术栈

| 端 | 技术 | 版本 |
|----|------|------|
| 后端 | Spring Boot | 2.7.18 |
| 后端 | Java | 1.8 |
| 后端 | RocketMQ Spring Boot Starter | 2.2.3 |
| 后端 | EasyExcel | 3.3.4 |
| 后端 | Redis (幂等存储) | - |
| 后端 | MySQL 8.0 + MyBatis-Plus (分片元数据) | 3.5.5 |
| 后端 | Lombok | 可选 |
| 前端 | Vue | 2.6.14 |
| 前端 | Vue Router | 3.6.5 |
| 前端 | Element UI | 2.15.14 |
| 前端 | Axios / Spark-MD5 | - |
| 前端 | Vue CLI | 5.0.8 |

---

## 二、项目结构

```
d:\demo
├── backend/                          # SpringBoot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/enterprise/
│       │   ├── EnterpriseDemoApplication.java   # 启动类
│       │   ├── common/               # Result 统一响应 / BusinessException / 全局异常
│       │   ├── config/               # EnterpriseProperties / WebConfig(CORS+静态资源)
│       │   ├── mq/                   # ★ RocketMQ 五大特性
│       │   │   ├── constant/         # Topic / 消费组常量
│       │   │   ├── entity/           # MessageBody(携带唯一 msgId)
│       │   │   ├── idempotent/       # IdempotentStorage(幂等存储, 可替换 Redis)
│       │   │   ├── producer/         # MqProducerService(同步/顺序/延迟/异步)
│       │   │   ├── consumer/         # 幂等/顺序/可靠/延迟/死信 5 个消费者
│       │   │   ├── service/          # MqDemoService(五大特性业务逻辑)
│       │   │   └── controller/       # MqDemoController(REST 接口)
│       │   └── upload/               # ★ 大文件分片上传
│       │       ├── entity/           # ChunkUploadDTO / UploadProgress
│       │       ├── service/          # ChunkStorageService(分片存储/合并/断点查询)
│       │       └── controller/       # ChunkUploadController(REST 接口)
│       └── resources/
│           └── application.yml       # 配置文件
│
└── frontend/                         # Vue2 前端
    └── src/
        ├── main.js                   # 入口
        ├── App.vue                   # 根组件 + 顶部导航
        ├── router/index.js           # 路由
        ├── api/                      # mq.js / upload.js 接口封装
        ├── utils/request.js          # axios 封装
        └── views/
            ├── MqDemo.vue            # ★ RocketMQ 五大特性演示页面
            └── ExcelUpload.vue       # ★ 大文件分片断点续传页面
```

---

## 三、功能一：RocketMQ 五大特性 Demo

### 3.1 幂等性保障（Idempotent）

**场景**：消息可能被重复投递（生产者重试、消费成功但 ACK 丢失等）。

**核心实现**：
- 每个消息体携带业务唯一 `msgId`
- 消费端通过 `IdempotentStorage.tryLock(msgId, ttl)` 判断是否首次处理
- 重复消息直接忽略，保证业务只执行一次

**演示**：发送订单消息并模拟用相同 `msgId` 再次发送，观察日志中重复消息被忽略。

```
[幂等] 首次处理消息 -> msgId=..., bizKey=...
[幂等] 检测到重复消息, 已忽略 -> msgId=..., bizKey=...
```

### 3.2 消息顺序性（Orderly）

**场景**：同一订单的 创建 → 支付 → 发货 → 完成 状态流转必须按顺序消费。

**核心实现**：
- 生产者使用 `syncSendOrderly(topic, msg, hashKey)`，以订单号为分区键保证进入同一队列
- 消费者使用 `ConsumeMode.CONSUME_MODE_ORDERLY`，同一队列串行消费

**演示**：对一个订单按序发送 4 条状态消息，观察日志中消费顺序始终为 创建→支付→发货→完成。

### 3.3 可靠性保障（Reliable）

**场景**：消息不能丢失，失败需要自动重试。

**核心实现**：
- 生产者：同步发送 + `retry-times-when-send-failed` 失败重试
- 消费者：处理异常时抛出异常，RocketMQ 按重试策略重新投递，达到最大次数进入死信队列

**演示**：消费者前两次处理失败触发重试，第三次成功。

```
[可靠] 第 1 次消费失败, 将触发重试
[可靠] 第 2 次消费失败, 将触发重试
[可靠] 第 3 次消费成功
```

### 3.4 延迟队列（Delay）

**场景**：订单超时未支付自动关闭、定时任务化处理。

**核心实现**：发送时指定 `delayLevel`（1s 5s 10s 30s 1m 2m ... 2h），Broker 延迟到指定时间后再投递。

**演示**：选择延迟级别发送消息，观察日志中的实际延迟时间。

### 3.5 死信队列（DLQ）

**场景**：消息重试耗尽仍失败，需人工介入或二次补偿。

**核心实现**：
- 消费组重试达到最大次数后，消息进入 `%DLQ%消费组名` 主题
- `DlxConsumer` 监听死信主题并记录，提示人工介入

**演示**：发送一条持续失败的消息（payload 含 `DLX_DEMO` 标记），重试耗尽后进入死信队列。

---

## 四、功能二：大文件 Excel 分片上传 + 断点续传

### 4.1 实现原理

```
前端计算文件 MD5(identifier)
        │
        ▼
调用 /api/upload/check 查询已上传分片(断点续传依据)
        │
        ▼
按 5MB 切分文件, 并发上传缺失分片到 /api/upload/upload
        │
        ▼
全部上传成功后调用 /api/upload/merge 后端按序合并
```

### 4.2 关键设计

| 环节 | 说明 |
|------|------|
| 文件标识 | 前端用 **Spark-MD5** 计算整个文件 MD5，作为唯一 `identifier` |
| 分片 | 前端按 **5MB** 将文件切成多个 Blob 分片 |
| 断点续传 | 上传前调用 `/api/upload/check`，返回已上传分片序号，只上传缺失部分 |
| 并发控制 | 前端 3 路并发上传，支持暂停 |
| 合并 | 全部上传后 `/api/upload/merge` 按分片序号升序合并为完整 Excel |
| 幂等合并 | 后端检测到文件已存在且大小正确则跳过合并 |

### 4.3 存储目录

```
upload/{identifier}/chunks/0.part ... N.part   # 各分片文件
upload/{identifier}/merged/xxx.xlsx            # 合并后的完整文件
```

---

## 五、REST API 一览

### RocketMQ（`/api/mq`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/mq/idempotent?orderId=` | 幂等性演示（含模拟重复） |
| GET | `/api/mq/order?orderId=` | 顺序性演示 |
| GET | `/api/mq/reliable?bizId=` | 可靠性演示 |
| GET | `/api/mq/delay?businessId=&delayLevel=` | 延迟队列演示 |
| GET | `/api/mq/dlx?bizId=` | 死信队列演示 |

### 大文件上传（`/api/upload`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/upload/upload` | 上传单个分片（multipart） |
| GET | `/api/upload/check` | 查询上传进度（断点续传） |
| POST | `/api/upload/merge` | 合并分片 |

---

## 六、快速启动

### 6.1 前置环境

- JDK 1.8+
- Maven 3.6+
- Node.js 14+ / npm
- RocketMQ 4.x（Nameserver 默认 `127.0.0.1:9876`）
- Redis（默认 `127.0.0.1:6379`，幂等存储）
- MySQL 8.0（默认 `127.0.0.1:3306`，分片元数据，账号 root/root，可按需修改 `application.yml`）

### 6.2 初始化数据库

```bash
# 执行建表脚本(自动创建数据库 enterprise_demo 及表 t_upload_file)
mysql -uroot -proot < backend/src/main/resources/sql/init.sql
```

### 6.3 启动 RocketMQ

```bash
# 启动 NameServer
mqnamesrv

# 启动 Broker
mqbroker -n 127.0.0.1:9876
```

> 需要创建 4 个 Topic，或开启 Broker 的 `autoCreateTopicEnable=true` 自动创建：
> - `demo_idempotent_topic`
> - `demo_order_topic`
> - `demo_reliable_topic`
> - `demo_delay_topic`

### 6.4 启动后端（端口 8080）

```bash
cd backend
mvn spring-boot:run
```

### 6.5 启动前端（端口 8081）

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:8081`。

---

## 七、配置说明（application.yml）

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: enterprise-producer-group
    retry-times-when-send-failed: 3   # 发送失败重试次数

enterprise:
  upload-dir: ./upload                # 上传文件存储根目录
  chunk-size: 5242880                 # 分片大小(5MB)
  delay-level: 3                      # 延迟消息默认级别(10s)
  simulate-duplicate: true            # 是否模拟消息重复(幂等演示)
```

---

## 八、注意事项

1. **幂等存储(Redis)**：使用 Redis `SETNX` + 过期时间实现分布式幂等，天然支持多实例部署；幂等保留时间可通过 `enterprise.idempotent-ttl` 配置。
2. **分片元数据(MySQL)**：使用 MySQL 8.0 + MyBatis-Plus 持久化上传任务元数据，跨实例共享断点续传状态。建表脚本见 `backend/src/main/resources/sql/init.sql`。
3. **数据库/Redis 连接**：密码通过环境变量 `DB_USERNAME` / `DB_PASSWORD` / `DB_URL` 注入，避免硬编码泄露；开发环境默认 root/root。
4. **SQL 日志**：默认不打印 SQL；通过 `--spring.profiles.active=dev` 开启开发日志，`prod` 环境关闭。
5. **死信主题**：本示例按 RocketMQ 4.x 标准（`%DLQ%消费组名`），5.x 结构略有差异。
6. **前端代理**：`vue.config.js` 中已将 `/api` 与 `/files` 代理到后端 `8080`。

---

## 九、演示建议

- **RocketMQ**：保持后端运行，在前端页面点击各特性按钮，切换后端控制台观察对应日志。
- **大文件上传**：选择较大的 Excel（建议 100MB+），上传过程中**暂停或刷新页面**，再重新选择同一文件，即可演示断点续传。
