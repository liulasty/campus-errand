# 前端公共枚举文件集中化实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `web/src/constants/enums.js` 集中 7 组枚举的 dbValue↔webValue 映射与工具函数，并把 14 个前端页面的硬编码状态/类型字符串与选项重构为引用公共常量，消除英文/中文比较不一致。

**Architecture:** 单一数据源 `constants/enums.js` 导出 7 组映射对象 + `enumText`/`enumDb`/`enumOptions` 工具。页面 import 常量，模板经 `data()` 暴露后使用（Vue2 模板只能访问组件属性）。按批次重构，每批 `npm run build` 验证，展示文案不变。

**Tech Stack:** Vue 2.6 / vue-cli 5 / Element UI 2.15

参考设计：`docs/superpowers/specs/2026-08-09-frontend-enums-centralization-design.md`

> **测试说明**：前端无单测框架，验证 = `npm run build` + 手动验收（页面文案与重构前一致）。不引入新框架（YAGNI）。

---

### Task 1: 新增 `web/src/constants/enums.js`

**Files:**
- Create: `web/src/constants/enums.js`

- [ ] **Step 1: 创建公共枚举文件**

写入以下完整内容（映射值与后端 `com.lz.pojo.Enum` 的 `@JsonValue` 逐字一致）：

```js
// web/src/constants/enums.js
// 与后端 com.lz.pojo.Enum 对齐的状态/类型枚举映射（key=dbValue/枚举名，value=中文 webValue）
// 统一为单一数据源：改一处 webValue 全局联动。

// ===== 任务状态（后端 TaskStatus）=====
export const TASK_STATUS = {
  DRAFT: '草稿',
  AUDITING: '审核中',
  AUDIT_FAILED: '审核未通过',
  PENDING_RELEASE: '等待发布',
  ONGOING: '委托发布中',
  ACCEPTED: '已接收',
  COMPLETED: '已完成',
  UNFINISHED: '未完成',
  EXPIRED: '已过期',
  CANCELLED: '已取消'
}

// ===== 接单状态（后端 AcceptStatus）=====
export const ACCEPT_STATUS = {
  UNCHECKED: '未选中',
  CHECKED: '已接收',
  EXPIRED: '已过期',
  PENDING: '待处理',
  CANCEL: '已取消'
}

// ===== 公告状态（后端 AnnouncementStatus）=====
export const ANNOUNCEMENT_STATUS = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  WITHDRAWN: '已撤回'
}

// ===== 实名认证状态（后端 AuthenticationStatus，key=枚举名，查询契约用英文枚举名）=====
export const AUTH_STATUS = {
  UNAUTHORIZED: '未认证',
  AUTHENTICATING: '认证中',
  AUTHENTICATION_FAILED: '认证失败',
  AUTHENTICATED: '认证通过'
}

// ===== 通知类型（后端 NotificationsType）=====
export const NOTIFICATION_TYPE = {
  OWN: '个人信息通知',
  TASK: '委托信息通知',
  SYSTEM: '系统信息通知',
  MARKETING: '营销信息通知'
}

// ===== 履约更新类型（后端 TaskUpdateType）=====
export const TASK_UPDATE_TYPE = {
  AUDITING: '审核',
  PUBLISHED: '发布委托',
  CREATED: '新委托创建',
  RESULT: '委托结果',
  FALLBACK_DRAFT: '回退草稿',
  PROGRESS_UPDATE: '进度更新',
  CONTACTED: '已联系',
  PICKED_UP: '已取件',
  DELIVERED: '已送达',
  AUTO_ADVANCE: '自动推进'
}

// ===== 任务阶段（后端 TaskPhase，仅英文，查询用）=====
export const TASK_PHASE = {
  EDITING_AND_AUDITING: 'EDITING_AND_AUDITING',
  PUBLISHING_AND_EXECUTION: 'PUBLISHING_AND_EXECUTION',
  LIFECYCLE_TERMINATION: 'LIFECYCLE_TERMINATION'
}

// ===== 通用工具 =====

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

- [ ] **Step 2: 提交**

```bash
git add web/src/constants/enums.js
git commit -m "feat: 新增前端公共枚举映射文件 constants/enums.js（7组枚举+工具函数）"
```

### Task 2: 重构用户任务相关页（TaskStatus/ACCEPT_STATUS）

**Files:**
- Modify: `web/src/views/user/ViewOnGoingList.vue`
- Modify: `web/src/components/Delegation.vue`
- Modify: `web/src/views/user/MyDelegationPublishList.vue`
- Modify: `web/src/views/user/MyDelegationAcceptList.vue`
- Modify: `web/src/views/admin/ExpireDelegationList.vue`

通用规则（每个文件）：
1. `import { TASK_STATUS } from '@/constants/enums'`（MyDelegationAcceptList 另加 `ACCEPT_STATUS`）。
2. 在 `data()` 的返回对象首行加 `TASK_STATUS,`（模板经 `this.TASK_STATUS` 访问；`MyDelegationAcceptList` 加 `ACCEPT_STATUS,`）。
3. 替换硬编码字符串（见下表）。

- [ ] **Step 1: ViewOnGoingList.vue**

> 注意：查询下拉的 `value="ONGOING"` 等是**发送给后端的 dbValue（英文）**，必须保持不变，不能改成 `TASK_STATUS.ONGOING`（那会得到中文 webValue，导致查询失效）。本步骤只改「比较」：

| 位置 | 旧 | 新 |
|------|-----|-----|
| 69 | `scope.row.status==='委托发布中'` | `scope.row.status===TASK_STATUS.ONGOING` |
| 70 | `scope.row.status==='已接收'` | `scope.row.status===TASK_STATUS.ACCEPTED` |
| 71 | `scope.row.status==='已完成'` | `scope.row.status===TASK_STATUS.COMPLETED` |
| 147 | `form.task.status=='已完成'` | `form.task.status==TASK_STATUS.COMPLETED` |
| 151 | `form.task.status=='委托发布中'` | `form.task.status==TASK_STATUS.ONGOING` |

- [ ] **Step 2: Delegation.vue**

| 位置 | 旧 | 新 |
|------|-----|-----|
| 64 | `t.status === '草稿'` | `t.status === TASK_STATUS.DRAFT` |
| 74 | `t.status === '审核中'` | `t.status === TASK_STATUS.AUDITING` |
| 102-105 | `'草稿'`/`'审核中'`/`'等待发布'`/`'审核未通过'` | `TASK_STATUS.DRAFT`/`TASK_STATUS.AUDITING`/`TASK_STATUS.PENDING_RELEASE`/`TASK_STATUS.AUDIT_FAILED` |
| 110/115/118/122 | `'草稿'`/`'审核中'`/`'等待发布'`/`'审核未通过'` | 同上常量 |

- [ ] **Step 3: MyDelegationPublishList.vue**

| 位置 | 旧 | 新 |
|------|-----|-----|
| 14 el-option | `value="CANCELLED"` | `:value="TASK_STATUS.CANCELLED"` |
| 111 | `form.task.status === '委托发布中'` | `=== TASK_STATUS.ONGOING` |
| 155 | `'已接收' \|\| ... '已完成'` | `TASK_STATUS.ACCEPTED` / `TASK_STATUS.COMPLETED` |
| 189 | `'已接收'` | `TASK_STATUS.ACCEPTED` |
| 200 | `'已完成'` | `TASK_STATUS.COMPLETED` |
| 457 | `'已接收' \|\| '已完成'` | `TASK_STATUS.ACCEPTED` / `TASK_STATUS.COMPLETED` |
| 322 操作 map | `"已取消": {` | `[TASK_STATUS.CANCELLED]: {` |

- [ ] **Step 4: MyDelegationAcceptList.vue**（import `ACCEPT_STATUS, TASK_STATUS`，data 加两者）

| 位置 | 旧 | 新 |
|------|-----|-----|
| 106 | `form.task.status === '已接收'` | `=== TASK_STATUS.ACCEPTED` |
| 381 | `'已接收'` | `TASK_STATUS.ACCEPTED` |
| 245 操作 map | `"待处理": {` | `[ACCEPT_STATUS.PENDING]: {` |
| 251 | `"已取消": {` | `[ACCEPT_STATUS.CANCEL]: {` |
| 257 | `"已过期": {` | `[ACCEPT_STATUS.EXPIRED]: {` |
| 263 | `"未选中": {` | `[ACCEPT_STATUS.UNCHECKED]: {` |
| 269 | `"已接收": {` | `[ACCEPT_STATUS.CHECKED]: {` |

- [ ] **Step 5: ExpireDelegationList.vue**（import `TASK_STATUS`，data 加 `TASK_STATUS`）

| 位置 | 旧 | 新 |
|------|-----|-----|
| 172 操作 map | `"已取消": {` | `[TASK_STATUS.CANCELLED]: {` |

- [ ] **Step 6: 构建校验 + 提交**

```bash
cd web && npm run build
```
预期：`Build complete`。然后：

```bash
cd .. && git add web/src/views/user/ViewOnGoingList.vue web/src/components/Delegation.vue web/src/views/user/MyDelegationPublishList.vue web/src/views/user/MyDelegationAcceptList.vue web/src/views/admin/ExpireDelegationList.vue
git commit -m "refactor: 用户任务相关页改用公共枚举常量（TaskStatus/ACCEPT_STATUS）"
```

### Task 3: 重构管理端任务页

**Files:**
- Modify: `web/src/views/admin/AuditList.vue`
- Modify: `web/src/views/admin/DraftList.vue`

- [ ] **Step 1: AuditList.vue**（import `TASK_PHASE`，data 加 `TASK_PHASE`）

| 位置 | 旧 | 新 |
|------|-----|-----|
| 201 | `"EDITING_AND_AUDITING"` | `TASK_PHASE.EDITING_AND_AUDITING` |
| 201 | `"PUBLISHING_AND_EXECUTION"` | `TASK_PHASE.PUBLISHING_AND_EXECUTION` |

（`activeTab` 的 `'DRAFT'`/`'AUDIT'` 是 UI Tab 键，保留不动。）

- [ ] **Step 2: DraftList.vue**（import `TASK_STATUS`，data 加 `TASK_STATUS`）

| 位置 | 旧 | 新 |
|------|-----|-----|
| 168 操作 map | `"草稿": {` | `[TASK_STATUS.DRAFT]: {` |
| 174 | `"审核中": {` | `[TASK_STATUS.AUDITING]: {` |
| 180 | `"审核未通过": {` | `[TASK_STATUS.AUDIT_FAILED]: {` |
| 186 | `"等待发布": {` | `[TASK_STATUS.PENDING_RELEASE]: {` |

（`record.type = this.taskType[...]` 是委托分类映射，非状态枚举，不改。）

- [ ] **Step 3: 构建校验 + 提交**

```bash
cd web && npm run build && cd .. && git add web/src/views/admin/AuditList.vue web/src/views/admin/DraftList.vue
git commit -m "refactor: 管理端任务页改用公共枚举常量（TASK_PHASE/TASK_STATUS）"
```

### Task 4: 重构公告/消息/通知页

**Files:**
- Modify: `web/src/views/admin/SystemBulletinList.vue`
- Modify: `web/src/components/noticeList.vue`
- Modify: `web/src/views/admin/DelegationUpdateRecords.vue`

- [ ] **Step 1: SystemBulletinList.vue**（import `ANNOUNCEMENT_STATUS`，data 加 `ANNOUNCEMENT_STATUS`）

> 查询下拉 `value="PUBLISHED"`/`value="WITHDRAWN"` 是发送后端的 dbValue（英文），保持不变。

| 位置 | 旧 | 新 |
|------|-----|-----|
| 343 statusLabel map | `{ DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }` | `{ DRAFT: ANNOUNCEMENT_STATUS.DRAFT, PUBLISHED: ANNOUNCEMENT_STATUS.PUBLISHED, WITHDRAWN: ANNOUNCEMENT_STATUS.WITHDRAWN }` |

- [ ] **Step 2: noticeList.vue**（import `NOTIFICATION_TYPE`，data 加 `NOTIFICATION_TYPE`）

| 位置 | 旧 | 新 |
|------|-----|-----|
| 59-72 typeOptions | `value: "个人信息通知"` / `"委托信息通知"` / `"营销信息通知"` / `"系统信息通知"` | `value: NOTIFICATION_TYPE.OWN` / `NOTIFICATION_TYPE.TASK` / `NOTIFICATION_TYPE.MARKETING` / `NOTIFICATION_TYPE.SYSTEM` |
| 134-143 case | `case "个人信息通知":` 等 | `case NOTIFICATION_TYPE.OWN:` 等 |

- [ ] **Step 3: DelegationUpdateRecords.vue**（import `TASK_UPDATE_TYPE`，data 加 `TASK_UPDATE_TYPE`）

| 位置 | 旧 | 新 |
|------|-----|-----|
| 35 | `scope.row.updateType === '自动推进'` | `=== TASK_UPDATE_TYPE.AUTO_ADVANCE` |
| 164 | `row.updateType === '自动推进'` | `=== TASK_UPDATE_TYPE.AUTO_ADVANCE` |

（`getNodeMeta` 已集中节点标签，不改。）

- [ ] **Step 4: 构建校验 + 提交**

```bash
cd web && npm run build && cd .. && git add web/src/views/admin/SystemBulletinList.vue web/src/components/noticeList.vue web/src/views/admin/DelegationUpdateRecords.vue
git commit -m "refactor: 公告/消息/通知页改用公共枚举常量（ANNOUNCEMENT_STATUS/NOTIFICATION_TYPE/TASK_UPDATE_TYPE）"
```

### Task 5: 重构实名/用户页

**Files:**
- Modify: `web/src/views/admin/RealNameAudit.vue`
- Modify: `web/src/views/admin/UserList.vue`
- Modify: `web/src/views/user/MyInfo.vue`
- Modify: `web/src/views/user/CreateDelegation.vue`

- [ ] **Step 1: RealNameAudit.vue**（import `AUTH_STATUS`，data 加 `AUTH_STATUS`）

| 位置 | 旧 | 新 |
|------|-----|-----|
| 62 | `authStatus === '认证中'` | `=== AUTH_STATUS.AUTHENTICATING` |
| 184-187 map | `'认证中'`/`'认证通过'`/`'认证失败'`/`'未认证'`（key 与 text） | 对应 `AUTH_STATUS.AUTHENTICATING`/`AUTH_STATUS.AUTHENTICATED`/`AUTH_STATUS.AUTHENTICATION_FAILED`/`AUTH_STATUS.UNAUTHORIZED` |

- [ ] **Step 2: UserList.vue**（import `AUTH_STATUS`，data 加 `AUTH_STATUS`）

| 位置 | 旧 | 新 |
|------|-----|-----|
| 114 | `authStatus == '认证中'` | `== AUTH_STATUS.AUTHENTICATING` |
| 118-119 | `'认证失败'` | `AUTH_STATUS.AUTHENTICATION_FAILED` |
| 123 | `'认证通过'` | `AUTH_STATUS.AUTHENTICATED` |
| 179 | `'认证中'` | `AUTH_STATUS.AUTHENTICATING` |
| 196 | `'认证通过'` | `AUTH_STATUS.AUTHENTICATED` |

（`creditScoreFilters` 的 `{ text: '未认证', value: 0 }` 是筛选器配置，value 传后端查询参数，保留不动。）

- [ ] **Step 3: MyInfo.vue**（import `AUTH_STATUS`，data 加 `AUTH_STATUS`）

| 位置 | 旧 | 新 |
|------|-----|-----|
| 6 | `authState === '认证中'` | `=== AUTH_STATUS.AUTHENTICATING` |
| 201 | `info.authStatus === '认证中'` | `=== AUTH_STATUS.AUTHENTICATING` |
| 205 | `info.authStatus === '认证通过'` | `=== AUTH_STATUS.AUTHENTICATED` |

（`authState()` 的 `{1:'未认证',2:'认证中',3:'认证失败',4:'认证通过'}` 是前端内部 UI 状态号 map，与后端 dbValue 无关，**保留不动**。）

- [ ] **Step 4: CreateDelegation.vue**（import `AUTH_STATUS`，data 加 `AUTH_STATUS`）

| 位置 | 旧 | 新 |
|------|-----|-----|
| 129 | `this.userInfo.authStatus != "认证通过"` | `!= AUTH_STATUS.AUTHENTICATED` |

- [ ] **Step 5: 构建校验 + 提交**

```bash
cd web && npm run build && cd .. && git add web/src/views/admin/RealNameAudit.vue web/src/views/admin/UserList.vue web/src/views/user/MyInfo.vue web/src/views/user/CreateDelegation.vue
git commit -m "refactor: 实名/用户页改用公共枚举常量（AUTH_STATUS）"
```

### Task 6: 全量构建校验 + 手动验收清单

**Files:**（只读验证，不改代码）

- [ ] **Step 1: 全量构建**

```bash
cd web && npm run build
```
预期：`Build complete`，无报错。

- [ ] **Step 2: 手动验收清单（dev server 复核）**

| 场景 | 操作 | 预期 |
|------|------|------|
| 委托大厅 | zhangsan 登录看 ONGOING 任务 | 状态 tag 显示「发布中」（绿色），非裸值 |
| 我的委托-发布 | 看已取消任务 | 操作按钮区正常（map key 用常量后行为不变） |
| 草稿列表 | admin 看草稿/审核中 | 操作按钮「审核」「退为草稿」正常 |
| 公告列表 | admin 看公告 | 状态「草稿/已发布/已撤回」正常 |
| 实名审核 | admin 看认证中用户 | 审核操作按钮正常显示 |
| 通知中心 | 用户看通知 | 类型下拉「个人信息/委托信息/营销/系统」正常 |

- [ ] **Step 3: 问题回退**

若构建失败或某页异常，回到对应 Task 修正并重新提交；验证不改代码，无额外提交。

---

## 说明

- 纯前端集中化，不改后端枚举/接口契约；所有 webValue 与后端逐字一致，展示文案不回归。
- Vue2 模板访问常量：经 `data()` 暴露（`TASK_STATUS,`），脚本内直接用 import 常量。
- 涉及 `TASK_STATUS`/`ACCEPT_STATUS`/`ANNOUNCEMENT_STATUS`/`AUTH_STATUS`/`NOTIFICATION_TYPE`/`TASK_UPDATE_TYPE`/`TASK_PHASE` 共 7 组，14 个页面文件。
