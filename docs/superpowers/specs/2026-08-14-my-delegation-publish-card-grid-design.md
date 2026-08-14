# 「我发布的订单」卡片网格布局 设计

> **文档状态**：设计已确认
> **日期**：2026-08-14
> **范围**：`web/src/views/user/MyDelegationPublishList.vue` 列表区重构（本页内联，不抽通用组件）

---

## 一、背景与现状

- 当前列表是标准 `el-table`（委托类型/描述/发布时间/截止时间/地点/状态/查看详情），与其他页面千篇一律。
- 列表数据来自 `publishDelegationList`（`GET /tasks/publisher/tasks`），后端返回 `PageResult<Task>`；前端在 `getList()` 里把 `taskType` id 映射为 `type` 类型名，存入 `viewOnGoingList`。
- 现表格**未利用**的 list 字段：`taskId`、`money`、`createdAt`、`receiverId`；表格里 `type`/`status` 展示简单，无层级。

## 二、设计决策

1. **卡片网格布局**：响应式网格，桌面端一行 3-4 张委托卡片，拉开与普通表格的差距。
2. **仅本页内联实现**：不抽通用组件，样式写在页面 `<style scoped>`；后续推广到其他列表页时再抽取。
3. **卡片只留「查看详情」入口**：操作（取消发布/确认完成/回退草稿/删除等）保留在现有详情弹窗——「确认完成」需要填评分+评价，不适合放卡片。
4. **搜索表单、详情弹窗、分页逻辑、后端接口一律不动**。

## 三、卡片结构（用全 list 字段）

```
┌─────────────────────────────┐
│ ▓ 状态色带 + 状态标签              │  ← status
│ 委托类型名 · 金额(面议)   #任务编号    │  ← type / money / taskId
│ 委托内容描述（最多2行，超出省略）      │  ← description
│ 📍地点   🕐发布时间   ⏰截止时间     │  ← location / startTime / endTime
│ 创建时间 · 承接人                     │  ← createdAt / receiverId
│                 [查看详情]           │
└─────────────────────────────┘
```

字段映射：

| 字段 | 展示 |
|---|---|
| `status` | 顶部状态色带 + 标签，配色复用现有 `statusTagType`（发布中=绿/已接收=橙/已完成=绿/已过期=灰/已取消=红/未完成=红/草稿=灰） |
| `type` | 类型名（`getList` 已映射），卡片头部主标题 |
| `money` | 金额；`null` 显示「面议」 |
| `taskId` | 卡片头部 `#任务编号` |
| `description` | 委托内容，`-webkit-line-clamp: 2` 两行截断 |
| `location` | 📍 地点 |
| `startTime` / `endTime` | 🕐 发布时间 / ⏰ 截止时间，`| dateTime` 过滤 |
| `createdAt` | 创建时间，辅助信息 |
| `receiverId` | 有值显示「已由 #xx 承接」，否则「待接单」 |
| 查看详情 | 按钮打开现有 `open` 详情弹窗 |

## 四、样式与交互

- 网格：`display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px;`
- 卡片：圆角、浅背景、细边框；hover 抬升阴影 + 边框高亮；状态色带用状态色横向渐变。
- 状态色映射：新增 `statusColor(status)` 方法，与 `statusTagType` 共用一套状态色。
- 加载：沿用 `v-loading="loading"`；空列表：统一空态（图标 + 文案）。
- 分页：保留现有 `el-pagination`。

## 五、不改动（Out of Scope）

- 搜索表单、详情弹窗、分页逻辑、后端接口、状态操作逻辑、`getList` 数据映射。
