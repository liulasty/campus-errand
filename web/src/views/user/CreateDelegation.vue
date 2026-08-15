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
