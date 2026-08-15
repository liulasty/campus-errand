# 「学院事务所」卷宗风委托发布工作台 设计

- 日期：2026-08-15
- 页面：`web/src/views/user/CreateDelegation.vue`（现为「发布委托」页，左表单右草稿两栏）
- 范围：**核心工作台 v1**（不含 30s 自动保存、批量操作/复制/排序等可选进阶项）

## 一、定位

把「填写表单页」升级为「委托全流程工作台」：以草稿列表为核心导航、编辑区为主工作区，「列表联动编辑」替代「弹窗编辑」，保留并强化复古纸质卷宗视觉调性。

## 二、整体布局

三段式工作台（放弃固定比例左右分栏）：

- **顶部页眉（全宽通栏，压缩高度）**：标题 + 全局统计 + 核心入口（刷新）
- **左栏（~38%，独立滚动）**：草稿管理区 = 所有委托的「总目录」
- **右栏（~62%，独立滚动）**：委托编辑/预览主工作区，全程无弹窗打断

## 三、组件架构（方案 B：容器 + 双面板）

| 文件 | 职责 |
| --- | --- |
| `web/src/views/user/CreateDelegation.vue` | 工作台容器：顶栏 + 布局 + 状态编排 + API 协调（重写） |
| `web/src/components/draft/DraftPanel.vue` | 左栏草稿管理区（新建） |
| `web/src/components/draft/EditorPanel.vue` | 右栏编辑主区（新建） |

`web/src/components/Delegation.vue` 删除（仅被 CreateDelegation 引用）。

### 容器状态（CreateDelegation）

- `allTasks`：`getTaskDraftById(userId)` 返回的全部委托（含 DRAFT/AUDITING/PENDING_RELEASE/AUDIT_FAILED）
- `filterStatus`：`''`=全部 | 具体状态（webValue）
- `searchKeyword`：委托内容模糊搜索
- `pageNum / pageSize`：草稿箱分页（每页 5）
- `selectedTask`：当前选中草稿行（null = 新建态）
- `taskTypeOption / typeNameMap`：类目 id → 名称

### 数据流

```
loadDrafts()                    // getTaskDraftById → allTasks
  → filteredTasks（状态+关键词过滤，computed）
  → pagedTasks（slice，computed）
  → statusCounts（各状态计数，computed）
selectTask(row)                 // selectedTask = row，编辑区据此载入
deleteTask(id)                  // deleteTaskDraft → refresh（若删的是选中项则清空选中）
saveDraft(payload)              // 新建→addTaskDraft / 编辑→updateTaskDraft → refresh
submitAudit(taskId)             // submitTaskDraft → refresh
publish({taskId,start,end})     // publishingDelegation → refresh
```

### DraftPanel 契约

- Props：`tasks`(分页后) / `total` / `pageNum` / `pageSize` / `filterStatus` / `statusCounts` / `selectedTaskId` / `typeNameMap`
- Emits：`select` / `delete` / `refresh` / `filter-change` / `search` / `page-change`
- 内部自持：审核失败原因展开（只读 `getReason`）

### EditorPanel 契约

- Props：`task`（选中行或 null）/ `taskTypeOption` / `mode`（`new`|`edit`|`publish`）
- Emits：`save-draft` / `submit-audit` / `publish`
- 内部自持：表单 model、预览抽屉、发布规则抽屉

## 四、左栏 DraftPanel

- **状态标签栏**：全部/草稿/审核中/待发布/未通过，选中黄铜色填充，带数量角标，点击即时过滤
- **关键词搜索框**：按委托内容模糊过滤；**刷新** = 极简图标按钮
- **列表项**（紧凑行式）：
  - 首行：委托类型小字标签（黄铜）+ 状态标签（草稿-灰/审核中-黄/待发布-绿/未通过-红）
  - 中间：委托内容 2 行省略（`-webkit-line-clamp:2`）
  - 底行：金额（黄铜 `￥`）+ 创建时间
- **交互**：
  - 选中态：左侧 3px 黄铜竖条 + 背景轻微加深
  - Hover：整体左移 2px + 背景提亮 + 右侧浮现删除图标
  - 未通过项：点状态标签 → 下方内联展开审核原因
- **分页器固定在左栏底部**（总数 + 上下页 + 页码），列表区独立滚动

## 五、右栏 EditorPanel

