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

> **赋值范围约束**：`ownerCredit` 仅用户大厅分页接口（`searchPage(int...)`）赋值，其余查询接口该字段恒为 `null`；后端不统一填充以减少性能损耗，**所有前端页面读取时必须 `null → 60` 兜底**。

### 4.2 `TaskServiceImpl.searchPage(int...)` 改造

```java
// 注入 CreditScoreService（沿用类内注入方式）
// 1. 保留现有过滤（状态/类型/地点/描述），去掉 wrapper.orderByDesc/orderByAsc("StartTime")
// 2. 查询匹配记录（内存排序）：taskMapper.selectList(wrapper.last("LIMIT 1000"))
//    —— 硬上限 1000 条，超过截断，规避全量加载 OOM / 慢查询
// 3. total 单独统计：taskMapper.selectCount(独立 wrapper 副本，不含 last("LIMIT..."))
//    —— 不能用 list.size()（截断后失真）
// 4. 去重收集 OwnerID：list.stream().map(Task::getOwnerId).filter(Objects::nonNull).distinct()
//    再逐个 creditScoreService.getScore(ownerId) —— 同一 owner 仅查一次
// 5. 对每行任务设置 task.setOwnerCredit(score)
// 6. 按 (credit DESC, StartTime DESC, TaskID DESC) 内存排序
// 7. 内存分页切片并拷贝：records = new ArrayList<>(list.subList(from, to))
//    —— 切片后新建列表，释放全量大列表引用，便于 GC
//    from = (pageNum-1)*pageSize, to = min(from+pageSize, size)
// 8. PageResult(total, records)
```

排序比较器要点：
- 主键 `credit DESC`：`ownerCredit`（null 视为默认 60）。
- 次键 `StartTime DESC`：null 视为最小（沉底）。
- 兜底 `TaskID DESC`：保证跨页稳定、无重复/丢失。

**`queryRules` 语义（明确）**：
- `queryRules == 0`：信用分 DESC + `StartTime` DESC。
- `queryRules != 0`：信用分 DESC + `StartTime` ASC。
- 主键固定为信用分倒序；`queryRules` **仅**控制次键时间方向。**告知前端**：切换 `queryRules` 不会改变信用分排序优先级，只影响同分任务的发布时间顺序。

**性能与安全约束**：
- 单次匹配硬上限 1000 条（`LIMIT 1000`）。
- 当过滤后匹配数持续 > 500，触发 v2 批量取分 + 缓存优化迭代（见 §8）。
- 深分页（pageNum 过大）仍会加载全部匹配数据；**产品侧建议前端限制最大页码为 10**，超限提示用户缩小筛选条件。

### 4.3 接口响应

`GET /user/task/page` 响应结构不变（`Result<PageResult<Task>>`），仅每个 `Task` 多出 `ownerCredit` 字段。前端直接读取 `task.ownerCredit`。

---

## 5. 前端设计

`web/src/views/user/ViewOnGoingList.vue` 任务卡片增加发布者信用分 badge：

- 展示 `task.ownerCredit`，**统一兜底**三类场景：字段不存在、值为 `null`、发布者纯新用户（默认 60），一律展示 60。
- **封装复用**：信用分等级判断抽成工具函数（如 `web/src/utils/creditLevel.js`），`displayScore` 兜底 + `displayLevel` 判定与 M1 信用档案页共用同一实现，避免多处重复 if。
- 等级配色复用 M1 阈值：`<60 待提升` / `60–79 良好` / `≥80 优秀`，用项目现有色板（灰 / 蓝 / 金），不引入第三方组件；badge 增加 `el-tooltip` hover 展示等级文字（待提升 / 良好 / 优秀）。
- **页面范围**：仅 `ViewOnGoingList.vue` 展示信用 badge；「我的发布 / 我的接单 / 管理端列表」不展示，避免多余渲染。
- 卡片布局：放在发布者信息行旁（具体样式实现时定）。

---

