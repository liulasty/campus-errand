# 「我发布的订单」卡片网格 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `web/src/views/user/MyDelegationPublishList.vue` 的列表从普通表格重构为响应式卡片网格，并利用上 list 返回的全部字段（taskId/money/createdAt/receiverId 等）。

**Architecture:** 纯前端单文件改动。模板把 `<el-table>` 换成 `.ce-card-grid` 卡片网格；script 新增 `statusColor`/`statusTagTypeBy` 两个状态映射方法（`statusTagType` computed 改为委托前者）；`<style scoped>` 追加卡片网格样式。搜索表单、详情弹窗、分页、后端接口均不动。

**Tech Stack:** Vue 2.7 / Element UI / SCSS-less 原生 CSS（`<style lang="css" scoped>`）。

**验证方式：** 本前端无单测框架（package.json 无 test/jest/vitest），验证靠 `npm run serve` dev server 热更新无编译错误 + 浏览器人工检查。

**设计依据：** `docs/superpowers/specs/2026-08-14-my-delegation-publish-card-grid-design.md`（已提交，commit `50c1b9e`）。

---

## 文件结构

- Modify: `web/src/views/user/MyDelegationPublishList.vue`
  - Template L43-60：`<el-table>` → 卡片网格
  - Script methods（`statusTagType` computed 附近）：新增 `statusColor`、`statusTagTypeBy`
  - `<style scoped>`（L620 起，追加在块末尾）：卡片网格 CSS

数据流不变：`getList()` 已把 `taskType` 映射为 `type`，`viewOnGoingList` 里每条是 Task + `type`；`status` 为中文 webValue（与 `TASK_STATUS` 常量直接匹配）；`moneyText`/`dateTime` 均已存在可复用。

---

### Task 1: script 新增状态映射方法 + 重构 computed

**Files:**
- Modify: `web/src/views/user/MyDelegationPublishList.vue:394-407`（computed `statusTagType`）、methods 区

- [ ] **Step 1: 重构 computed `statusTagType` 为委托新方法**

把现有 computed（L394-407）：

```js
        computed: {
            statusTagType() {
                const map = {
                    [TASK_STATUS.ONGOING]: 'success',
                    [TASK_STATUS.ACCEPTED]: 'warning',
                    [TASK_STATUS.COMPLETED]: 'success',
                    [TASK_STATUS.EXPIRED]: 'info',
                    [TASK_STATUS.CANCELLED]: 'danger',
                    [TASK_STATUS.UNFINISHED]: 'danger',
                    [TASK_STATUS.DRAFT]: 'info'
                }
                return map[this.form.task.status] || 'info'
            }
        },
```

改为委托：

```js
        computed: {
            statusTagType() {
                return this.statusTagTypeBy(this.form.task.status)
            }
        },
```

- [ ] **Step 2: 在 methods 内新增 `statusColor` 与 `statusTagTypeBy`**

在 `handleType()` 方法（L410）之前插入：

```js
            /** 卡片状态色带（按状态取渐变背景） */
            statusColor(status) {
                const map = {
                    [TASK_STATUS.ONGOING]: 'linear-gradient(135deg, #67c23a, #a0d911)',
                    [TASK_STATUS.ACCEPTED]: 'linear-gradient(135deg, #e6a23c, #f5c97b)',
                    [TASK_STATUS.COMPLETED]: 'linear-gradient(135deg, #67c23a, #a0d911)',
                    [TASK_STATUS.EXPIRED]: 'linear-gradient(135deg, #909399, #b1b3b8)',
                    [TASK_STATUS.CANCELLED]: 'linear-gradient(135deg, #f56c6c, #f78989)',
                    [TASK_STATUS.UNFINISHED]: 'linear-gradient(135deg, #f56c6c, #f78989)',
                    [TASK_STATUS.DRAFT]: 'linear-gradient(135deg, #909399, #b1b3b8)'
                }
                return map[status] || 'linear-gradient(135deg, #909399, #b1b3b8)'
            },
            /** 卡片状态标签类型 */
            statusTagTypeBy(status) {
                const map = {
                    [TASK_STATUS.ONGOING]: 'success',
                    [TASK_STATUS.ACCEPTED]: 'warning',
                    [TASK_STATUS.COMPLETED]: 'success',
                    [TASK_STATUS.EXPIRED]: 'info',
                    [TASK_STATUS.CANCELLED]: 'danger',
                    [TASK_STATUS.UNFINISHED]: 'danger',
                    [TASK_STATUS.DRAFT]: 'info'
                }
                return map[status] || 'info'
            },
```

