# 「学院事务所」卷宗风委托发布工作台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `web/src/views/user/CreateDelegation.vue` 从「左表单右草稿」重构为「学院事务所」卷宗风三段式工作台（容器 + 左栏草稿管理区 + 右栏编辑主区），弹窗改抽屉/内联。

**Architecture:** 方案 B 容器 + 双面板。`CreateDelegation.vue` 重写为工作台容器（顶栏统计 + 状态编排 + API 协调）；新建 `DraftPanel.vue`（左栏草稿目录：状态筛选/搜索/列表/分页/内联原因/删除气泡）；新建 `EditorPanel.vue`（右栏编辑主区：分组表单/发布设置/预览+规则抽屉）；删除 `Delegation.vue`。两处小后端改动补齐数据契约（草稿列表带 money、更新草稿持久化 money）。

**Tech Stack:** Vue 2 + Element UI（el-input/el-select/el-input-number/el-popover/el-pagination/el-drawer）、Less、Spring Boot 2.7.3 / MyBatis-Plus。

**验证方式：** 本仓库前端无单测框架 → 每个前端任务以 `npm --prefix web run build` 通过为门禁；后端 `mvn compile` 通过为门禁。浏览器端到端跑测由用户（Business Validator）执行，本计划不含验收用例预跑。

**参考规格：** `docs/superpowers/specs/2026-08-15-delegation-workbench-design.md`

---

## 文件结构

| 文件 | 动作 | 职责 |
| --- | --- | --- |
| `src/main/java/com/lz/pojo/vo/UserDelegateDraft.java` | 修改 | 加 `money` 字段，草稿列表行带金额 |
| `src/main/java/com/lz/pojo/dto/TaskDraftDTO.java` | 修改 | 加 `money` 字段，更新草稿持久化金额 |
| `src/main/java/com/lz/controller/task/TaskController.java` | 修改 | `updateTask` 构造 Task 带上 money |
| `web/src/components/draft/DraftPanel.vue` | 新建 | 左栏草稿管理区 |
| `web/src/components/draft/EditorPanel.vue` | 新建 | 右栏编辑主区 |
| `web/src/views/user/CreateDelegation.vue` | 重写 | 工作台容器 |
| `web/src/components/Delegation.vue` | 删除 | 被容器取代 |

---

## Task 1: 后端 — UserDelegateDraft 增加 money

**Files:**
- Modify: `src/main/java/com/lz/pojo/vo/UserDelegateDraft.java`

- [ ] **Step 1: 加字段**

在 `UserDelegateDraft.java` 末尾（`private Date createTime;` 之后）加入：

```java
    private java.math.BigDecimal money;
```

`getUserDelegateDraft`（`TaskServiceImpl.java:330`）用 `BeanUtils.copyProperties(task, userDelegateDraft)`，Task 已有 `money`（BigDecimal），同属性名自动拷贝，无需改查询逻辑。

- [ ] **Step 2: 编译验证**

Run: `"D:/soft-tools/apache-maven-3.9.16/bin/mvn.cmd" -q compile`
Expected: EXIT=0（无输出）

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/lz/pojo/vo/UserDelegateDraft.java
git commit -m "feat: 草稿列表 VO 增加 money 字段（卷宗工作台显示金额）"
```

---

## Task 2: 后端 — TaskDraftDTO 与 updateTask 持久化 money

**Files:**
- Modify: `src/main/java/com/lz/pojo/dto/TaskDraftDTO.java`
- Modify: `src/main/java/com/lz/controller/task/TaskController.java:116-121`

- [ ] **Step 1: DTO 加字段**

`TaskDraftDTO.java` 末尾（`private Date createdAt;` 之后）加入：

```java
    private java.math.BigDecimal money;
```

- [ ] **Step 2: updateTask 带上 money**

`TaskController.java` 的 `updateTask` 中，`Task.builder()` 链加一行（在 `.createdAt(taskDTO.getCreatedAt())` 后）：

```java
                    .money(taskDTO.getMoney())
```

最终 builder 变为：

```java
            Task task = Task.builder().taskId(taskDTO.getTaskId())
                    .taskType(delegationCategories.getCategoryId())
                    .description(taskDTO.getDescription())
                    .location(taskDTO.getLocation())
                    .status(TaskStatus.DRAFT)
                    .createdAt(taskDTO.getCreatedAt())
                    .money(taskDTO.getMoney()).build();
```

- [ ] **Step 3: 编译验证**

Run: `"D:/soft-tools/apache-maven-3.9.16/bin/mvn.cmd" -q compile`
Expected: EXIT=0

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/lz/pojo/dto/TaskDraftDTO.java src/main/java/com/lz/controller/task/TaskController.java
git commit -m "feat: 更新草稿持久化 money（卷宗工作台编辑金额生效）"
```

---

## Task 3: 新建 DraftPanel.vue（左栏草稿管理区）

**Files:**
- Create: `web/src/components/draft/DraftPanel.vue`

- [ ] **Step 1: 写组件**

创建 `web/src/components/draft/DraftPanel.vue`，完整内容：

