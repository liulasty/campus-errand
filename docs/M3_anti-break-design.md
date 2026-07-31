# M3 防断流 · 详细设计

> **文档状态**：设计稿（供实现计划拆解与编码参考）
> **版本**：1.0
> **更新日期**：2026-08-01
> **前置**：[ROADMAP_1.0.md](ROADMAP_1.0.md) M3 章节、[委托全流程说明.md](委托全流程说明.md)
> **决策基线**：逐节点自动推进（方案 A，2026-08-01 确认）；节点超时 `node-hours=6h`、完成确认超时 `complete-hours=24h`

---

## 1. 目标与范围

让任务在履约阶段永不「卡死」：任一端超时未操作，系统自动推进任务，最终进入评价归档闭环。

- **规则 1 · 节点超时推进**：ACCEPTED 任务的当前节点超时未打卡 → 系统自动推进至下一节点，写入 `TaskUpdates(type=AUTO_ADVANCE)` 留痕（与手动打卡区分）。
- **规则 2 · 完成确认超时自动完成**：三个节点均已达到（真实或自动）但发布者超时未确认完成 → 系统自动置 `COMPLETED`，交给既有 `TaskAutoReview` 自动评价归档。

**不做**（留给后续）：发送消息通知（RabbitMQ 未装，消息功能本就不可用）；基于自动推进的信用惩罚（`AUTO_ADVANCE` 留痕已为其预留）；逐节点真实性校验。

---

## 2. 现状与关键事实

- **节点机制**：`/taskUpdate/node` → `TaskUpdatesServiceImpl.addNodeUpdate`，仅追加一条 `TaskUpdates`（`UpdateType` 为 `CONTACTED/PICKED_UP/DELIVERED`），**无顺序强制、无「当前节点」持久状态**。
- **状态流**：任务进入 `ACCEPTED` 后执行履约 → 发布者 `completed` 确认 → `COMPLETED` → `TaskAutoReview` 归档。执行期间任务状态恒为 `ACCEPTED`。
- **既有定时任务**：
  - `TaskOverDue`（`0/30 * * * * ?`）：ACCEPTED 且 `EndTime <= now` → `UNFINISHED`（截止时间兜底）。
  - `TaskAutoReview`（`0 13 * * * ?`）：COMPLETED 无评价 → 自动中性好评。
- **接单确认锚点**：`confirmTheRecipient` 在任务置 ACCEPTED 时写入一条 `TaskUpdates(type=RESULT)`；`taskacceptrecords.adoptTime` 为确认接收时间。

---

## 3. 数据层设计

### 3.1 `TaskUpdates` 加 `NodeIndex` 列

```sql
ALTER TABLE taskupdates
  ADD COLUMN NodeIndex TINYINT NOT NULL DEFAULT 0
  COMMENT '履约节点进度（0=无,1=已联系,2=已取件,3=已送达）';
```

- `addNodeUpdate`（手动打卡）写对应级别：`CONTACTED→1`、`PICKED_UP→2`、`DELIVERED→3`，取 `MAX(该任务既有 MAX(NodeIndex), 本次级别)`，容忍乱序打卡。
- 系统自动推进行同样写 `NodeIndex`（= 既有 `MAX(NodeIndex)+1`）。
- 同步更新 master SQL `校园委托0.99.sql` 的 `taskupdates` 建表语句。

### 3.2 `TaskUpdateType` 枚举加 `AUTO_ADVANCE`

```java
/** 系统自动推进（超时未操作） */
AUTO_ADVANCE("AUTO_ADVANCE", 9, "自动推进");
```

> 排序值 9 为当前最大，预留后续扩展枚举类型不冲突。

### 3.3 `task` 表加 `version` 乐观锁列（防多实例重复推进）

```sql
ALTER TABLE task
  ADD COLUMN version INT NOT NULL DEFAULT 0
  COMMENT '乐观锁版本号（防多实例/并发重复推进）';
```

- `Task` 实体加普通 `version` 字段（**非 `@Version` 注解**，避免触发 MyBatis-Plus 全局乐观锁插件影响既有 `updateById` 行为）。
- `TaskAutoAdvance` 处理每条任务前先**原子 claim**：
  ```sql
  UPDATE task SET version = version + 1 WHERE TaskID = ? AND version = #{loadedVersion}
  ```
  `rows == 0` → 已被其他实例/线程抢占，本条跳过。
- 同步更新 master SQL `校园委托0.99.sql`。

> 说明：当前单实例部署下此为防御性措施；后续集群扩容需替换为 ShedLock 分布式锁（见 §6 升级位）。

---

## 4. `TaskAutoAdvance` 定时任务

新增 `com.lz.task.TaskAutoAdvance`（`@Component @Slf4j`，模板同 `TaskAutoReview`）。

### 4.1 扫描条件

`STATUS = ACCEPTED` 且（`EndTime IS NULL OR EndTime > now`）—— **已过截止时间的任务不处理**，交给 `TaskOverDue` 标记 UNFINISHED，避免两个任务对同一任务同时改状态。

- **分页分批扫描**：每批 200 条，处理完一批再查下一批，控制单次内存占用。
- **每条任务处理前先执行乐观锁 claim**（见 §3.3）：`UPDATE task SET version=version+1 WHERE TaskID=? AND version=loadedVersion`，`rows==0` 则跳过（并发/多实例下另一执行者已抢占）。

### 4.2 对每个任务的判定

- `nodeReached` = 该任务节点事件（`UpdateType` ∈ {CONTACTED, PICKED_UP, DELIVERED, AUTO_ADVANCE}）的 `MAX(NodeIndex)`，无任何节点事件时为 0。
- **锚点**：有节点事件 → 取最新一条节点事件的 `UpdateTime`；无节点事件 → 取 `taskacceptrecords.adoptTime`（接单确认时间）。
- **超时判定**：统一 `Duration.between(锚点, now).toHours() > node-hours`（不足整小时不触发，贴合配置定义）。

