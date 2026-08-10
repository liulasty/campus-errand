# MVP 1.0 收尾归档

> **文档状态**：收尾基线（代码/数据/债务归档）
> **更新日期**：2026-08-01
> **基线 tag**：`mvp1.0-release-base`

---

## 一、已完成范围（对齐 MVP 1.0）

| 模块 | 状态 | 关键提交/文档 |
| :--- | :--- | :--- |
| M1 信用引擎 | ✅ | 信用分算法 + 可插拔 `CreditCalculator` + 档案展示；`docs/M1_credit-engine-design.md` |
| M2 智能曝光 | ✅ | `users.CreditScore` 列 + SQL JOIN 排序（方案 B），大厅信用分 badge |
| M3 防断流 | ✅ | 节点超时自动推进 + 完成超时自动完成，`AUTO_ADVANCE` 留痕审计 |
| P2 实名认证 | ✅ | L1 门禁 + `IIdentityVerifier` 预留 + 独立实名审核页 + 驳回原因 |
| 管理员列表对齐 | ✅ | 信用分/实名等级/用户名替换 ID/时间格式化/M3 审计标识 |
| 管理员菜单重构 | ✅ | 5 大模块收敛 + 委托审核/消息 Tab 合并 + 实名审核入口 |

## 二、标准测试账号与数据（验收基线）

| 账号 | 角色 | 密码 | 说明 |
| :--- | :--- | :--- | :--- |
| `majiaqi` | ADMIN | `admin123456` | 后台审核/菜单验收 |
| `zhangsan` | student | `test123456` | 用户侧学生主流程 |
| `lisiyuan` | teacher | `test123456` | 用户侧教师 |
| `testapply` | student | `test123456` | 已通过实名（验收初始为待审核，管理员已通过） |

预置数据：2 条标准 ONGOING 委托（代取快递/代买午餐，owner 2/3）。

## 三、遗留技术债（按优先级）

**优先级说明**：P0=下迭代优先修复（直接影响验收观感/体验）；P1=常规 backlog；P2=可延后增值。

### P0（建议下一迭代优先）
| # | 项 | 说明 |
| :-- | :--- | :--- |
| 5 | 接单接口空请求 NPE | `/user/accept` 空 body → 业务逻辑 NPE（既有） |
| 6 | 大厅状态列显示错误 | 后端返回 `status:"委托发布中"`（webValue），前端比较 `'ONGOING'` 不匹配，状态 tag 走 else 分支显示裸值 |
| 7 | 前端方案 A 未全覆盖 | catch / SUCCESS_CODE 只做了大厅/实名/履约记录/Home；发布/接单/查看联系方式等写操作页 catch 兜底未全部补 |

### P1（常规 backlog）
| # | 项 | 说明 |
| :-- | :--- | :--- |
| 2 | 任务详情实名旁证展示 | 发布者掩码学号 + L1 徽章 + 信用分（设计 §6，已定义掩码规则） |
| 3 | 履约记录操作人 ID → 用户名 | 需后端联表 users |
| 4 | 委托导出 Excel 字段同步 | 替换 ID、增加信用/实名字段 |
| 10 | searchPageByAdmin 富化 N+1 | ownerName/ownerCredit 逐条 selectById，数据量大时性能隐患 |
| 11 | 大厅 SQL 未带 ownerName | `searchHallPage` 只 JOIN CreditScore，旁证需补字段 |
| 16 | `taskType` vs `type` 历史遗留 | 部分旧页面可能仍裸读 `type`（已修 ViewOnGoingList/AuditList，未全面排查） |
| 17 | 状态列 enum 比较混乱 | 前端多处用英文比较 vs 后端返回中文 webValue，隐性 bug 温床 |
| 18 | identity_no 掩码仅前端实现 | 后端返回完整值，导出/详情需统一脱敏策略 |