- [ ] **Step 3: 校验脚本无语法错误**

Run: 观察 dev server 终端（任务 bp88lgy2m）`Build finished` 无 `ERROR`/`Failed to compile`。
Expected: 编译通过。

- [ ] **Step 4: Commit**

```bash
git add web/src/views/user/MyDelegationPublishList.vue
git commit -m "refactor: 我发布的订单卡片网格 — 新增状态色映射方法（statusColor/statusTagTypeBy）"
```

---

### Task 2: 模板把 `<el-table>` 换成卡片网格

**Files:**
- Modify: `web/src/views/user/MyDelegationPublishList.vue:43-60`（`<el-table>` 整块）

- [ ] **Step 1: 替换列表模板**

把 L43-60 的 `<el-table v-loading="loading" ...>...</el-table>` 整块替换为：

```html
        <!-- 委托卡片网格 -->
        <div v-loading="loading" class="ce-card-grid">
            <div v-for="item in viewOnGoingList" :key="item.taskId" class="ce-deleg-card">
                <div class="ce-card-band" :style="{ background: statusColor(item.status) }"></div>
                <div class="ce-card-head">
                    <div class="ce-card-title">
                        <span class="ce-card-type">{{ item.type }}</span>
                        <span class="ce-card-money" :class="{ 'is-negotiable': item.money == null }">{{ moneyText(item.money) }}</span>
                    </div>
                    <span class="ce-card-no">#{{ item.taskId }}</span>
                </div>
                <div class="ce-card-desc">{{ item.description }}</div>
                <div class="ce-card-meta">
                    <span class="ce-card-meta-item"><i class="el-icon-location-outline"></i>{{ item.location }}</span>
                    <span class="ce-card-meta-item"><i class="el-icon-time"></i>发布 {{ item.startTime | dateTime }}</span>
                    <span class="ce-card-meta-item"><i class="el-icon-finished"></i>截止 {{ item.endTime | dateTime }}</span>
                </div>
                <div class="ce-card-foot">
                    <div class="ce-card-sub">
                        <span class="ce-card-sub-item"><i class="el-icon-date"></i>创建 {{ item.createdAt | dateTime }}</span>
                        <span v-if="item.receiverId" class="ce-card-acceptor"><i class="el-icon-user"></i>由 #{{ item.receiverId }} 承接</span>
                        <span v-else class="ce-card-acceptor is-pending">待接单</span>
                    </div>
                    <div class="ce-card-actions">
                        <el-tag size="mini" :type="statusTagTypeBy(item.status)" effect="dark">{{ item.status }}</el-tag>
                        <el-button size="mini" type="primary" icon="el-icon-view" @click="handleView(item)">查看详情</el-button>
                    </div>
                </div>
            </div>
            <el-empty v-if="!viewOnGoingList.length && !loading" description="暂无委托"></el-empty>
        </div>
```

> 注意：`el-empty` 与卡片并排时同属 `.ce-card-grid` 网格，`el-empty` 会是网格的一个 item；为让其占满整行，Task 3 会给 `.ce-card-grid` 里的 `el-empty` 设置 `grid-column: 1 / -1`。`v-loading` 移到 `.ce-card-grid` 上，空列表判断用 `!viewOnGoingList.length && !loading`。

- [ ] **Step 2: 校验模板编译**

Run: 观察 dev server 终端 `Build finished` 无 `ERROR`。
Expected: 编译通过，页面显示卡片网格（样式未加前布局简陋属正常，Task 3 补齐）。

- [ ] **Step 3: Commit**

```bash
git add web/src/views/user/MyDelegationPublishList.vue
git commit -m "feat: 我发布的订单卡片网格 — 模板由表格改为卡片（用全 list 字段）"
```

---

### Task 3: `<style scoped>` 追加卡片网格样式

