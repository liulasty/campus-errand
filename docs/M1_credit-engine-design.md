# M1 信用引擎 · 详细设计

> **文档状态**：设计稿（供实现计划拆解与编码参考）
> **版本**：1.0
> **更新日期**：2026-08-01
> **前置**：[ROADMAP_1.0.md](ROADMAP_1.0.md) M1 章节
> **决策基线**：信用分初始默认 60；权重 均分 60% / 完成率 40%；纯新用户特判返回 60（2026-08-01 确认）

---

## 1. 目标与范围

M1 为「信用分」建立可计算的、可扩展的地基：

- 输出一个 **0–100 的信用分**（`creditScore`），无数据用户为默认 60。
- 数据来源复用现有表与现有统计口径，**零新表**。
- 计算逻辑放在**可插拔策略**（`CreditCalculator`）中，v2 换多维算法不触碰调用方。
- 复用现有 `GET /credit` 接口返回信用分，前端信用档案页展示。

**不做**（留给后续里程碑）：信用加权排序（M2）、超时自动推进（M3）、评价护栏（M4）、多维信用算法与惩罚（P2）。

---

## 2. 数据来源与统计口径

沿用现有 `CreditController` 的口径，全部按 `userId`（接单方视角）统计：

| 原始指标 | 来源 | 口径 |
| :--- | :--- | :--- |
| `acceptedCount` | `taskacceptrecords` | `AccepterId = userId AND status = 'Checked'` 计数（已被发布者确认接收） |
| `completedCount` | `task` | `ReceiverID = userId AND STATUS = 'COMPLETED'` 计数 |
| `ratingAvg` | `reviews`（已实现 `avgRatingByAcceptor`） | 作为接收方（`AcceptorID = userId`）收到的平均评分，可为 `null` |

> 注：`taskacceptrecords.status` 用 `AcceptStatus.CHECKED.getDbValue()`（`"Checked"`）；`task.STATUS` 用 `TaskStatus.COMPLETED.getDbValue()`（`"COMPLETED"`）。与现有 `CreditController` 一致。

---

## 3. 算法规则（v1）

```
若 acceptedCount == 0 且 ratingAvg == null  →  creditScore = 60          # 纯新用户特判
否则：
  rating     = ratingAvg == null ? 60 : round(ratingAvg / 5.0 * 100)      # 无评分按 3 星 = 60
  completion = acceptedCount == 0 ? 100 : round(completedCount / acceptedCount * 100)
  creditScore = round(0.6 * rating + 0.4 * completion)                    # 0–100
```

边界说明：

- **纯新用户**（无接单且无评分）：特判返回 60，避免「无评分 60 + 无接单 100%」套公式算出 76 与默认值矛盾。
- **只有评分、从未被确认接单**：`rating` 实算，`completion = 100`（无接单视为完成率中性）。
- **只有接单、从未被评价**：`rating = 60`（中性），`completion` 实算。
- **接了未完成**：`completion` 随 `completed/accepted` 降低，扣分自然体现。
- 权重 0.6 / 0.4 与默认 60 在 `SimpleCreditCalculator` 内以常量固化（v1 不做配置化，v2 由策略接管）。

---

## 4. 组件设计

新增独立包 `com.lz.credit`（领域逻辑），与 service/mapper 分层：

### 4.1 `CreditMetrics`（POJO，纯输入）

```java
@Data
@Builder
public class CreditMetrics {
    private Long acceptedCount;     // 已确认接单数
    private Long completedCount;    // 已完成接单数
    private Double ratingAvg;       // 接收方均分，可为 null
}
```

### 4.2 `CreditCalculator`（策略接口）

```java
public interface CreditCalculator {
    /** 输入原始指标，返回 0–100 信用分 */
    int calculate(CreditMetrics metrics);
}
```

### 4.3 `SimpleCreditCalculator`（v1 实现，`@Component`）

- 常量 `RATING_WEIGHT = 0.6`、`COMPLETION_WEIGHT = 0.4`、`DEFAULT_SCORE = 60`、`NEUTRAL_RATING = 60`（3 星）、`NEUTRAL_COMPLETION = 100`。
- 严格按 §3 规则实现，纯函数，可单测。

### 4.4 `CreditScoreService`（`@Service`，位于 `com.lz.service`）

```java
@Service
public class CreditScoreService {
    // 注入 ReviewsMapper / TaskMapper / TaskAcceptRecordsMapper
    public int getScore(Long userId) { ... }   // 聚合 → CreditMetrics → calculator
}
```

- 聚合逻辑从现有 `CreditController` 内联代码**收拢到 Service**（`CreditController` 瘦身为组装 VO）。
- `getScore(Long userId)` 供 M2 大厅排序按任务 OwnerID 复用。

---

## 5. 接口变更

- `CreditProfileVO` 增字段：`private Integer creditScore;`
- 现有 `GET /credit`（`CreditController.credit()`）在返回的 VO 中带上 `creditScore`，**不新增端点、不改路径**。
- 前端 `getCreditProfile` 返回结构新增 `creditScore` 字段，向后兼容。

---

## 6. 前端变更

`web/src/views/user/CreditProfile.vue` 增补信用分展示：

- 醒目主数字：`profile.creditScore`
- 简单等级文案：`<60 待提升` / `60–79 良好` / `≥80 优秀`
- 建议放在顶部统计区上方或作为首个 stat-card 放大展示（具体样式实现时定，不引入新依赖）。

---

## 7. 测试

对 `SimpleCreditCalculator` 写单元测试（纯函数，无需 Spring 容器）：

| # | 场景 | 输入 | 期望 |
| :-- | :--- | :--- | :--- |
| 1 | 纯新用户 | accepted=0, completed=0, ratingAvg=null | 60 |
| 2 | 只有评分 | accepted=0, ratingAvg=5.0 | `0.6×100 + 0.4×100 = 100` |
| 3 | 只有评分（中性完成率） | accepted=0, ratingAvg=4.0 | `0.6×80 + 0.4×100 = 88` |
| 4 | 接了未完成 | accepted=2, completed=1, ratingAvg=null | `0.6×60 + 0.4×50 = 56` |
| 5 | 满分履约 | accepted=2, completed=2, ratingAvg=5.0 | `0.6×100 + 0.4×100 = 100` |

> 示例值仅供验收参考；四舍五入取整以实现为准。

---

## 8. 升级位（v2 预留）

- 新增维度（准时率、合规记录、主动方画像）→ `CreditMetrics` 加字段。
- 换算法 → 新增 `CreditCalculator` 实现（如 `MultiDimCreditCalculator`），替换注入即可。
- 大规模计算/大厅排序性能问题 → 引入预计算缓存（对齐架构里的 Redis 缓存位），属 M2 评估项。

---

## 9. 验收要点

- [ ] `SimpleCreditCalculator` 单元测试全绿（§7 五例）。
- [ ] 纯新用户 `GET /credit` 返回 `creditScore = 60`。
- [ ] 有历史数据用户返回 0–100 内分数，且随完成率/评分单调变化。
- [ ] `CreditProfile.vue` 展示信用分与等级文案。
- [ ] 现有信用档案统计（接单数/均分/好评率）不回归。
- [ ] 不新增数据库表；`application.yml` 无需新配置。
