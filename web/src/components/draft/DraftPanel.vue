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