```vue
<template>
    <aside class="dp">
        <nav class="dp__tabs">
            <button v-for="tab in tabs" :key="tab.key" type="button"
                class="dp__tab" :class="{ 'is-active': filterStatus === tab.key }"
                @click="$emit('filter-change', tab.key)">
                {{ tab.label }}
                <em class="dp__tab-count">{{ countOf(tab.key) }}</em>
            </button>
        </nav>

        <div class="dp__tools">
            <el-input v-model="keyword" placeholder="搜索委托内容" clearable size="small"
                prefix-icon="el-icon-search" @input="onSearch" class="dp__search" />
            <button type="button" class="dp__refresh" title="刷新" @click="$emit('refresh')">
                <i class="el-icon-refresh"></i>
            </button>
        </div>

        <div v-loading="loading" class="dp__list">
            <article v-for="row in tasks" :key="row.taskId" class="dp__item"
                :class="{ 'is-selected': row.taskId === selectedTaskId }"
                @click="$emit('select', row)">
                <div class="dp__item-head">
                    <span class="dp__item-type">{{ typeName(row.taskType) }}</span>
                    <span class="dp__item-status" :class="statusClass(row.status)"
                        @click.stop="onStatusClick(row)">{{ row.status }}</span>
                </div>
                <p class="dp__item-desc">{{ row.description }}</p>
                <div class="dp__item-foot">
                    <span class="dp__item-money">￥{{ fmtMoney(row.money) }}</span>
                    <span class="dp__item-time">{{ row.createTime | dateTime }}</span>
                    <el-popover :visible="deleteTarget === row.taskId" placement="top" width="190"
                        popper-class="dp-pop">
                        <p class="dp-pop__msg">确认删除该委托？删除后不可恢复。</p>
                        <div class="dp-pop__btns">
                            <el-button size="mini" @click="deleteTarget = null">取消</el-button>
                            <el-button size="mini" type="danger" @click="onDelete(row)">删除</el-button>
                        </div>
                        <button slot="reference" type="button" class="dp__item-del"
                            title="删除" @click.stop="deleteTarget = row.taskId">
                            <i class="el-icon-delete"></i>
                        </button>
                    </el-popover>
                </div>
                <div v-if="row.status === TASK_STATUS.AUDIT_FAILED && expandedId === row.taskId"
                    class="dp__reason">
                    <template v-if="reason">
                        <div class="dp__reason-row"><em>审核说明</em><span>{{ reason.reviewComment }}</span></div>
                        <div class="dp__reason-row"><em>审核时间</em><span>{{ reason.reviewTime | dateTime }}</span></div>
                    </template>
                    <span v-else class="dp__reason-loading">加载原因…</span>
                </div>
            </article>
            <div v-if="!tasks.length && !loading" class="dp__empty">暂无委托</div>
        </div>

        <div v-if="total > pageSize" class="dp__pager">
            <el-pagination background small layout="total, prev, pager, next"
                :total="total" :page-size="pageSize" :current-page="pageNum"
                @current-change="onPage" />
        </div>
    </aside>
</template>

<script>
    import { getReason } from '@/api/index'
    import { TASK_STATUS } from '@/constants/enums'

    export default {
        name: 'DraftPanel',
        props: {
            tasks: { type: Array, default: () => [] },
            total: { type: Number, default: 0 },
            pageNum: { type: Number, default: 1 },
            pageSize: { type: Number, default: 5 },
            filterStatus: { type: String, default: '' },
            statusCounts: { type: Object, default: () => ({}) },
            selectedTaskId: { type: [Number, String], default: null },
            typeNameMap: { type: Object, default: () => ({}) },
            loading: { type: Boolean, default: false }
        },
        data() {
            return {
                TASK_STATUS,
                keyword: '',
                deleteTarget: null,
                expandedId: null,
                reason: null
            }
        },
        computed: {
            tabs() {
                return [
                    { key: '', label: '全部' },
                    { key: TASK_STATUS.DRAFT, label: '草稿' },
                    { key: TASK_STATUS.AUDITING, label: '审核中' },
                    { key: TASK_STATUS.PENDING_RELEASE, label: '待发布' },
                    { key: TASK_STATUS.AUDIT_FAILED, label: '未通过' }
                ]
            }
        },
        methods: {
            countOf(key) {
                if (key === '') return this.statusCounts.all || this.tasks.length;
                const map = {
                    [TASK_STATUS.DRAFT]: 'draft',
                    [TASK_STATUS.AUDITING]: 'auditing',
                    [TASK_STATUS.PENDING_RELEASE]: 'pending',
                    [TASK_STATUS.AUDIT_FAILED]: 'failed'
                };
                return this.statusCounts[map[key]] || 0;
            },
            typeName(taskType) {
                return this.typeNameMap[taskType] || '未分类';
            },
            fmtMoney(v) {
                if (v === null || v === undefined || v === '') return '0.00';
                return Number(v).toFixed(2);
            },
            statusClass(status) {
                const map = {
                    [TASK_STATUS.DRAFT]: 'st--draft',
                    [TASK_STATUS.AUDITING]: 'st--auditing',
                    [TASK_STATUS.PENDING_RELEASE]: 'st--pending',
                    [TASK_STATUS.AUDIT_FAILED]: 'st--failed'
                };
                return map[status] || '';
            },
            onSearch(value) {
                this.$emit('search', value);
            },
            onPage(page) {
                this.$emit('page-change', page);
            },
            onDelete(row) {
                this.deleteTarget = null;
                this.$emit('delete', row.taskId);
            },
            onStatusClick(row) {
                if (row.status !== TASK_STATUS.AUDIT_FAILED) return;
                if (this.expandedId === row.taskId) {
                    this.expandedId = null;
                    return;
                }
                this.expandedId = row.taskId;
                this.reason = null;
                getReason(row.taskId).then((data) => {
                    if (data.data.code === 1) {
                        this.reason = data.data.data;
                    } else {
                        this.$message.error(data.data.msg || '获取原因失败');
                    }
                }).catch(err => {
                    console.error('获取驳回原因失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                });
            }
        }
    }
</script>

<style lang="less" scoped>
    .dp {
        --ink: #2a3a30;
        --ink-soft: #5f6b62;
        --muted: #9aa198;
        --line: #e6ddc9;
        --line-strong: #d8ccb2;
        --brass: #b9892c;
        --brass-deep: #96701f;
        --sage: #5f8a6f;
        --terra: #b4543a;
        --card: #fffcf5;

        display: flex;
        flex-direction: column;
        height: 100%;
        min-height: 0;
        background: var(--card);
        border: 1px solid var(--line);
        border-radius: 14px;
        box-shadow: 0 8px 22px -18px rgba(42, 58, 48, .3);
        font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans SC", sans-serif;
    }

    .dp__tabs {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
        padding: 14px 14px 10px;
        border-bottom: 1px solid var(--line);
    }

    .dp__tab {
        appearance: none;
        border: 0;
        background: transparent;
        font-family: inherit;
        font-size: 12px;
        letter-spacing: .04em;
        color: var(--ink-soft);
        cursor: pointer;
        padding: 6px 12px;
        border-radius: 999px;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        transition: background .2s, color .2s;
    }

    .dp__tab:hover {
        background: rgba(185, 137, 44, .1);
        color: var(--ink);
    }

    .dp__tab.is-active {
        background: var(--brass);
        color: #f7f3ea;
    }

    .dp__tab-count {
        font-style: normal;
        font-size: 11px;
        font-family: Georgia, "Times New Roman", serif;
        opacity: .85;
    }

    .dp__tools {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 10px 14px;
    }

    .dp__search { flex: 1; }
    .dp__search :deep(.el-input__inner) {
        background: rgba(255, 252, 245, .85);
        border: 1px solid var(--line);
        border-radius: 9px;
        font-family: inherit;
        font-size: 13px;
    }

    .dp__refresh {
        appearance: none;
        border: 1px solid var(--line);
        background: transparent;
        width: 32px;
        height: 32px;
        border-radius: 9px;
        color: var(--ink-soft);
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        transition: border-color .2s, color .2s;
    }
    .dp__refresh:hover { border-color: var(--brass); color: var(--brass-deep); }

    .dp__list {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        padding: 4px 10px 10px;
        display: flex;
        flex-direction: column;
        gap: 8px;
    }

    .dp__item {
        position: relative;
        background: rgba(255, 252, 245, .7);
        border: 1px solid var(--line);
        border-left: 3px solid transparent;
        border-radius: 9px;
        padding: 11px 12px;
        cursor: pointer;
        transition: transform .18s, background .18s, border-color .18s, box-shadow .18s;
    }

    .dp__item:hover {
        transform: translateX(-2px);
        background: #fffcf5;
        border-color: var(--line-strong);
        box-shadow: 0 6px 16px -12px rgba(42, 58, 48, .3);
    }

    .dp__item.is-selected {
        border-left-color: var(--brass);
        background: rgba(185, 137, 44, .07);
    }

    .dp__item-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 6px;
    }

    .dp__item-type {
        font-size: 11px;
        letter-spacing: .08em;
        color: #7a6a3a;
        background: #efe9d7;
        padding: 2px 9px;
        border-radius: 999px;
    }

    .dp__item-status {
        font-size: 11px;
        letter-spacing: .04em;
        padding: 2px 9px;
        border-radius: 999px;
        cursor: pointer;
    }
    .dp__item-status.st--draft { background: #ece8de; color: #807a68; }
    .dp__item-status.st--auditing { background: #f1e9d3; color: #8a6d26; }
    .dp__item-status.st--pending { background: #dce8e0; color: #3e6b50; }
    .dp__item-status.st--failed { background: #f1e3dc; color: #a0523b; }

    .dp__item-desc {
        margin: 0 0 8px;
        font-size: 13px;
        line-height: 1.55;
        color: var(--ink);
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .dp__item-foot {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 12px;
        color: var(--muted);
    }

    .dp__item-money {
        color: var(--brass-deep);
        font-weight: 600;
        font-family: Georgia, "Times New Roman", serif;
    }

    .dp__item-time { margin-right: auto; }

    .dp__item-del {
        appearance: none;
        border: 0;
        background: transparent;
        color: var(--muted);
        cursor: pointer;
        font-size: 14px;
        padding: 4px;
        border-radius: 6px;
        opacity: 0;
        transition: opacity .18s, color .18s, background .18s;
    }
    .dp__item:hover .dp__item-del { opacity: 1; }
    .dp__item-del:hover { color: var(--terra); background: rgba(180, 84, 58, .08); }

    .dp__reason {
        margin-top: 10px;
        padding: 10px 12px;
        background: rgba(180, 84, 58, .06);
        border: 1px solid rgba(180, 84, 58, .18);
        border-radius: 8px;
        font-size: 12px;
        color: var(--ink-soft);
    }
    .dp__reason-row { display: flex; margin-bottom: 6px; }
    .dp__reason-row:last-child { margin-bottom: 0; }
    .dp__reason-row em { font-style: normal; width: 56px; flex-shrink: 0; color: var(--muted); }
    .dp__reason-loading { color: var(--muted); }

    .dp__empty {
        text-align: center;
        padding: 40px 0;
        color: var(--muted);
        font-size: 13px;
        letter-spacing: .1em;
    }

    .dp__pager {
        flex-shrink: 0;
        padding: 12px 14px;
        border-top: 1px solid var(--line);
        display: flex;
        justify-content: center;
    }
</style>

<style lang="less">
    .dp-pop__msg { margin: 0 0 10px; font-size: 13px; color: #2a3a30; }
    .dp-pop__btns { text-align: right; }
</style>
```

