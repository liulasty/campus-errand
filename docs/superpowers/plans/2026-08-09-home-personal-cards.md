# 首页新增「与我相关的委托」「我的接单记录」卡片实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在首页 Home.vue 底部新增两张个人数据卡（「与我相关的委托」用 `tasksWithUser`、「我的接单记录」用 `taskAcceptRecordsWithUser`），纯前端改动，数据已由 `getData(currentUserId)` 按登录用户返回。

**Architecture:** 单文件改动 `web/src/views/dashboard/Home.vue`：`data()` 增加两个响应式字段 → `applyQuickData` 从 `getNewTask` 响应填充（前 5 条）→ 模板 Row3 下方新增 Row4 双卡渲染。不触碰后端与 API 契约。

**Tech Stack:** Vue 2 + Element UI 2.x（`el-table`/`el-empty`/`el-tag`）、全局 `| dateTime` 过滤器（已注册，空值容错）。

**验证说明：** 本仓库无前端测试框架（Vue CLI，无 jest/vitest），每个任务的「测试」为 `npm run build` 编译通过 + 末尾手动验收清单（浏览器以不同身份登录核对）。

---

### Task 1: data() 增加两个响应式字段

**Files:**
- Modify: `web/src/views/dashboard/Home.vue:155-156`（`hotCategories: {},` 与 `stats: null,` 之间）

- [ ] **Step 1: 在 data() 中新增两个字段**

在 `web/src/views/dashboard/Home.vue` 的 `data()` 返回对象中，于 `stats: null,` 之后追加两行：

```js
                stats: null,
                relatedTasks: [],
                acceptRecords: [],
```

修改后该段应为：

```js
                latestTasks: [],
                hotCategories: {},
                stats: null,
                relatedTasks: [],
                acceptRecords: [],
                metricCards: [
```

- [ ] **Step 2: 构建校验**

Run: `cd web && npm run build`
Expected: `DONE  Build complete.`（只有既有 bundle 体积警告）

- [ ] **Step 3: 提交（由用户统一提交，执行时跳过）**

```bash
git add web/src/views/dashboard/Home.vue
git commit -m "feat: 首页新增与我相关/接单记录卡片（data 字段）"
```

---

### Task 2: applyQuickData 填充两字段

**Files:**
- Modify: `web/src/views/dashboard/Home.vue`（`applyQuickData` 方法内，`this.hotCategories = data.hotTaskCategory || {};` 之后）

- [ ] **Step 1: 填充两个字段**

在 `applyQuickData` 中 `this.hotCategories = data.hotTaskCategory || {};` 后追加两行（与 `latestTasks` 一致取前 5 条）：

```js
                this.hotCategories = data.hotTaskCategory || {};
                // 个人相关卡片：与本人关联的委托 / 本人接单记录（前 5 条）
                this.relatedTasks = (data.tasksWithUser || []).slice(0, 5);
                this.acceptRecords = (data.taskAcceptRecordsWithUser || []).slice(0, 5);
```

- [ ] **Step 2: 构建校验**

Run: `cd web && npm run build`
Expected: `DONE  Build complete.`

- [ ] **Step 3: 提交（由用户统一提交，执行时跳过）**

```bash
git add web/src/views/dashboard/Home.vue
git commit -m "feat: 首页新增与我相关/接单记录卡片（数据填充）"
```

---

### Task 3: 模板新增 Row4 双卡

**Files:**
- Modify: `web/src/views/dashboard/Home.vue:126`（Row3 的 `</el-row>` 与 `</div>` 之间）

- [ ] **Step 1: 在 Row3 后新增 Row4 模板块**

在模板中 Row3 结束的 `</el-row>`（紧接 `</div>` 前）与 `</div>` 之间插入：

```html
        <!-- Row4: 个人相关卡片 -->
        <el-row :gutter="20" class="dashboard-row">
            <el-col :xs="24" :md="12">
                <div class="dashboard-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-user"></i> 与我相关的委托</span>
                        <el-button type="text" @click="$router.push(taskMorePath)">更多</el-button>
                    </div>
                    <el-table v-if="relatedTasks.length" :data="relatedTasks" size="small">
                        <el-table-column prop="description" label="内容" show-overflow-tooltip />
                        <el-table-column prop="status" label="状态" width="110">
                            <template slot-scope="scope">
                                <el-tag size="mini" type="info">{{ scope.row.status }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="startTime" label="发布时间" width="150">
                            <template slot-scope="scope">
                                <span class="dashboard-time">{{ scope.row.startTime | dateTime }}</span>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-else description="暂无相关委托" />
                </div>
            </el-col>
            <el-col :xs="24" :md="12">
                <div class="dashboard-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-document"></i> 我的接单记录</span>
                    </div>
                    <el-table v-if="acceptRecords.length" :data="acceptRecords" size="small">
                        <el-table-column prop="taskId" label="任务ID" width="80" />
                        <el-table-column prop="status" label="接单状态" width="100">
                            <template slot-scope="scope">
                                <el-tag size="mini" type="info">{{ scope.row.status }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="acceptTime" label="接单时间" width="150">
                            <template slot-scope="scope">
                                <span class="dashboard-time">{{ scope.row.acceptTime | dateTime }}</span>
                            </template>
                        </el-table-column>
                        <el-table-column prop="str" label="留言" show-overflow-tooltip />
                    </el-table>
                    <el-empty v-else description="暂无接单记录" />
                </div>
            </el-col>
        </el-row>
```

- [ ] **Step 2: 构建校验**

Run: `cd web && npm run build`
Expected: `DONE  Build complete.`

- [ ] **Step 3: 提交（由用户统一提交，执行时跳过）**

```bash
git add web/src/views/dashboard/Home.vue
git commit -m "feat: 首页新增与我相关/接单记录卡片（模板）"
```

---

### Task 4: 构建 + 手动验收清单

**Files:** 无代码改动，纯验证。

- [ ] **Step 1: 全量构建**

Run: `cd web && npm run build`
Expected: `DONE  Build complete.` 无新增报错。

- [ ] **Step 2: 手动验收（由用户用 `npm run serve` 在浏览器核对）**

| # | 操作 | 预期 |
|---|---|---|
| 1 | 以 publisher 登录首页 | Row4 出现「与我相关的委托」「我的接单记录」两卡；内容为本人的任务/接单记录（非 userId=2 数据） |
| 2 | 以 admin 登录首页 | 两卡显示 admin 本人相关数据；「更多」跳 `/publishedList` |
| 3 | 以 L0 未实名账号登录首页 | 两卡正常显示（接口无 L1 门禁）或空态「暂无相关委托/暂无接单记录」 |
| 4 | 无相关数据时 | `el-empty` 空态，无假数据/报错 |
| 5 | 最新委托/公告/柱状图/饼图/排行 | 不受本次改动影响，仍正常 |
