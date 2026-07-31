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

**编码约束**：
- 状态值一律走枚举：`AcceptStatus.CHECKED.getDbValue()`（`"Checked"`）、`TaskStatus.COMPLETED.getDbValue()`（`"COMPLETED"`），**禁止在代码中硬编码字符串字面量**。
- **术语「纯新用户」**：指 `acceptedCount == 0`（无已确认接单记录）**且** `ratingAvg == null`（无作为接收方的评价记录），全文统一按此定义。

> 注：以上口径与现有 `CreditController` 完全一致。

---

## 3. 算法规则（v1）

```
若 acceptedCount == 0 且 ratingAvg == null  →  creditScore = 60          # 纯新用户特判
否则：
  rating     = ratingAvg == null ? 60 : round(ratingAvg / 5.0 * 100)      # 无评分按 3 星 = 60
  completion = acceptedCount == 0 ? 100 : round((double) completedCount / acceptedCount * 100)  # 强转浮点，避免整数除法
  creditScore = round(0.6 * rating + 0.4 * completion)                    # 0–100
```

边界说明：

- **纯新用户**（无已确认接单且无评价记录）：特判返回 60，避免「无评分 60 + 无接单 100%」套公式算出 76 与默认值矛盾。
- **只有评分、从未被确认接单**：`rating` 实算，`completion = 100`（无接单视为完成率中性）。
- **只有接单、从未被评价**：`rating = 60`（中性），`completion` 实算。
- **接了未完成**：`completion` 随 `completed/accepted` 降低，扣分自然体现。
- **舍入规则**：全文 `round()` 统一为 Java `Math.round`（四舍五入，half-up）。
- **等级边界**：`60–79` **含 60** 归「良好」；`<60`「待提升」；`≥80`「优秀」。前端判断以此为唯一标准。
- 权重 0.6 / 0.4 与默认 60 抽到 `CreditConstant` 常量类（见 §4.1），v1 不做配置化，v2 由策略接管。

---

## 4. 组件设计

新增独立包 `com.lz.credit`（领域逻辑），与 service/mapper 分层。内部子包规划：

```text
com.lz.credit
  ├── constant    CreditConstant 信用常量
  ├── dto         CreditMetrics 指标入参
  └── strategy    CreditCalculator 接口 + SimpleCreditCalculator 实现
com.lz.service    CreditScoreService（注入 mapper，聚合 + 调策略）
```

### 4.1 `CreditConstant`（常量，`com.lz.credit.constant`）

```java
public final class CreditConstant {
    public static final double RATING_WEIGHT = 0.6;          // 均分权重
    public static final double COMPLETION_WEIGHT = 0.4;      // 完成率权重
    public static final int DEFAULT_USER_SCORE = 60;         // 纯新用户默认分
    public static final int NEUTRAL_RATING_SCORE = 60;       // 无评分按 3 星
    public static final int NEUTRAL_COMPLETION_RATE = 100;   // 无接单完成率中性值
    public static final int CREDIT_GOOD_MIN = 60;            // 「良好」下限（含）
    public static final int CREDIT_EXCELLENT_MIN = 80;       // 「优秀」下限
}
```

### 4.2 `CreditMetrics`（DTO，`com.lz.credit.dto`，纯输入）

```java
@Data
@Builder
public class CreditMetrics {
    private Long acceptedCount;     // 已确认接单数（聚合时兜底 0）
    private Long completedCount;    // 已完成接单数（聚合时兜底 0）
    private Double ratingAvg;       // 接收方均分，可为 null
}
```

> `CreditScoreService` 聚合时对计数做空值兜底（`Optional.ofNullable(...).orElse(0L)`），`ratingAvg` 为 null 由算法规则处理，防止 NPE。

### 4.3 `CreditCalculator`（策略接口，`com.lz.credit.strategy`）

```java
public interface CreditCalculator {
    /** 输入原始指标，返回 0–100 信用分 */
    int calculate(CreditMetrics metrics);
}
```

### 4.4 `SimpleCreditCalculator`（v1 实现，`@Component`，`com.lz.credit.strategy`）

- 常量引用 `CreditConstant`，严格按 §3 规则实现（含浮点强转、`Math.round` 半入）。
- 纯函数、无状态，可单测。

### 4.5 `CreditScoreService`（`@Service`，`com.lz.service`）

```java
@Service
public class CreditScoreService {
    // 注入 ReviewsMapper / TaskMapper / TaskAcceptRecordsMapper
    public int getScore(Long userId) { ... }   // 聚合（3 次 SQL）→ CreditMetrics → calculator
}
```

- 聚合逻辑从现有 `CreditController` 内联代码**收拢到 Service**（`CreditController` 瘦身为组装 VO）。
- `getScore(Long userId)` 供 M2 大厅排序按任务 OwnerID 复用。
- 计数空值兜底见 §4.2 注释；`ratingAvg` 为 null 交算法处理。
- **性能基线**：单用户信用分 = 3 次 SQL（acceptedCount / completedCount / ratingAvg），M1 量级可控；M2 大厅按 OwnerID 聚合存在 N+1，届时评估联合聚合 SQL 或 Redis 预计算（见 §8）。

---

## 5. 接口变更

- `CreditProfileVO` 增字段：`private Integer creditScore;`（**可空**；老前端未适配时忽略该字段不报错）
- 现有 `GET /credit`（`CreditController.credit()`）在返回的 VO 中带上 `creditScore`，**不新增端点、不改路径**。
- 前端 `getCreditProfile` 返回结构新增 `creditScore` 字段，向后兼容。

---

## 6. 前端变更

`web/src/views/user/CreditProfile.vue` 增补信用分展示：

- 醒目主数字：`profile.creditScore`
- 等级文案（边界唯一标准）：`<60 待提升` / `60–79`（**含 60**）`良好` / `≥80 优秀`
- `creditScore` 为 `null` 时前端兜底展示 `60`
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
| 6 | 四舍五入临界 | accepted=2, completed=1, ratingAvg=4.8 | `rating=96, completion=50 → 77.6 → round=78` |

> 示例值仅供验收参考；舍入统一 `Math.round`（half-up），以实现为准。

---

## 8. 升级位（v2 预留）

- 新增维度（准时率、合规记录、主动方画像）→ `CreditMetrics` 加字段。
- 换算法 → 新增 `CreditCalculator` 实现（如 `MultiDimCreditCalculator`），替换注入即可。
- 大规模计算/大厅排序性能问题 → **M2 评估项**：M1 单用户信用分为 3 次 SQL（已文档化于 §4.5），量级可控；M2 大厅按 OwnerID 逐任务聚合存在 N+1，届时在「联合聚合自定义 SQL（单查询出三项指标）」与「Redis 预计算缓存（对齐架构缓存位）」中选型（已确认 2026-08-01：M1 不引入联合 SQL）。

---

## 9. 验收要点

- [ ] `SimpleCreditCalculator` 单元测试全绿（§7 六例）。
- [ ] 单用户信用分查询 SQL 次数可观测（M1 为 3 次，已文档化于 §8；M2 再做聚合优化）。
- [ ] 状态值统一走枚举 `AcceptStatus.CHECKED` / `TaskStatus.COMPLETED`，无硬编码字符串字面量。
- [ ] 纯新用户 `GET /credit` 返回 `creditScore = 60`。
- [ ] 有历史数据用户返回 0–100 内分数，且随完成率/评分单调变化。
- [ ] `CreditProfile.vue` 展示信用分与等级文案。
- [ ] 现有信用档案统计（接单数/均分/好评率）不回归。
- [ ] 不新增数据库表；`application.yml` 无需新配置。