**规则 1 · 节点超时推进**：`nodeReached < 3` 且超时：
- 插入 `TaskUpdates(type=AUTO_ADVANCE, NodeIndex=nodeReached+1, description=节点【{nodeReached+1}】超时未打卡，系统自动推进至下一履约节点, updateTime=now)`。
- 该行成为新的锚点 → 下一节点再等 `node-hours`，**天然防重复推进**。

**规则 2 · 完成确认超时自动完成**：`nodeReached >= 3` 且超时（`complete-hours`）：
- 任务置 `COMPLETED`（`taskMapper.updateById`），不插入评价（交给 `TaskAutoReview` 自动归档）。
- **乱序打卡说明**：用户直接手动打卡 `DELIVERED`（`NodeIndex=3`）跳过前两节点，属用户主动操作，系统认可并直接进入完成确认阶段，不拦截。

### 4.3 配置（`application.yml`）

```yaml
app:
  auto-advance:
    enabled: true
    # 节点超时未打卡的小时数
    node-hours: 6
    # 三节点完成后发布者未确认完成的小时数
    complete-hours: 24
    cron: "0 */5 * * * ?"
```

---

## 5. 三个定时任务分工（互不冲突）

| 任务 | 作用对象 | 动作 | 触发 |
|---|---|---|---|
| `TaskOverDue` | ACCEPTED 且 `EndTime <= now` | → `UNFINISHED` | 每 30 秒 |
| `TaskAutoAdvance`（新） | ACCEPTED 且 `EndTime IS NULL OR EndTime > now` | 节点超时推进 / 完成超时 → `COMPLETED` | 可配置（默认 5 分钟） |
| `TaskAutoReview` | COMPLETED 无评价 | 自动中性好评归档 | 每日 13:00 |

三者按「是否过截止时间 × 任务状态」分区，同一任务同一时刻只会被其中一个处理。

- **执行顺序**：三者 cron 周期独立，无强制执行先后，靠过滤条件互斥天然隔离，无需加锁区分顺序。
- **脏数据兜底**：极端情况下同一任务同时匹配多个定时任务时，靠任务表乐观锁 claim 抢占处理权，先 claim 成功者处理，后到者跳过。
- **事务隔离**：每条任务处理独立事务，单条失败不影响整批其余任务。

---

## 6. 已知限制

- **不校验节点真实性**：超时即自动推进，未验证接收者是否真的完成；后续信用惩罚机制可依据 `AUTO_ADVANCE` 留痕追责（P2）。
- **不发送通知**：RabbitMQ 未装，消息通知功能不可用，自动推进/自动完成暂不主动告知用户。
- **手动乱序打卡**：`NodeIndex` 取 MAX 容忍乱序，但若用户先打卡「已送达」直接到 3，则直接进入完成确认阶段（属用户自身行为）。
- **多实例集群重复执行风险**：MVP 以数据库乐观锁（`task.version` claim）兜底；后续扩容集群需引入 ShedLock 分布式锁（升级位）。
- **单线程执行耗时**：定时任务默认单线程，大批量待处理任务会拉长执行耗时；可后续引入线程池分批处理（升级位）。

---

## 7. 测试

**测试规范**：抽取纯判定方法 `judgeAutoAdvanceAction(task, 节点记录列表)`，返回「推进至 NodeIndex=n / 置 COMPLETED / 不处理」动作，无 DB 依赖，纯逻辑单测；乐观锁 claim 冲突用集成测试验证。

| # | 场景 | 断言 |
| :-- | :--- | :--- |
| 1 | 无节点事件 + 接单确认超时 | 推进至 NodeIndex=1，写 AUTO_ADVANCE |
| 2 | 已有节点事件但未超时 | 不推进 |
| 3 | 节点推进至 NodeIndex=3 后完成确认超时 | 任务置 COMPLETED |
| 4 | EndTime 已过期的 ACCEPTED 任务 | 跳过（不处理） |
| 5 | 防重复：推进后锚点更新，短时间内不再推进 | 仅一条 AUTO_ADVANCE |
| 6 | 乱序手动打卡：直接 DELIVERED(NodeIndex=3)，超时后 | 直接触发自动 COMPLETED |
| 7 | 并发冲突乐观锁（集成）：多执行者同时处理同一条 | 仅一条 AUTO_ADVANCE 记录，其余因 version 冲突跳过 |

---

## 8. 验收要点

- [ ] `taskupdates` 表含 `NodeIndex` 列，手动打卡写入正确级别。
- [ ] `TaskUpdateType.AUTO_ADVANCE` 存在并随留痕写入。
- [ ] ACCEPTED 任务节点超时自动推进（写 AUTO_ADVANCE，NodeIndex 递增）。
- [ ] 三节点达到后完成确认超时自动置 COMPLETED，进入自动评价。
- [ ] EndTime 过期任务不被 `TaskAutoAdvance` 处理（由 `TaskOverDue` 负责）。
- [ ] 防重复推进（锚点更新机制生效）。
- [ ] `application.yml` 新增 `app.auto-advance.*` 配置，可开关。
- [ ] 无改动既有 `TaskOverDue` / `TaskAutoReview` 行为。
- [ ] 多线程/多实例并发处理同一条任务不产生多条 AUTO_ADVANCE（乐观锁 claim 生效）。
- [ ] 批量分页扫描，单次加载数据量可控（每批 200）。
- [ ] 乱序手动打卡（直接 DELIVERED）可正常走完自动完成流程。
- [ ] 定时任务独立事务，单条任务处理失败不影响其他任务。