### P2（可延后增值）
| # | 项 | 说明 |
| :-- | :--- | :--- |
| 1 | 信用/数据统计看板 | 信用分分布、委托履约统计、自动推进异常报表（增值模块） |
| 8 | L2 校园卡认证 | 字段/接口/页面占位预留，未实现 |
| 9 | 教务系统对接 | `IIdentityVerifier` 预留，`EduSystemIdentityVerifier` 未实现（当前 manual 模式） |
| 12 | M3 定时任务单线程 | 大批量任务拉长耗时，需线程池分批（已文档化升级位） |
| 13 | M3 集群重复执行 | 乐观锁兜底，扩容需 ShedLock（已文档化） |
| 14 | 前端方案 B（响应 unwrap） | 拦截器解包规范化，几十处调用点待改，明确延后 |
| 15 | M3 自动推进不校验真实性 | 超时即推进，信用惩罚钩子（`AUTO_ADVANCE` 留痕）P2 接 |

### 环境/运维（非代码债，影响验收；不一定要实现）

| # | 项 | 说明 |
| :-- | :--- | :--- |
| 19 | Redis / RabbitMQ 未装 | 消息通知、缓存功能不可用；`TaskOverDue` 发 RabbitMQ 通知会失败 |
| 20 | 消息通知整体降级 | 自动推进/审核等系统通知依赖 RabbitMQ，未装则静默失败 |

### 契约修复（2026-08-10 状态机用例触发，D-18 已修）

| 项 | 说明 |
| :--- | :--- |
| EndTime 截断到本地 00:00 | 根因：自定义 `DateDeserializer` 先按 lenient `yyyy-MM-dd` 前缀解析，`2026-08-10T23:59:59Z` 只吃掉日期、时间丢失。已改为 datetime 格式优先 + `ParsePosition` 整串匹配，时间分量保留。 |
| 过去 end 放行 | `confirmTask` 现校验 end 须晚于当前时间，过去/当天-已过 end → code=0「截止时间必须晚于当前时间」；任务保持 PENDING_RELEASE。 |
| 过期不可接单契约 | 原靠「发布过去 end」构造已不可行（发布被拦截）；DSL 无 SQL 写步骤。该契约改为时间依赖延后（同 T16/E02）。 |
| 附带发现 | `DateSerializer` 仍按 `yyyy-MM-dd` 输出（响应展示日期粒度，未改）；前端 `el-date-picker` 发送中文日期字符串 `yyyy年MM月dd日HH:mm:ss` 与反序列化格式不匹配，为既有潜在问题（非验收路径），建议后续核查。 |

### 验收补修（2026-08-10，2.1/2.2）

| 项 | 说明 |
| :--- | :--- |
| `/task/searchPage` status 参数生效 | 原实现硬编码 `status=AUDITING` 忽略 `TaskPageDTO.status`。已修为 status 参数生效，缺省保持 AUDITING（审核队列默认，兼容 TC-014）。注意：大厅检索是 `/user/task/page`，本接口为审核/通用分页。 |
| 发布留痕 PUBLISHED | `confirmTask` 发布成功现写 `taskupdates.UpdateType=PUBLISHED`（与 CREATED/AUDITING 对齐）。管理员放行 `allowPublish`（→PENDING_RELEASE）写 `delegate_audit_records`，非 taskupdates。受影响用例：TC-041 计数 2→3、TC-036 补 PUBLISHED 断言。 |

## 四、废弃路由清单（组件保留，仅移出菜单）

| 路由 | 原页面 | 处置 |
| :--- | :--- | :--- |
| `/draftList` | 草稿与审核 | 已并入 `/auditList` 委托审核 Tab，路由保留组件，移出菜单 |
| `/notifications` | 消息列表 | 已并入 `/notificationReadStatus` 消息管理 Tab |
| `/systemNoticeList` | 系统通知 | 空壳页，移出菜单 |
| `/myDelegationProgress` | 履约进度 | 无 taskId 直接访问会重定向，移出菜单 |

## 五、环境限制

- Redis / RabbitMQ 未装：缓存与消息通知功能不可用（后端惰性连接可启动）。
- GitHub 推送需网络恢复后补（gitee 已同步全部提交）。
