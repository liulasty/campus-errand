# 前端公共枚举文件集中化设计

日期：2026-08-09
状态：已批准（全量枚举 + 重构现有页面）

## 背景与问题

前端各页面散落硬编码状态/类型字符串：既有后端 API 返回的中文 webValue（如 `'委托发布中'`、`'认证通过'`），也有提交/查询用的英文 dbValue（如 `'ONGOING'`、`'PUBLISHED'`）。P0-6 事故即源于英文/中文比较不一致。需集中为单一数据源，方便全局修改与对齐。

## 目标

1. 新建公共枚举文件 `web/src/constants/enums.js`，集中 7 组枚举的 dbValue↔webValue 映射（与后端 `com.lz.pojo.Enum` 的 `@JsonValue` 逐字对齐）。
2. 提供通用工具 `enumText` / `enumDb` / `enumOptions`。
3. 重构现有页面，替换硬编码字符串/选项/操作 map 为公共常量，不改变任何展示文案。

## 文件内容

### 位置
`web/src/constants/enums.js`（与 `constants/http.js` 同目录，常量归属）。

### 枚举映射（key = dbValue/枚举名，value = 中文 webValue）

| 导出 | 映射 | 备注 |
|------|------|------|
| `TASK_STATUS` | DRAFT草稿 / AUDITING审核中 / AUDIT_FAILED审核未通过 / PENDING_RELEASE等待发布 / ONGOING委托发布中 / ACCEPTED已接收 / COMPLETED已完成 / UNFINISHED未完成 / EXPIRED已过期 / CANCELLED已取消 | 后端 TaskStatus |
| `ACCEPT_STATUS` | UNCHECKED未选中 / CHECKED已接收 / EXPIRED已过期 / PENDING待处理 / CANCEL已取消 | 后端 AcceptStatus |
| `ANNOUNCEMENT_STATUS` | DRAFT草稿 / PUBLISHED已发布 / WITHDRAWN已撤回 | 后端 AnnouncementStatus |
| `AUTH_STATUS` | UNAUTHORIZED未认证 / AUTHENTICATING认证中 / AUTHENTICATION_FAILED认证失败 / AUTHENTICATED认证通过 | 后端 AuthenticationStatus；key=枚举名（查询契约用英文枚举名） |
| `NOTIFICATION_TYPE` | OWN个人信息通知 / TASK委托信息通知 / SYSTEM系统信息通知 / MARKETING营销信息通知 | 后端 NotificationsType |
| `TASK_UPDATE_TYPE` | AUDITING审核 / PUBLISHED发布委托 / CREATED新委托创建 / RESULT委托结果 / FALLBACK_DRAFT回退草稿 / PROGRESS_UPDATE进度更新 / CONTACTED已联系 / PICKED_UP已取件 / DELIVERED已送达 / AUTO_ADVANCE自动推进 | 后端 TaskUpdateType |
| `TASK_PHASE` | EDITING_AND_AUDITING / PUBLISHING_AND_EXECUTION / LIFECYCLE_TERMINATION | 后端 TaskPhase；仅英文，查询用，无中文 |

### 通用工具

```js
// dbValue 或 webValue → 中文展示（未知值原样返回）
export function enumText(enumMap, value) {
  if (value === null || value === undefined || value === '') return ''
  return enumMap[value] !== undefined ? enumMap[value] : value
}

// webValue 或 dbValue → dbValue（查询/提交参数用）
export function enumDb(enumMap, value) {
  if (value === null || value === undefined || value === '') return ''
  if (enumMap[value] !== undefined) return value
  const db = Object.keys(enumMap).find(k => enumMap[k] === value)
  return db || value
}

// el-select 选项 [{value: dbValue, label: webValue}]，可选「全部」占位
export function enumOptions(enumMap, { empty = false, emptyLabel = '全部' } = {}) {
  const opts = Object.entries(enumMap).map(([value, label]) => ({ value, label }))
  return empty ? [{ value: '', label: emptyLabel }, ...opts] : opts
}
```

## 现有页面重构（覆盖 14 个文件）

替换规则：
1. **状态比较**：`scope.row.status === '委托发布中'` → `scope.row.status === TASK_STATUS.ONGOING`；认证 `=== '认证通过'` → `AUTH_STATUS.AUTHENTICATED`。
2. **el-select 选项**：硬编码 `el-option` → `v-for="o in enumOptions(TASK_STATUS)"`。
3. **查询参数**：`status: 'ONGOING'` → `status: TASK_STATUS.ONGOING`（值本身是英文，用常量便于联动改名）。
4. **状态操作 map**：`'已取消': {...}` 的 key → `TASK_STATUS.CANCELLED`。
5. **Vue2 模板约束**：import 的函数必须挂入 `methods`（或经 computed/data 暴露），不能模板直用 import 模块函数。

重构文件清单：
- 任务相关：`ViewOnGoingList.vue`、`Delegation.vue`、`MyDelegationPublishList.vue`、`MyDelegationAcceptList.vue`、`ExpireDelegationList.vue`、`AuditList.vue`、`DraftList.vue`
- 公告/消息：`SystemBulletinList.vue`、`noticeList.vue`、`DelegationUpdateRecords.vue`
- 实名/用户：`RealNameAudit.vue`、`UserList.vue`、`MyInfo.vue`、`CreateDelegation.vue`

## 风险与处理

1. **中文逐字一致**：映射值取自后端枚举源码，重构不改变任何展示文案；用 `npm run build` 验证。
2. **MyInfo 认证 code map**：`{1:'未认证',2:'认证中',3:'认证失败',4:'认证通过'}` 与后端 AuthenticationStatus dbValue(0-3) 疑似偏移。重构时先核对 `this.code` 来源；若确为偏移，修正为后端枚举名映射（`AUTH_STATUS`）。
3. **分批重构**：先建文件 + 任务相关核心页，构建通过后再推广其余页面，避免一次大改难以定位。
4. **枚举名/别名不动**：不修改任何后端枚举值或接口契约，纯前端集中化。

## 验收关注点

- `web/src/constants/enums.js` 导出 7 组映射 + 3 个工具函数。
- 重构后各页面展示文案与重构前一致（无回归）。
- `npm run build` 通过。
- 全局改一处（如某 webValue）即可联动所有页面。