- [ ] **Step 2: 构建验证**

Run: `npm --prefix web run build`
Expected: DONE Build complete（组件未被引用，仅验证语法）

- [ ] **Step 3: 提交**

```bash
git add web/src/components/draft/DraftPanel.vue
git commit -m "feat: 卷宗工作台左栏 DraftPanel（状态筛选/搜索/列表/分页/内联原因/删除气泡）"
```

---

## Task 4: 新建 EditorPanel.vue（右栏编辑主区）

**Files:**
- Create: `web/src/components/draft/EditorPanel.vue`

- [ ] **Step 1: 写组件**

创建 `web/src/components/draft/EditorPanel.vue`，完整内容：

```vue
<template>
    <section class="ep">
        <header class="ep__bar">
            <h3 class="ep__bar-title">{{ barTitle }}</h3>
            <div class="ep__bar-actions">
                <button type="button" class="ep__text-btn" @click="rulesVisible = true">
                    <i class="el-icon-warning-outline"></i>发布规则</button>
                <button type="button" class="ep__text-btn" @click="previewVisible = true">
                    <i class="el-icon-view"></i>预览</button>
                <button type="button" class="ep__text-btn" @click="resetForm">
                    <i class="el-icon-refresh-left"></i>重置</button>
            </div>
        </header>

        <div class="ep__scroll">
            <div v-if="mode === 'auditing'" class="ep__notice">
                <i class="el-icon-s-check"></i>
                <p>该委托正在审核中，暂不可编辑。</p>
            </div>

            <template v-else>
                <div class="ep__group">
                    <h4 class="ep__group-title"><i class="el-icon-document"></i>委托基本信息</h4>
                    <div class="ep__field">
                        <label class="ep__label">委托类型</label>
                        <el-select v-model="form.type" placeholder="请选择委托类型" style="width: 100%;">
                            <el-option v-for="opt in taskTypeOption" :key="opt.value" :label="opt.label"
                                :value="opt.value" />
                        </el-select>
                    </div>
                    <div class="ep__row">
                        <div class="ep__field">
                            <label class="ep__label">委托地点</label>
                            <el-select v-model="form.location" placeholder="请选择委托地点" style="width: 100%;">
                                <el-option v-for="opt in locationOptions" :key="opt.value" :label="opt.label"
                                    :value="opt.value" />
                            </el-select>
                        </div>
                        <div class="ep__field">
                            <label class="ep__label">委托金额</label>
                            <el-input-number v-model="form.money" :min="0" :precision="2" :step="1"
                                controls-position="right" style="width: 100%;" />
                        </div>
                    </div>
                </div>

                <div class="ep__group">
                    <h4 class="ep__group-title"><i class="el-icon-edit-outline"></i>委托详情</h4>
                    <div class="ep__field">
                        <label class="ep__label">委托内容</label>
                        <el-input type="textarea" v-model="form.description" :rows="10" maxlength="200"
                            show-word-limit placeholder="请详细描述您的委托内容，例如：具体要求、时间限制、报酬预算等..." />
                    </div>
                </div>
            </template>

            <div v-if="mode === 'publish'" class="ep__publish">
                <h4 class="ep__publish-title"><i class="el-icon-time"></i>发布设置</h4>
                <div class="ep__row">
                    <div class="ep__field">
                        <label class="ep__label">发布时间</label>
                        <el-date-picker v-model="publishForm.startTime" type="datetime"
                            value-format="yyyy年MM月dd日HH:mm:ss" placeholder="请选择委托发布时间" style="width: 100%;" />
                    </div>
                    <div class="ep__field">
                        <label class="ep__label">截止时间</label>
                        <el-date-picker v-model="publishForm.endTime" type="datetime"
                            value-format="yyyy年MM月dd日HH:mm:ss" placeholder="请选择委托截止时间" style="width: 100%;" />
                    </div>
                </div>
            </div>
        </div>

        <footer v-if="mode !== 'auditing'" class="ep__footer">
            <button type="button" class="ep__btn ep__btn--ghost" @click="onSaveDraft">
                <i class="el-icon-document-checked"></i>保存草稿</button>
            <button v-if="mode === 'new' || mode === 'edit'" type="button"
                class="ep__btn ep__btn--primary" @click="onSubmitAudit">
                <i class="el-icon-s-promotion"></i>提交审核</button>
            <button v-else-if="mode === 'publish'" type="button"
                class="ep__btn ep__btn--primary" @click="onPublish">
                <i class="el-icon-s-promotion"></i>确认发布</button>
        </footer>

        <el-drawer title="委托预览" :visible.sync="previewVisible" size="46%" custom-class="ep-drawer">
            <div class="ep-preview">
                <div class="ep-preview__row"><em>委托类型</em><span>{{ typeLabel(form.type) }}</span></div>
                <div class="ep-preview__row"><em>委托地点</em><span>{{ form.location || '未选择' }}</span></div>
                <div class="ep-preview__row"><em>委托金额</em><span>￥{{ fmtMoney(form.money) }}</span></div>
                <div class="ep-preview__row ep-preview__row--block"><em>委托内容</em>
                    <p>{{ form.description || '未填写内容' }}</p></div>
            </div>
        </el-drawer>

        <el-drawer title="发布须知" :visible.sync="rulesVisible" size="46%" custom-class="ep-drawer">
            <div class="ep-rules">
                <div class="ep-rules__item"><i class="el-icon-s-promotion"></i>
                    <p>发布委托信息流程：先创建草稿，再申请发布委托，只有通过审核后，再发布委托。</p></div>
                <div class="ep-rules__item"><i class="el-icon-edit"></i>
                    <p>草稿创建后可以修改，发布后不可修改。</p></div>
                <div class="ep-rules__item"><i class="el-icon-warning"></i>
                    <p>内容合法合规：所有发布的信息必须符合国家法律法规和学校相关规定，不得含有违法、淫秽、暴力、歧视等不良内容。</p></div>
                <div class="ep-rules__item"><i class="el-icon-document-checked"></i>
                    <p>真实准确：发布的信息必须真实准确，不得故意虚假宣传、夸大事实或误导他人。</p></div>
                <div class="ep-rules__item"><i class="el-icon-lock"></i>
                    <p>尊重隐私：严禁发布他人隐私信息，包括但不限于手机号码、学号、家庭住址等个人敏感信息。</p></div>
                <div class="ep-rules__item"><i class="el-icon-shop"></i>
                    <p>适度宣传：允许校园内部组织、社团、团队等发布相关活动、招募信息，但不得进行过度商业宣传。</p></div>
            </div>
        </el-drawer>
    </section>
</template>

<script>
    import { TASK_STATUS } from '@/constants/enums'

    export default {
        name: 'EditorPanel',
        props: {
            task: { type: Object, default: null },
            taskTypeOption: { type: Array, default: () => [] }
        },
        data() {
            return {
                TASK_STATUS,
                previewVisible: false,
                rulesVisible: false,
                locationOptions: [
                    { value: '教学楼', label: '教学楼' },
                    { value: '图书馆', label: '图书馆' },
                    { value: '食堂', label: '食堂' },
                    { value: '运动场', label: '运动场' },
                    { value: '实验室', label: '实验室' },
                    { value: '其他', label: '其他' }
                ],
                form: { type: null, location: '', money: 0, description: '' },
                publishForm: { startTime: null, endTime: null }
            }
        },
        computed: {
            mode() {
                if (!this.task) return 'new';
                if (this.task.status === TASK_STATUS.AUDITING) return 'auditing';
                if (this.task.status === TASK_STATUS.PENDING_RELEASE) return 'publish';
                return 'edit';
            },
            barTitle() {
                if (this.mode === 'new') return '新建委托';
                if (this.mode === 'publish') return `发布委托 · #${this.task.taskId}`;
                if (this.mode === 'auditing') return `审核中 · #${this.task.taskId}`;
                return `编辑草稿 · #${this.task.taskId}`;
            }
        },
        watch: {
            task: {
                immediate: true,
                handler(val) {
                    if (val && val.taskType != null) {
                        this.form = {
                            type: val.taskType,
                            location: val.location || '',
                            money: val.money == null ? 0 : Number(val.money),
                            description: val.description || ''
                        };
                        this.publishForm = { startTime: null, endTime: null };
                    } else {
                        this.form = { type: null, location: '', money: 0, description: '' };
                        this.publishForm = { startTime: null, endTime: null };
                    }
                }
            }
        },
        methods: {
            typeLabel(taskType) {
                if (taskType === null || taskType === undefined) return '未选择';
                const opt = this.taskTypeOption.find(o => o.value === Number(taskType));
                return opt ? opt.label : String(taskType);
            },
            fmtMoney(v) {
                if (v === null || v === undefined || v === '') return '0.00';
                return Number(v).toFixed(2);
            },
            resetForm() {
                this.form = { type: null, location: '', money: 0, description: '' };
                this.publishForm = { startTime: null, endTime: null };
            },
            validateForm() {
                if (!this.form.type) { this.$message.error('请选择委托类型'); return false; }
                if (!this.form.location) { this.$message.error('请选择委托地点'); return false; }
                if (!this.form.description || !this.form.description.trim()) { this.$message.error('请填写委托内容'); return false; }
                return true;
            },
            onSaveDraft() {
                if (!this.validateForm()) return;
                this.$emit('save-draft', {
                    taskId: this.task ? this.task.taskId : null,
                    type: this.form.type,
                    location: this.form.location,
                    money: this.form.money,
                    description: this.form.description
                });
            },
            onSubmitAudit() {
                if (!this.validateForm()) return;
                this.$emit('submit-audit', {
                    taskId: this.task ? this.task.taskId : null,
                    form: {
                        type: this.form.type,
                        location: this.form.location,
                        money: this.form.money,
                        description: this.form.description
                    }
                });
            },
            onPublish() {
                if (!this.publishForm.startTime || !this.publishForm.endTime) {
                    this.$message.error('请选择发布时间与截止时间');
                    return;
                }
                this.$emit('publish', {
                    taskId: this.task.taskId,
                    startTime: this.publishForm.startTime,
                    endTime: this.publishForm.endTime
                });
            }
        }
    }