- **顶部操作栏**：左 = 「新建委托」/「编辑草稿·#编号」；右 = 预览、重置（文字按钮，弱化）
- **表单（卷宗式两组）**：
  - 委托基本信息：委托类型下拉（原分类 Tab 移除，改为下拉补齐）、委托地点 + 委托金额两列并排（金额 `¥` 前缀，el-input-number）
  - 委托详情：委托内容 10 行 textarea + 右下角字数统计（maxlength 200）；输入框微内凹、聚焦黄铜柔光描边
- **底部操作区**（右对齐）：提交审核（深色主按钮，视觉最重）、保存草稿（次一级描边按钮）
- **待发布态**：编辑区底部自动展开「发布时间 / 截止时间」设置栏 + 确认发布（取消独立发布弹窗）
- **新建态**：表单默认空白；保存草稿 / 提交审核后左栏即时更新

## 六、弹窗 → 抽屉/内联

| 原弹窗 | 新形式 |
| --- | --- |
| 发布规则 | 右侧滑出抽屉（半屏宽，静态内容） |
| 委托预览 | 右侧滑出抽屉（实时同步表单，预览与创建解耦） |
| 审核失败原因 | 列表项内联展开（无需弹窗） |
| 发布设置 | 待发布态编辑区底部展开时间栏 |
| 删除确认 | 气泡确认框（列表项原地弹出） |

## 七、视觉与质感

- **背景**：米纸底色 + 极细微颗粒纹理 + 顶部/边缘渐变光影
- **三级阴影**：左栏列表（低，极淡）→ 右栏编辑区（中，柔和投影）→ 抽屉/气泡（深）
- **色阶**：黄铜、鼠尾草绿、赭石红各含浅/中/深三阶，用于背景/标签/文字
- **字体**：标题体系衬线（Georgia / 思源宋体），层级 32/16/12px；正文与输入无衬线；数字统一衬线
- **动效**：切换草稿右栏淡入淡出；筛选切换列表错落淡入；主按钮 hover 上浮加深阴影、点击按压缩放

## 八、交互流程

- **新建**：进入页面 → 右栏默认空白新建表单 → 填写 → 保存草稿/提交审核 → 左栏即时新增
- **编辑**：点左栏草稿 → 右栏自动载入全部信息 → 直接修改 → 保存后列表即时更新，全程无弹窗
- **发布**：审核通过 → 草稿变「待发布」→ 编辑区底部展开时间设置 → 确认发布 → 完成
- **校验**：保存/提交前前端校验委托类型、地点、内容非空（后端 TaskDTO 已 `@NotBlank`）

## 九、后端改动（数据契约补齐）

1. **`UserDelegateDraft` 加 `money` 字段**：`getUserDelegateDraft` 用 `BeanUtils.copyProperties(task, vo)`，Task 已有 money，加字段即自动带出 → 列表可显示金额
2. **`TaskDraftDTO` 加 `money` + 后端 `updateTask` 带上 money**：否则编辑草稿时修改金额不生效（当前 updateTask 构造 Task 不含 money）

## 十、取舍与约束

- 列表「最后更新时间」用 `createTime` 展示（task 表无 `updatedAt` 列）
- 创建/更新草稿的 `type` 传**类目名称**（后端 `getTaskCategoryByCategoryName` 契约；前端在下拉选中 id 后反向映射为名称提交）
- 创建用 `content` 字段、更新用 `description` 字段（既有 API 差异，前端表单统一为 description，提交时分别映射）
- `updateTask` 后端固定置 `status=DRAFT`：编辑被驳回草稿后需重新提交审核（既有行为，保留）
- 延后项：30s 自动保存、批量操作（多选删除/提交）、草稿复制、草稿排序

## 十一、API 清单

| 用途 | 接口 | 说明 |
| --- | --- | --- |
| 类目 | `getTaskCategoriesUser` | id/名称 → taskTypeOption |
| 草稿列表 | `getTaskDraftById(userId)` | 返回 4 状态委托，无分页（前端分页） |
| 新建草稿 | `addTaskDraft(TaskDTO)` | content/type(名称)/location/money/ownerId |
| 更新草稿 | `updateTaskDraft(TaskDraftDTO)` | taskId/type(名称)/location/description/money/createdAt |
| 提交审核 | `submitTaskDraft(id)` | DRAFT/AUDIT_FAILED → AUDITING |
| 删除草稿 | `deleteTaskDraft(id)` | |
| 发布信息 | `confirmTask(id)` | PENDING_RELEASE 校验，返回 Task |
| 发布 | `publishingDelegation({id,start,end})` | PENDING_RELEASE → ONGOING |
| 驳回原因 | `getReason(id)` | AUDIT_FAILED 原因 |
