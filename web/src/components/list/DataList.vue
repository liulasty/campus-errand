<template>
    <div class="dl-list" v-loading="loading" element-loading-text="载入中…"
        element-loading-background="rgba(246,243,236,0.72)">
        <template v-if="data.length">
            <!-- ===== table 模式：统一 el-table ===== -->
            <el-table v-if="mode === 'table'" ref="innerTable" :data="data" border stripe style="width: 100%"
                :empty-text="emptyText" :row-key="rowKey || undefined"
                @selection-change="onSelectionChange">
                <!-- selection 列独立渲染：不能带任何自定义 slot，否则会覆盖 Element 内置复选框 -->
                <el-table-column v-if="selectionCol" type="selection" :width="selectionCol.width || 48"
                    :align="selectionCol.align || 'center'" />
                <el-table-column v-for="col in dataCols" :key="col.field || col.label || col.type"
                    :label="col.label"
                    :prop="col.type === 'operate' ? undefined : col.field"
                    :width="col.width" :min-width="col.minWidth" :align="col.align || 'center'"
                    :filters="col.filters" :filter-method="col.filterMethod"
                    :show-overflow-tooltip="col.type !== 'operate' && col.type !== 'badge' && col.type !== 'tag'">
                    <template slot-scope="scope">
                        <template v-if="hasCellSlot(col)">
                            <slot :name="cellSlotName(col)" :row="scope.row" :value="rowValue(scope.row, col)"
                                :col="col"></slot>
                        </template>
                        <template v-else-if="col.type === 'money'">
                            <span class="dl-money">{{ formatMoney(rowValue(scope.row, col)) }}</span>
                        </template>
                        <template v-else-if="col.type === 'date'">
                            <span class="dl-date">{{ formatDate(rowValue(scope.row, col)) }}</span>
                        </template>
                        <template v-else-if="col.type === 'badge' || col.type === 'tag'">
                            <span class="dl-badge" :class="badgeTone(rowValue(scope.row, col), col)">{{
                                badgeText(rowValue(scope.row, col), col) }}</span>
                        </template>
                        <template v-else-if="col.type === 'operate'">
                            <el-button v-for="act in col.actions" :key="act.key" size="mini"
                                :type="btnType(act.tone)" :icon="act.icon" plain
                                @click="emitAction(act.key, scope.row)">{{ actionLabel(act, scope.row) }}</el-button>
                        </template>
                        <template v-else>
                            <span :class="{ 'dl-clamp': col.lineClamp }"
                                :style="col.lineClamp ? { '-webkit-line-clamp': col.lineClamp } : {}">{{ textValue(scope.row, col) }}</span>
                        </template>
                    </template>
                </el-table-column>
            </el-table>

            <!-- ===== card 模式：档案风卡片网格 ===== -->
            <div v-else-if="mode === 'card'" class="dl-cards">
                <article v-for="(row, index) in data" :key="cardKey(row, index)" class="dl-card"
                    :style="{ animationDelay: (index * 60) + 'ms' }">
                    <div class="dl-card__head">
                        <span class="dl-card__badges">
                            <span v-for="col in badgeCols" :key="col.field" class="dl-badge"
                                :class="badgeTone(rowValue(row, col), col)">{{ badgeText(rowValue(row, col), col) }}</span>
                        </span>
                        <span v-if="titleCol" class="dl-card__no">NO.{{ rowValue(row, titleCol) }}</span>
                    </div>
                    <template v-if="titleCol">
                        <h3 class="dl-card__title">{{ titleText(row) }}</h3>
                    </template>
                    <div class="dl-card__meta">
                        <span v-for="col in fieldCols" :key="col.field" class="dl-card__meta-item">
                            <em>{{ col.label }}</em>
                            <span v-if="col.type === 'money'">{{ formatMoney(rowValue(row, col)) }}</span>
                            <span v-else-if="col.type === 'date'">{{ formatDate(rowValue(row, col)) }}</span>
                            <span v-else>{{ textValue(row, col) }}</span>
                        </span>
                    </div>
                    <div v-if="actionCol" class="dl-card__foot">
                        <span class="dl-card__spacer"></span>
                        <button v-for="act in actionCol.actions" :key="act.key" type="button"
                            class="dl-card__action" :class="'dl-card__action--' + act.tone"
                            @click="emitAction(act.key, row)">{{ actionLabel(act, row) }}</button>
                    </div>
                </article>
            </div>
        </template>

        <div v-else-if="!loading" class="dl-list__empty">
            <span class="dl-list__empty-mark">∅</span>
            <p>{{ emptyText }}</p>
        </div>
    </div>
</template>

