# M2 智能曝光 · 详细设计

> **文档状态**：设计稿（供实现计划拆解与编码参考）
> **版本**：1.0
> **更新日期**：2026-08-01
> **前置**：[ROADMAP_1.0.md](ROADMAP_1.0.md) M2 章节、[M1_credit-engine-design.md](M1_credit-engine-design.md)
> **决策基线**：排序键 `(信用分 DESC, 创建时间 DESC)`；大厅任务卡片展示发布者信用分；实现采用内存排序（方案 A，2026-08-01 确认）

---

## 1. 目标与范围

M2 让「信用分」参与任务大厅的曝光排序，并在任务卡片上展示发布者信用分：

- **排序**：任务大厅列表按 `(发布者信用分 DESC, StartTime DESC)` 排序，信用高靠前、信用低沉底。
- **展示**：任务卡片展示发布者的 `ownerCredit`（0–100）与等级，呼应「不看头像看信用」。

**不做**（留给后续）：批量取分 / 联合聚合 SQL / Redis 预计算缓存（性能升级位）、多维信用算法（P2）、发布方独立信用画像（P2）。

---

## 2. 现状（改动点）

**链路**：`GET /user/task/page` → `TaskUserController.getTaskPage` → `TaskServiceImpl.searchPage(pageNum, pageSize, location, description, taskTypeId, queryRules, status)`。

**当前 `searchPage(int...)`**（`TaskServiceImpl.java:546`）：
- 过滤：状态（默认 `ACCEPTED/ONGOING/COMPLETED`，或指定）+ `TaskType`/`Location` 等值 + `Description` 模糊。
- 排序：`queryRules == 0` → `StartTime` 倒序，否则正序。
- 分页：MyBatis-Plus `taskMapper.selectPage(page, wrapper)` SQL 层分页。

**影响面确认**：6 参 `searchPage` 仅 `TaskUserController.getTaskPage` 调用；管理端 `TaskAdminController.searchPage`、`searchPage(TaskPageDTO)` 重载走各自方法，**不受影响**。

---

## 3. 方案：内存排序 + 复用 CreditScoreService（方案 A）

- 保留现有过滤与 `taskMapper.selectPage` 的全量匹配能力，但排序改在内存完成。
- 信用分来源复用 M1 `CreditScoreService.getScore(Long userId)`（3 次 SQL/owner，学校量级去重后 owner 数少，可接受）。
- 零数据库表结构变更；`Task` 加瞬态字段承载 `ownerCredit`（不落库）。

---

## 4. 后端设计

### 4.1 `Task` 实体增瞬态字段

```java
/** 发布者信用分（瞬态，不落库，大厅展示用） */
@TableField(exist = false)
private Integer ownerCredit;
```

### 4.2 `TaskServiceImpl.searchPage(int...)` 改造

```java
// 注入 CreditScoreService（沿用类内注入方式）
// 1. 保留现有过滤（状态/类型/地点/描述），去掉 wrapper.orderByDesc/orderByAsc("StartTime")
// 2. 查询全量匹配记录：taskMapper.selectList(wrapper)（不再用带分页的 selectPage，排序在内存做）
// 3. 收集去重 OwnerID，逐个 creditScoreService.getScore(ownerId)
// 4. 对每行任务设置 task.setOwnerCredit(score)
// 5. 按 (credit DESC, StartTime DESC, TaskID DESC) 内存排序
// 6. 内存分页切片：from = (pageNum-1)*pageSize, to = min(from+pageSize, size)
// 7. PageResult(total = 全量匹配数, records = 切片)
```

排序比较器要点：
- 主键 `credit DESC`：`ownerCredit`（null 视为默认 60）。
- 次键 `StartTime DESC`：null 视为最小（沉底）。
- 兜底 `TaskID DESC`：保证跨页稳定、无重复/丢失。

> `queryRules` 参数语义：当前 0/非 0 控制 StartTime 倒序/正序。M2 统一以信用分为主键后，`queryRules` 仅作为次键方向的开关保留（倒序/正序 StartTime），主键固定为信用分倒序。若产品后续要求按时间切换，再评估。

### 4.3 接口响应

`GET /user/task/page` 响应结构不变（`Result<PageResult<Task>>`），仅每个 `Task` 多出 `ownerCredit` 字段。前端直接读取 `task.ownerCredit`。

---

## 5. 前端设计

`web/src/views/user/ViewOnGoingList.vue` 任务卡片增加发布者信用分 badge：

- 展示 `task.ownerCredit`（`null` 兜底 60）。
- 等级配色复用 M1 阈值：`<60 待提升` / `60–79 良好` / `≥80 优秀`，以不同颜色（如灰/蓝/金）标识。
- 卡片布局：放在发布者信息行旁（具体样式实现时定，不引入新依赖）。

---

## 6. 边界与已知限制

- **v1 信用分是「接单方」口径**：纯发布者（从未被确认接单）显示默认 60，即使其发布质量高。发布方独立画像属 P2，v1 接受此口径。
- **冷启动**：新用户信用分均为 60，此阶段实际近似按 `StartTime` 排序，属文档化取舍。
- **内存排序**：每请求对去重 OwnerID 做 N 次信用分查询 + 全量匹配行载入。学校量级（几百任务、去重 owner 个位数）可接受；量大后见 §8 升级位。
- **瞬态字段**：`ownerCredit` 仅大厅查询填充，其他 `Task` 使用场景为 `null`，前端需兜底。

---

## 7. 测试

对 `TaskServiceImpl.searchPage(int...)` 写 Mockito 单测（mock `TaskMapper` + `CreditScoreService`）：

| # | 场景 | 断言 |
| :-- | :--- | :--- |
| 1 | 排序 | 不同 ownerCredit 的任务按 credit 降序 |
| 2 | 稳定 tiebreak | 同 credit 同 StartTime 按 TaskID 降序 |
| 3 | 分页切片 | 20 条数据 pageSize=10 时 page1/page2 各 10 条且不重复不遗漏 |
| 4 | ownerCredit 填充 | 每条记录 `getOwnerCredit()` 等于该 owner 的 `getScore()` |
| 5 | null StartTime | 无 StartTime 的任务排在同分任务之后 |

前端以 `npm run build` 验证编译。

---

## 8. 升级位（v2 预留）

- **批量取分**：`CreditScoreService` 增加 `Map<Long, Integer> getScores(List<Long> userIds)`，一次聚合多用户指标，消除逐 owner 3 次 SQL。
- **联合聚合 SQL**：单条查询按 OwnerID 分组聚合三项指标，直接参与排序。
- **Redis 预计算**：对齐架构缓存位，定时刷新信用分列/缓存，SQL 层 JOIN 排序（对应方案 B）。
- **时间衰减混合权重**：`信用分 + 时间衰减` 混合排序，缓解冷启动沉底，产品确认后启用。

---

## 9. 验收要点

- [ ] 大厅列表按 `(credit DESC, StartTime DESC, TaskID DESC)` 排序（Mockito 单测覆盖）。
- [ ] 每页 `Task` 携带 `ownerCredit`，值等于该 owner 的 `CreditScoreService.getScore()`。
- [ ] 分页跨页不重复、不遗漏（total 正确）。
- [ ] 管理端 `TaskAdminController` / `searchPage(TaskPageDTO)` 行为不回归。
- [ ] 前端卡片展示 `ownerCredit` 等级 badge，`null` 兜底 60。
- [ ] 不新增数据库表、不新增列、`application.yml` 无需新配置。