**Files:**
- Modify: `web/src/views/user/MyDelegationPublishList.vue`（`<style lang="css" scoped>` 块末尾，即 `</style>` 前）

- [ ] **Step 1: 追加样式**

在 `<style lang="css" scoped>` 块的 `</style>` 之前追加：

```css
    .ce-card-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 16px;
        padding: 4px 2px 16px;
    }

    .ce-card-grid .el-empty {
        grid-column: 1 / -1;
    }

    .ce-deleg-card {
        position: relative;
        display: flex;
        flex-direction: column;
        background: #fff;
        border: 1px solid #ebeef5;
        border-radius: 10px;
        overflow: hidden;
        box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
        transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
    }

    .ce-deleg-card:hover {
        box-shadow: 0 6px 20px rgba(0, 21, 41, 0.12);
        transform: translateY(-2px);
        border-color: #c0c4cc;
    }

    .ce-card-band {
        height: 6px;
    }

    .ce-card-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 16px 0;
    }

    .ce-card-title {
        display: flex;
        align-items: baseline;
        gap: 8px;
        min-width: 0;
    }

    .ce-card-type {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
    }

    .ce-card-money {
        font-size: 14px;
        font-weight: 600;
        color: #e6a23c;
    }

    .ce-card-money.is-negotiable {
        color: #909399;
        font-weight: 400;
    }

    .ce-card-no {
        flex-shrink: 0;
        font-size: 12px;
        color: #909399;
    }

    .ce-card-desc {
        margin: 8px 16px 0;
        font-size: 13px;
        color: #606266;
        line-height: 1.6;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
        min-height: 42px;
    }

    .ce-card-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 4px 14px;
        padding: 10px 16px 0;
    }

    .ce-card-meta-item {
        font-size: 12px;
        color: #909399;
        display: inline-flex;
        align-items: center;
        gap: 4px;
    }

    .ce-card-foot {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 8px;
        padding: 12px 16px;
        margin-top: auto;
        border-top: 1px dashed #ebeef5;
    }

    .ce-card-sub {
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;
    }

    .ce-card-sub-item {
        font-size: 12px;
        color: #909399;
        display: inline-flex;
        align-items: center;
        gap: 4px;
    }

    .ce-card-acceptor {
        font-size: 12px;
        color: #67c23a;
        display: inline-flex;
        align-items: center;
        gap: 4px;
    }

    .ce-card-acceptor.is-pending {
        color: #909399;
    }

    .ce-card-actions {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 6px;
        flex-shrink: 0;
    }
```

- [ ] **Step 2: 校验编译 + 浏览器人工检查**

Run: 观察 dev server 终端 `Build finished` 无 `ERROR`。
然后在浏览器打开 `http://localhost:8080/campus_entrustment/myDelegationPublishList`，核对：
- 卡片网格响应式排布（窄屏自动换行）；每张卡片含状态色带+标签、类型/金额/#编号、两行描述、地点/发布时间/截止时间、创建时间/承接人、查看详情按钮
- 空列表显示 `el-empty` 占整行；hover 卡片抬升
- 点「查看详情」仍打开原详情弹窗，弹窗内操作正常

- [ ] **Step 3: Commit**

```bash
git add web/src/views/user/MyDelegationPublishList.vue
git commit -m "style: 我发布的订单卡片网格 — 追加卡片网格样式与空态/悬停效果"
```

---

## Self-Review

- **Spec coverage：** spec 要求卡片网格 ✓（Task 2）、用全字段 taskId/money/createdAt/receiverId ✓（Task 2 模板）、状态色带复用 statusTagType 映射 ✓（Task 1）、本页内联不抽组件 ✓（样式在 scoped）、搜索表单/详情弹窗/分页/后端不动 ✓（未触碰）。
- **Placeholder scan：** 无 TBD/「适当处理」式占位；每步含完整代码。
- **Type consistency：** `statusColor(status)`/`statusTagTypeBy(status)` 在 Task 1 定义、Task 2 模板使用，签名一致；`moneyText`/`dateTime` 复用既有；`TASK_STATUS` 键与状态 webValue 匹配（已在设计阶段核实）。
