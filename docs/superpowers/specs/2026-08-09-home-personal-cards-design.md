# 首页新增「与我相关的委托」「我的接单记录」卡片设计

日期：2026-08-09
范围：`web/src/views/dashboard/Home.vue` 新增两张卡片，**纯前端改动，不动后端接口、不重启**。

## 背景

- F-01（前端审计发现）：`Home.vue` 原先硬编码 `getData(2)` → 已修复为 `getData(currentUserId)`，接口按登录用户返回本人数据。
- 用户确认：首页需展示「与我相关的委托」「我的接单记录」两张卡，数据来自 `getNewTask` 响应的 `tasksWithUser` / `taskAcceptRecordsWithUser` 字段（已按登录用户返回）。
- 历史版本（重构前 `f895ee1` 与重构后 `HEAD`）均只存储这两个字段、**从未真正渲染**，本次为新建卡片。

## 布局

- Row3（底部四宫格）下方新增 Row4，两卡 `:xs="24" :md="12"` 并排。
- 沿用现有 `dashboard-card` 视觉规范：白底、圆角 10px、分层阴影、hover 上浮；header 统一「图标 + 标题 + 右上更多」。
- 新增 class 统一 `dashboard-` 前缀。

## 卡 A：与我相关的委托

- 数据源：`tasksWithUser`（`Task[]`，`getNewTask` 已返回，按 `currentUserId`）。
- 列：内容（`description`）｜状态（`status` 后端中文 webValue，如「委托发布中/已接收」）｜发布时间（`startTime`，`dateTime` 过滤）。
- 取前 5 条；空态 `el-empty`「暂无相关委托」；右上「更多」跳 `taskMorePath`（与最新委托一致：ADMIN→/publishedList，USER→/viewOnGoingList）。

## 卡 B：我的接单记录

- 数据源：`taskAcceptRecordsWithUser`（`TaskAcceptRecords[]`，`getNewTask` 已返回）。
- 列：任务ID（`taskId`）｜接单状态（`status` 后端中文 webValue：待处理/已接收/未选中/已取消）｜接单时间（`acceptTime`，`dateTime` 过滤）｜留言（`str`）。
- 取前 5 条；空态 `el-empty`「暂无接单记录」。
- **数据限制**：原始接单记录只含 `taskId`，不含任务内容。本次不显示任务内容；如需内容，后续改用 `GET /user/accept/page`（联表带 description/location）数据源，多一次请求，本次不做。

## 数据流

`mounted` → `initUserType()`（读 `localStorage.TaskUser.userId` → `this.currentUserId`）→ `loadData()` → `getData(currentUserId)` → `applyQuickData()` 将 `tasksWithUser`/`taskAcceptRecordsWithUser` 挂到 `data`（新增两个响应式字段 `relatedTasks`/`acceptRecords`）→ 模板渲染两卡。

## 验收

- 不同身份登录首页：publisher / admin / L0 各自「与我相关」「接单记录」显示**本人**数据（F-01 修复生效）。
- 无数据身份显示 `el-empty` 空态，无假数据。
- `npm run build` 通过。
- 回归：TC-032（后端 API）不受影响。

## 范围外

- 不改后端 `getNewTask` / `/stats` 契约。
- 接单记录不显示任务内容（数据源限制，留后续联表数据源）。