</script>

<style lang="less" scoped>
    .ep {
        --ink: #2a3a30;
        --ink-soft: #5f6b62;
        --muted: #9aa198;
        --line: #e6ddc9;
        --line-strong: #d8ccb2;
        --brass: #b9892c;
        --brass-deep: #96701f;
        --sage: #5f8a6f;
        --terra: #b4543a;
        --card: #fffcf5;

        display: flex;
        flex-direction: column;
        height: 100%;
        min-height: 0;
        background: var(--card);
        border: 1px solid var(--line);
        border-radius: 14px;
        box-shadow: 0 18px 40px -24px rgba(42, 58, 48, .35);
        font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans SC", sans-serif;
    }

    .ep__bar {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 18px 24px 14px;
        border-bottom: 1px solid var(--line);
    }

    .ep__bar-title {
        margin: 0;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 18px;
        font-weight: 700;
        color: var(--ink);
        letter-spacing: .02em;
    }

    .ep__bar-actions { display: flex; gap: 4px; }

    .ep__text-btn {
        appearance: none;
        border: 0;
        background: transparent;
        font-family: inherit;
        font-size: 12px;
        letter-spacing: .04em;
        color: var(--brass-deep);
        cursor: pointer;
        padding: 5px 10px;
        border-radius: 6px;
        transition: background .2s;
    }
    .ep__text-btn i { margin-right: 4px; }
    .ep__text-btn:hover { background: rgba(185, 137, 44, .1); }

    .ep__scroll {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        padding: 18px 24px;
    }

    .ep__group { margin-bottom: 22px; }

    .ep__group-title {
        margin: 0 0 14px;
        padding-bottom: 10px;
        border-bottom: 1px dashed var(--line);
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 15px;
        font-weight: 700;
        color: var(--ink);
    }
    .ep__group-title i { margin-right: 6px; color: var(--brass); }

    .ep__field { margin-bottom: 16px; }

    .ep__label {
        display: block;
        margin-bottom: 7px;
        font-size: 13px;
        letter-spacing: .04em;
        color: var(--ink-soft);
    }

    .ep__row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

    .ep :deep(.el-input__inner),
    .ep :deep(.el-textarea__inner),
    .ep :deep(.el-input-number) {
        width: 100%;
        background: rgba(255, 252, 245, .85);
        border: 1px solid var(--line);
        border-radius: 9px;
        color: var(--ink);
        font-family: inherit;
        font-size: 13px;
        box-shadow: inset 0 1px 3px rgba(42, 58, 48, .05);
        transition: border-color .2s, box-shadow .2s, background .2s;
    }
    .ep :deep(.el-input__inner:hover),
    .ep :deep(.el-textarea__inner:hover) { border-color: var(--line-strong); }
    .ep :deep(.el-input__inner:focus),
    .ep :deep(.el-textarea__inner:focus) {
        border-color: var(--brass);
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13), inset 0 1px 3px rgba(42, 58, 48, .05);
        background: var(--card);
    }

    .ep__publish {
        margin: 4px 0 18px;
        padding: 16px 18px;
        background: rgba(220, 232, 224, .3);
        border: 1px solid rgba(95, 138, 111, .3);
        border-radius: 12px;
    }

    .ep__publish-title {
        margin: 0 0 14px;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 15px;
        font-weight: 700;
        color: var(--sage);
    }
    .ep__publish-title i { margin-right: 6px; }

    .ep__notice {
        text-align: center;
        padding: 80px 0;
        color: var(--muted);
    }
    .ep__notice i { font-size: 40px; display: block; margin-bottom: 12px; color: var(--brass); }

    .ep__footer {
        flex-shrink: 0;
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        padding: 16px 24px 20px;
        border-top: 1px solid var(--line);
    }

    .ep__btn {
        appearance: none;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 22px;
        border-radius: 9px;
        border: 1px solid transparent;
        font-family: inherit;
        font-size: 13px;
        letter-spacing: .06em;
        cursor: pointer;
        transition: transform .18s, box-shadow .18s, background .18s, border-color .18s, color .18s;
    }
    .ep__btn:active { transform: scale(.97); }
    .ep__btn--primary {
        background: var(--ink);
        color: #f7f3ea;
    }
    .ep__btn--primary:hover {
        background: #33493c;
        transform: translateY(-1px);
        box-shadow: 0 6px 14px -6px rgba(42, 58, 48, .55);
    }
    .ep__btn--ghost {
        background: transparent;
        border-color: var(--line-strong);
        color: var(--ink-soft);
    }
    .ep__btn--ghost:hover {
        border-color: var(--brass);
        color: var(--brass-deep);
        background: rgba(255, 252, 245, .6);
    }