<script>
    import { formatDateTime } from '@/utils/dateFormat'

    /**
     * DataList：配置驱动列表内核
     * 一份 ListColumn 配置，支持 mode="table"（统一 el-table）与 mode="card"（档案风卡片网格）。
     * 内置格式化：money / date / badge / operate；操作通过 @action="{ key, row }" 回调给业务页。
     *
     * ListColumn:
     *  label     列/卡片标签
     *  field     绑定字段
     *  type      'text' | 'badge' | 'tag' | 'money' | 'date' | 'operate'（默认 text）
     *  width/minWidth/align   table 模式布局
     *  title     card 模式：作为卡片大标题字段（默认取 NO.）
     *  lineClamp card/table 文本省略行数（card 标题下附带一行 lead 时用）
     *  badgeMap  { value: { text, tone } }，tone: default|success|warning|danger|info
     *  actions   [{ key, label, tone, icon }]，tone: primary|success|warning|danger|ghost
     *  emptyText 空值占位（默认 '—'）
     */
    export default {
        name: 'DataList',
        props: {
            data: { type: Array, default: () => [] },
            loading: { type: Boolean, default: false },
            mode: { type: String, default: 'table' },
            config: { type: Array, required: true },
            rowKey: { type: String, default: null },
            emptyText: { type: String, default: '暂无数据' }
        },
        computed: {
            selectionCol() {
                return this.config.find(c => c.type === 'selection') || null;
            },
            dataCols() {
                return this.config.filter(c => c.type !== 'selection');
            },
            badgeCols() {
                return this.config.filter(c => c.type === 'badge' || c.type === 'tag');
            },
            titleCol() {
                return this.config.find(c => c.title) || null;
            },
            fieldCols() {
                return this.config.filter(c => c.type !== 'operate' && c.type !== 'badge' && c.type !== 'tag' && c.type !== 'selection' && c !== this.titleCol);
            },
            actionCol() {
                return this.config.find(c => c.type === 'operate') || null;
            }
        },
        methods: {
            rowValue(row, col) {
                return col && col.field ? row[col.field] : null;
            },
            textValue(row, col) {
                const v = this.rowValue(row, col);
                if (v === null || v === undefined || v === '') return col.emptyText || '—';
                return v;
            },
            titleText(row) {
                const v = this.rowValue(row, this.titleCol);
                return (v === null || v === undefined || v === '') ? '（无标题）' : v;
            },
            formatMoney(v) {
                if (v === null || v === undefined || v === '') return '—';
                return '￥' + Number(v).toFixed(2);
            },
            formatDate(v) {
                return formatDateTime(v, false, '—');
            },
            badgeText(value, col) {
                const item = (col.badgeMap || {})[value];
                if (item) return item.text;
                if (value === null || value === undefined || value === '') return col.emptyText || '—';
                return value;
            },
            badgeTone(value, col) {
                const item = (col.badgeMap || {})[value];
                return 'badge--' + (item ? item.tone : 'info');
            },
            btnType(tone) {
                const map = { primary: 'primary', success: 'success', warning: 'warning', danger: 'danger', ghost: 'info' };
                return map[tone] || 'primary';
            },
            actionLabel(act, row) {
                return typeof act.label === 'function' ? act.label(row) : act.label;
            },
            cardKey(row, index) {
                const key = this.rowKey && row[this.rowKey] != null ? row[this.rowKey] : index;
                return (this.rowKey || 'i') + key;
            },
            emitAction(key, row) {
                this.$emit('action', { key, row });
            },
            // ===== selection / 自定义单元格插槽 =====
            onSelectionChange(val) {
                this.$emit('selection-change', val);
            },
            clearSelection() {
                if (this.$refs.innerTable) {
                    this.$refs.innerTable.clearSelection();
                }
            },
            cellSlotName(col) {
                return col.type === 'operate' ? 'cell-operate' : ('cell-' + col.field);
            },
            hasCellSlot(col) {
                return !!this.$scopedSlots[this.cellSlotName(col)];
            }
        }
    }
</script>