## 6. 边界与已知限制

- **v1 信用分是「接单方」口径**：纯发布者（从未被确认接单）显示默认 60，即使其发布质量高。**该口径为已知取舍，需同步告知产品**，避免后续运营质疑排序不准。发布方独立画像属 P2，v1 接受此口径。
- **冷启动**：新用户信用分均为 60，此阶段实际近似按 `StartTime` 排序，属文档化取舍。
- **内存排序**：每请求对去重 OwnerID 做 N 次信用分查询 + 全量匹配行载入。学校量级（几百任务、去重 owner 个位数）可接受；量大后见 §8 升级位。
- **瞬态字段**：`ownerCredit` 仅大厅查询填充，其他 `Task` 使用场景为 `null`，前端需兜底。

---

## 7. 测试

对 `TaskServiceImpl.searchPage(int...)` 写 Mockito 单测。

**测试基建规范**：
- `@InjectMocks TaskServiceImpl` + `@Mock TaskMapper`、`@Mock CreditScoreService`（`searchPage` 仅触碰这两个依赖，其余注入字段在本次测试中保持 `null` 且不被访问）。
- 每条用例末尾 `verify(creditScoreService, times(N)).getScore(ownerId)` 校验**去重后查询次数**，同一 owner 无重复调用。
- 断言用 AssertJ 链式（`org.assertj.core.api.Assertions`，随 spring-boot-starter-test 提供）。

| # | 场景 | 断言 |
| :-- | :--- | :--- |
| 1 | 排序 | 不同 ownerCredit 的任务按 credit 降序 |
| 2 | 稳定 tiebreak | 同 credit 同 StartTime 按 TaskID 降序 |
| 3 | 分页切片 | 20 条数据 pageSize=10 时 page1/page2 各 10 条且不重复不遗漏 |
| 4 | ownerCredit 填充 | 每条记录 `getOwnerCredit()` 等于该 owner 的 `getScore()` |
| 5 | null StartTime | 无 StartTime 的任务排在同分任务之后 |
| 6 | 多 owner 含 null 信用分 | 部分 owner `getScore` 返回 null，兜底 60 参与排序，低信用沉底 |
| 7 | 过滤后不足一页 | 总 5 条、pageSize=10，返回全部 5 条且 total=5 |

前端以 `npm run build` 验证编译。

---

## 8. 升级位（v2 预留）

- **批量取分**：`CreditScoreService` 增加 `Map<Long, Integer> getScores(List<Long> userIds)`，一次聚合多用户指标，消除逐 owner 3 次 SQL。
- **联合聚合 SQL**：单条查询按 OwnerID 分组聚合三项指标，直接参与排序。
- **Redis 预计算**：对齐架构缓存位，**定时任务（如每小时）刷新全量用户信用分缓存**，大厅查询直接 JOIN 缓存结果，完全移除内存排序（对应方案 B）。
- **时间衰减混合权重**：`信用分 + 时间衰减` 混合排序，缓解冷启动沉底，产品确认后启用。

---

## 9. 验收要点

- [ ] 大厅列表按 `(credit DESC, StartTime DESC, TaskID DESC)` 排序（Mockito 单测覆盖）。
- [ ] 每页 `Task` 携带 `ownerCredit`，值等于该 owner 的 `CreditScoreService.getScore()`。
- [ ] 分页跨页不重复、不遗漏（total 正确）。
- [ ] 管理端 `TaskAdminController` / `searchPage(TaskPageDTO)` 行为不回归。
- [ ] 前端卡片展示 `ownerCredit` 等级 badge，`null` 兜底 60（仅 `ViewOnGoingList.vue`）。
- [ ] 单接口最大匹配条数不超过 1000（`LIMIT 1000` 截断防护）。
- [ ] 同一 owner 仅调用一次 `CreditScoreService.getScore`，无重复 SQL（`verify(times(N))` 覆盖）。
- [ ] 不新增数据库表、不新增列、`application.yml` 无需新配置。