</style>

<style lang="less">
    .ep-drawer { background: #fffcf5; }
    .ep-preview { padding: 6px 24px 24px; }
    .ep-preview__row { display: flex; margin-bottom: 14px; font-size: 13px; }
    .ep-preview__row em { font-style: normal; width: 72px; flex-shrink: 0; color: #9aa198; }
    .ep-preview__row span, .ep-preview__row p { color: #2a3a30; margin: 0; line-height: 1.6; word-break: break-all; }
    .ep-preview__row--block { padding: 12px 14px; background: rgba(255, 252, 245, .8); border: 1px dashed #e6ddc9; border-radius: 8px; }
    .ep-rules { padding: 6px 24px 24px; }
    .ep-rules__item { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 14px; }
    .ep-rules__item i { font-size: 17px; color: #b9892c; margin-top: 2px; flex-shrink: 0; }
    .ep-rules__item p { margin: 0; font-size: 13px; line-height: 1.7; color: #5f6b62; }
</style>
```

- [ ] **Step 2: 构建验证**

Run: `npm --prefix web run build`
Expected: DONE Build complete

- [ ] **Step 3: 提交**

```bash
git add web/src/components/draft/EditorPanel.vue
git commit -m "feat: 卷宗工作台右栏 EditorPanel（分组表单/发布设置/预览+规则抽屉）"
```

---

## Task 5: 重写 CreateDelegation.vue 为工作台容器

**Files:**
- Rewrite: `web/src/views/user/CreateDelegation.vue`

- [ ] **Step 1: 替换文件完整内容**

将 `web/src/views/user/CreateDelegation.vue` 完整替换为：

```vue
<template>
    <div class="wb">
        <header class="wb__header">
            <div class="wb__heading">
                <span class="wb__eyebrow">学院事务所</span>
                <h1 class="wb__title">发布委托</h1>
                <p class="wb__sub">草稿创建 → 审核 → 发布，全流程在此完成</p>
            </div>
            <div class="wb__stats">
                <div class="wb__stat"><strong>{{ allTasks.length }}</strong><em>全部</em></div>
                <div class="wb__stat"><strong>{{ statusCounts.draft }}</strong><em>草稿</em></div>
                <div class="wb__stat"><strong>{{ statusCounts.pending }}</strong><em>待发布</em></div>
                <button type="button" class="wb__refresh" title="刷新" @click="loadDrafts">
                    <i class="el-icon-refresh"></i>
                </button>
            </div>
        </header>

        <div class="wb__body">
            <draft-panel
                :tasks="pagedTasks" :total="filteredTasks.length" :page-num="pageNum" :page-size="pageSize"
                :filter-status="filterStatus" :status-counts="statusCounts"
                :selected-task-id="selectedTask ? selectedTask.taskId : null"
                :type-name-map="typeNameMap" :loading="loading"
                @select="selectTask" @delete="deleteTask" @refresh="loadDrafts"
                @filter-change="onFilter" @search="onSearch" @page-change="onPage" />
            <editor-panel :task="selectedTask" :task-type-option="taskTypeOption"
                @save-draft="saveDraft" @submit-audit="submitAudit" @publish="publish" />
        </div>
    </div>
</template>

<script>
    import DraftPanel from '@/components/draft/DraftPanel'
    import EditorPanel from '@/components/draft/EditorPanel'
    import {
        getTaskDraftById, addTaskDraft, updateTaskDraft, submitTaskDraft,
        deleteTaskDraft, publishingDelegation
    } from '@/api/index'
    import { getTaskCategoriesUser } from '@/api/user'
    import { TASK_STATUS } from '@/constants/enums'
    import { SUCCESS_CODE } from '@/constants/http'

    export default {
        name: 'CreateDelegation',
        components: { DraftPanel, EditorPanel },
        data() {
            return {
                TASK_STATUS,
                loading: false,
                allTasks: [],
                filterStatus: '',
                searchKeyword: '',
                pageNum: 1,
                pageSize: 5,
                selectedTask: null,
                taskTypeOption: []
            }
        },
        computed: {
            typeNameMap() {
                const m = {};
                this.taskTypeOption.forEach(o => { m[o.value] = o.label; });
                return m;
            },
            statusCounts() {
                const c = { all: this.allTasks.length, draft: 0, auditing: 0, pending: 0, failed: 0 };
                this.allTasks.forEach(t => {
                    if (t.status === TASK_STATUS.DRAFT) c.draft++;
                    else if (t.status === TASK_STATUS.AUDITING) c.auditing++;
                    else if (t.status === TASK_STATUS.PENDING_RELEASE) c.pending++;
                    else if (t.status === TASK_STATUS.AUDIT_FAILED) c.failed++;
                });
                return c;
            },
            filteredTasks() {
                let list = this.allTasks;
                if (this.filterStatus) list = list.filter(t => t.status === this.filterStatus);
                const kw = this.searchKeyword && this.searchKeyword.trim();
                if (kw) list = list.filter(t => (t.description || '').toLowerCase().includes(kw.toLowerCase()));
                return list;
            },
            pagedTasks() {
                const start = (this.pageNum - 1) * this.pageSize;
                return this.filteredTasks.slice(start, start + this.pageSize);
            }
        },
        methods: {
            async loadDrafts() {
                this.loading = true;
                try {
                    const data = await getTaskDraftById(this.$store.state.userInfo.userId);
                    if (data.data.code === SUCCESS_CODE) {
                        this.allTasks = data.data.data || [];
                    } else {
                        this.$message.error(data.data.msg || '加载草稿失败');
                    }
                } catch (err) {
                    console.error('加载草稿失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                } finally {
                    this.loading = false;
                }
            },
            async loadCategories() {
                try {
                    const data = await getTaskCategoriesUser();
                    if (data.data.code === SUCCESS_CODE && data.data.data.length > 0) {
                        this.taskTypeOption = data.data.data.map(category => ({
                            label: category.name,
                            value: category.id
                        }));
                    }
                } catch (err) {
                    console.error('获取委托类型失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                }
            },
            selectTask(row) {
                this.selectedTask = row;
            },
            onFilter(status) {
                this.filterStatus = status;
                this.pageNum = 1;
            },
            onSearch(keyword) {
                this.searchKeyword = keyword;
                this.pageNum = 1;
            },
            onPage(page) {
                this.pageNum = page;
            },
            async deleteTask(taskId) {
                try {
                    const data = await deleteTaskDraft(taskId);
                    if (data.data.code === 1) {
                        this.$message.success(data.data.msg || '删除成功');
                        if (this.selectedTask && this.selectedTask.taskId === taskId) {
                            this.selectedTask = null;
                        }
                        this.loadDrafts();
                    } else {
                        this.$message.error(data.data.msg || '删除失败');
                    }
                } catch (err) {
                    console.error('删除草稿失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                }
            },
            async saveDraft(payload) {
                const typeName = this.typeNameMap[payload.type] || payload.type;
                try {
                    let data;
                    if (payload.taskId) {
                        data = await updateTaskDraft({
                            taskId: payload.taskId,
                            type: typeName,
                            location: payload.location,
                            description: payload.description,
                            money: payload.money
                        });
                    } else {
                        data = await addTaskDraft({
                            ownerId: this.$store.state.userInfo.userId,
                            location: payload.location,
                            content: payload.description,
                            type: typeName,
                            money: payload.money
                        });
                    }
                    if (data.data.code === 1) {
                        this.$message.success(data.data.msg || '保存成功');
                        await this.loadDrafts();
                    } else {
                        this.$message.error(data.data.msg || '保存失败');
                    }
                } catch (err) {
                    console.error('保存草稿失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                }
            },
            async submitAudit(payload) {
                try {
                    let taskId = payload.taskId;
                    if (!taskId) {
                        const typeName = this.typeNameMap[payload.form.type] || payload.form.type;
                        const created = await addTaskDraft({
                            ownerId: this.$store.state.userInfo.userId,
                            location: payload.form.location,
                            content: payload.form.description,
                            type: typeName,
                            money: payload.form.money
                        });
                        if (created.data.code !== 1) {
                            this.$message.error(created.data.msg || '创建草稿失败');
                            return;
                        }
                        taskId = created.data.data;
                    }
                    const data = await submitTaskDraft(taskId);
                    if (data.data.code === 1) {
                        this.$message.success(data.data.msg || '已提交审核');
                        this.selectedTask = null;
                        await this.loadDrafts();
                    } else {
                        if (data.data.msg && data.data.msg.indexOf('L1实名认证') !== -1) {
                            this.$confirm('发布委托需完成 L1 实名认证，是否前往认证？', '提示', {
                                confirmButtonText: '去认证', cancelButtonText: '取消', type: 'warning'
                            }).then(() => this.$router.push('/myInfo')).catch(() => {});
                        } else {
                            this.$message.error(data.data.msg || '提交失败');
                        }
                    }
                } catch (err) {
                    console.error('提交审核失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                }
            },
            async publish(payload) {
                try {
                    const data = await publishingDelegation({
                        id: payload.taskId,
                        start: payload.startTime,
                        end: payload.endTime
                    });
                    if (data.data.code === 1) {
                        this.$message.success(data.data.msg || '发布成功');
                        this.selectedTask = null;
                        await this.loadDrafts();
                    } else {
                        this.$message.error(data.data.msg || '发布失败');
                    }
                } catch (err) {
                    console.error('发布失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                }
            }
        },
        mounted() {
            this.loadDrafts();
            this.loadCategories();
        }
    }
</script>

<style lang="less" scoped>
    .wb {
        --paper: #f5f1e8;
        --ink: #2a3a30;
        --ink-soft: #5f6b62;
        --muted: #9aa198;
        --line: #e6ddc9;
        --line-strong: #d8ccb2;
        --brass: #b9892c;
        --brass-deep: #96701f;
        --sage: #5f8a6f;
        --terra: #b4543a;

        position: relative;
        min-height: calc(100vh - 140px);
        padding: 4px;
        border-radius: 14px;
        color: var(--ink);
        font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans SC", sans-serif;
        background:
            radial-gradient(1100px 420px at 90% -6%, rgba(185, 137, 44, 0.13), transparent 60%),
            radial-gradient(900px 380px at -4% 0%, rgba(95, 138, 111, 0.11), transparent 55%),
            var(--paper);

        &::before {
            content: "";
            position: absolute;
            inset: 0;
            z-index: 0;
            pointer-events: none;
            opacity: .5;
            background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.045'/%3E%3C/svg%3E");
        }
    }

    .wb__header {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        padding: 26px 34px 20px;
    }

    .wb__eyebrow {
        display: block;
        margin-bottom: 8px;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 11px;
        letter-spacing: .34em;
        text-transform: uppercase;
        color: var(--brass);
    }

    .wb__title {
        margin: 0;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 32px;
        font-weight: 700;
        line-height: 1.12;
        letter-spacing: .02em;
    }

    .wb__sub {
        margin: 8px 0 0;
        font-size: 13px;
        letter-spacing: .06em;
        color: var(--muted);
    }

    .wb__stats {
        display: flex;
        align-items: center;
        gap: 18px;
        padding-left: 18px;
        border-left: 2px solid var(--brass);
    }

    .wb__stat { text-align: right; }
    .wb__stat strong {
        display: block;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 26px;
        font-weight: 700;
        line-height: 1;
        color: var(--ink);
    }
    .wb__stat em {
        display: block;
        margin-top: 5px;
        font-style: normal;
        font-size: 12px;
        letter-spacing: .16em;
        color: var(--muted);
    }

    .wb__refresh {
        appearance: none;
        border: 1px solid var(--line-strong);
        background: transparent;
        width: 36px;
        height: 36px;
        border-radius: 10px;
        color: var(--ink-soft);
        cursor: pointer;
        font-size: 16px;
        transition: border-color .2s, color .2s, background .2s;
    }
    .wb__refresh:hover { border-color: var(--brass); color: var(--brass-deep); background: rgba(255, 252, 245, .6); }

    .wb__body {
        position: relative;
        z-index: 1;
        display: grid;
        grid-template-columns: 38% 62%;
        gap: 18px;
        padding: 0 34px 26px;
        height: calc(100vh - 210px);
        min-height: 480px;
    }
</style>
```

- [ ] **Step 2: 构建验证**

Run: `npm --prefix web run build`
Expected: DONE Build complete

- [ ] **Step 3: 提交**

```bash
git add web/src/views/user/CreateDelegation.vue
git commit -m "feat: 重写发布委托页为卷宗风工作台容器（顶栏统计+双面板编排）"
```

---

## Task 6: 删除 Delegation.vue 并清理引用

**Files:**
- Delete: `web/src/components/Delegation.vue`

- [ ] **Step 1: 确认无引用**

Run: `grep -rn "Delegation.vue\|components/Delegation" web/src --include=*.vue --include=*.js`
Expected: 无输出（CreateDelegation 已不再 import；若仍有引用需先改）

- [ ] **Step 2: 删除文件**

```bash
git rm web/src/components/Delegation.vue
```

- [ ] **Step 3: 构建验证**

Run: `npm --prefix web run build`
Expected: DONE Build complete

- [ ] **Step 4: 提交**

```bash
git commit -m "refactor: 删除被工作台取代的 Delegation.vue"
```

---

## Task 7: 端到端验证

- [ ] **Step 1: 后端编译**

Run: `"D:/soft-tools/apache-maven-3.9.16/bin/mvn.cmd" -q compile`
Expected: EXIT=0

- [ ] **Step 2: 前端构建**

Run: `npm --prefix web run build`
Expected: DONE Build complete（dist 就绪）

- [ ] **Step 3: 提交剩余改动**

```bash
git status --short
git add -A
git commit -m "feat: 卷宗风委托发布工作台（学院事务所）"
```

---

## Self-Review 记录

- **规格覆盖**：三段式布局(T5 容器 grid 38/62 + 顶栏)✓；左栏状态标签+计数(T3 tabs/countOf)✓；搜索(T3)✓；列表项选中/hover/删除(T3)✓；分页固定底部(T3 dp__pager)✓；原因内联(T3 onStatusClick)✓；右栏分组表单(T4)✓；待发布发布设置(T4 ep__publish)✓；预览/规则抽屉(T4)✓；删除气泡(T3 el-popover)✓；后端 money 两处(T1/T2)✓；Delegation.vue 删除(T6)✓。
- **规格外决策（已入实现）**：无单测框架 → 用 build 门禁；`publishFrom` 相关旧逻辑废弃（无弹窗发布）；AUDITING 态右栏只读提示。
- **已知约束**：`submitAudit` 对无 taskId 的新草稿先 `addTaskDraft` 再 `submitTaskDraft`；`type` 均传类目名称（后端契约）；列表时间用 `createTime`。