<style lang="less" scoped>
    .dl-list {
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
        font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans SC", sans-serif;
        color: var(--ink);
    }

    /* ===== 通用徽标 ===== */
    .dl-badge {
        display: inline-flex;
        align-items: center;
        font-size: 11px;
        letter-spacing: .08em;
        padding: 2px 10px;
        border-radius: 999px;
        white-space: nowrap;
    }

    .dl-badge.badge--default {
        background: #efe9d7;
        color: #7a6a3a;
    }

    .dl-badge.badge--success {
        background: #dce8e0;
        color: #3e6b50;
    }

    .dl-badge.badge--warning {
        background: #f1e9d3;
        color: #8a6d26;
    }

    .dl-badge.badge--danger {
        background: #f1e3dc;
        color: #a0523b;
    }

    .dl-badge.badge--info {
        background: #ece8de;
        color: #807a68;
    }

    .dl-money {
        font-family: Georgia, "Times New Roman", serif;
        color: var(--brass-deep);
        font-weight: 600;
    }

    .dl-date {
        color: var(--ink-soft);
    }

    .dl-clamp {
        display: -webkit-box;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    /* ===== table 模式：统一样式 ===== */
    .dl-list :deep(.el-table) {
        background: transparent;
        color: var(--ink);
        border-radius: 12px;
    }

    .dl-list :deep(.el-table th.el-table__cell) {
        background: #efe9db;
        color: #4c5340;
        font-weight: 600;
        letter-spacing: .05em;
        border-color: #e6ddc9;
    }

    .dl-list :deep(.el-table td.el-table__cell) {
        border-color: #ece3d1;
    }

    .dl-list :deep(.el-table--border),
    .dl-list :deep(.el-table--group) {
        border-color: #e6ddc9;
    }

    .dl-list :deep(.el-table__row:hover > td.el-table__cell) {
        background: rgba(220, 232, 224, .25) !important;
    }

    .dl-list :deep(.el-table__empty-block) {
        background: transparent;
    }

    .dl-list :deep(.el-button--mini) {
        font-family: inherit;
    }

    /* ===== card 模式 ===== */
    .dl-cards {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 16px;
    }

    .dl-card {
        background: var(--card, #fffcf5);
        border: 1px solid var(--line);
        border-left: 3px solid var(--line-strong);
        border-radius: 12px;
        padding: 16px 18px 12px;
        box-shadow: 0 10px 26px -20px rgba(42, 58, 48, .22);
        animation: dlIn .45s ease both;
        transition: transform .22s, box-shadow .22s, border-color .22s;
    }

    .dl-card:hover {
        transform: translateY(-2px);
        border-color: var(--line-strong);
        box-shadow: 0 8px 20px -10px rgba(42, 58, 48, .28);
    }

    @keyframes dlIn {
        from {
            opacity: 0;
            transform: translateY(12px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    .dl-card__head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 10px;
        margin-bottom: 10px;
    }

    .dl-card__badges {
        display: inline-flex;
        flex-wrap: wrap;
        gap: 6px;
    }

    .dl-card__no {
        font-family: Georgia, "Times New Roman", serif;
        font-size: 12px;
        letter-spacing: .08em;
        color: var(--muted);
        flex-shrink: 0;
    }

    .dl-card__title {
        margin: 0 0 6px;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 17px;
        font-weight: 700;
        line-height: 1.45;
        color: var(--ink);
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .dl-card__meta {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
        gap: 8px 14px;
        padding-top: 10px;
        border-top: 1px dashed var(--line);
    }

    .dl-card__meta-item {
        display: flex;
        flex-direction: column;
        gap: 2px;
        font-size: 12px;
        color: var(--ink-soft);
        min-width: 0;
    }

    .dl-card__meta-item em {
        font-style: normal;
        font-size: 11px;
        letter-spacing: .06em;
        color: var(--muted);
    }

    .dl-card__meta-item > span {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .dl-card__foot {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-top: 12px;
        padding-top: 10px;
        border-top: 1px dashed var(--line);
    }

    .dl-card__spacer {
        flex: 1;
    }

    .dl-card__action {
        appearance: none;
        border: 1px solid transparent;
        background: transparent;
        font-family: inherit;
        font-size: 12px;
        padding: 4px 12px;
        border-radius: 7px;
        cursor: pointer;
        transition: background .2s, border-color .2s, color .2s;
    }

    .dl-card__action--primary {
        color: var(--brass-deep);
    }

    .dl-card__action--primary:hover {
        background: rgba(185, 137, 44, .1);
        border-color: rgba(185, 137, 44, .28);
    }

    .dl-card__action--success {
        color: var(--sage);
    }

    .dl-card__action--success:hover {
        background: rgba(95, 138, 111, .1);
        border-color: rgba(95, 138, 111, .28);
    }

    .dl-card__action--warning {
        color: var(--brass-deep);
    }

    .dl-card__action--warning:hover {
        background: rgba(185, 137, 44, .1);
        border-color: rgba(185, 137, 44, .28);
    }

    .dl-card__action--danger {
        color: var(--terra);
    }

    .dl-card__action--danger:hover {
        background: rgba(180, 84, 58, .09);
        border-color: rgba(180, 84, 58, .28);
    }

    .dl-card__action--ghost {
        color: var(--ink-soft);
    }

    .dl-card__action--ghost:hover {
        background: rgba(255, 252, 245, .7);
        border-color: var(--line);
    }

    /* 空态 */
    .dl-list__empty {
        text-align: center;
        padding: 60px 0;
        color: var(--muted);
    }

    .dl-list__empty-mark {
        display: block;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 46px;
        line-height: 1;
        color: var(--line-strong);
    }

    .dl-list__empty p {
        margin: 14px 0 0;
        font-size: 13px;
        letter-spacing: .1em;
    }
</style>
