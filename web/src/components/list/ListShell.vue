<template>
    <div class="ls-shell">
        <header class="ls-shell__header">
            <div class="ls-shell__heading">
                <span v-if="eyebrow" class="ls-shell__eyebrow">{{ eyebrow }}</span>
                <h1 class="ls-shell__title">{{ title }}</h1>
                <p v-if="subtitle" class="ls-shell__sub">{{ subtitle }}</p>
            </div>
            <div class="ls-shell__header-side">
                <slot name="header-extra"></slot>
                <div v-if="count != null" class="ls-shell__stamp">
                    <strong class="ls-shell__stamp-num">{{ count }}</strong>
                    <span class="ls-shell__stamp-label">{{ countLabel }}</span>
                </div>
            </div>
        </header>

        <div v-if="$slots.toolbar" class="ls-shell__toolbar">
            <slot name="toolbar"></slot>
        </div>

        <main class="ls-shell__body">
            <slot></slot>
        </main>

        <footer v-if="showPagination" class="ls-shell__footer">
            <el-pagination
                :current-page="page"
                :page-size="pageSize"
                :page-sizes="pageSizes"
                :total="total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="onSizeChange"
                @current-change="onCurrentChange" />
        </footer>
    </div>
</template>

<script>
    /**
     * ListShell：列表页外壳（纯布局）
     * 统一：页头（标题/副标题/计数印章/辅助按钮）、工具栏插槽、纸感背景、分页。
     * 分页使用 .sync 同步 page / page-size，同时透传 size-change / current-change。
     */
    export default {
        name: 'ListShell',
        props: {
            title: { type: String, required: true },
            subtitle: { type: String, default: '' },
            eyebrow: { type: String, default: '' },
            count: { type: Number, default: null },
            countLabel: { type: String, default: '条记录' },
            loading: { type: Boolean, default: false },
            total: { type: Number, default: 0 },
            page: { type: Number, default: 1 },
            pageSize: { type: Number, default: 10 },
            pageSizes: { type: Array, default: () => [5, 10, 15, 20] },
            showPagination: { type: Boolean, default: true }
        },
        methods: {
            onSizeChange(val) {
                this.$emit('update:page-size', val);
                this.$emit('size-change', val);
            },
            onCurrentChange(val) {
                this.$emit('update:page', val);
                this.$emit('current-change', val);
            }
        }
    }
</script>

<style lang="less" scoped>
    .ls-shell {
        --paper: #f5f1e8;
        --card: #fffcf5;
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
        min-height: calc(100vh - 160px);
        padding: 6px 4px 30px;
        border-radius: 14px;
        color: var(--ink);
        font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans SC", sans-serif;
        background:
            radial-gradient(1100px 480px at 88% -8%, rgba(185, 137, 44, 0.14), transparent 60%),
            radial-gradient(900px 420px at -6% 0%, rgba(95, 138, 111, 0.12), transparent 55%),
            var(--paper);

        /* 颗粒层置于内容之下：避免 fixed 弹层(弹窗/抽屉)被 z-index 困住导致整页卡死 */
        &::before {
            content: "";
            position: absolute;
            inset: 0;
            z-index: 0;
            pointer-events: none;
            opacity: .55;
            background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.045'/%3E%3C/svg%3E");
        }
    }

    .ls-shell__header {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        gap: 20px;
        padding: 32px 40px 20px;
    }

    .ls-shell__eyebrow {
        display: block;
        margin-bottom: 10px;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 11px;
        letter-spacing: .34em;
        text-transform: uppercase;
        color: var(--brass);
    }

    .ls-shell__title {
        margin: 0;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 32px;
        font-weight: 700;
        line-height: 1.12;
        letter-spacing: .02em;
    }

    .ls-shell__sub {
        margin: 9px 0 0;
        font-size: 13px;
        letter-spacing: .08em;
        color: var(--muted);
    }

    .ls-shell__header-side {
        display: flex;
        align-items: flex-end;
        gap: 18px;
    }

    .ls-shell__stamp {
        text-align: right;
        padding-left: 18px;
        border-left: 2px solid var(--brass);
    }

    .ls-shell__stamp-num {
        display: block;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 40px;
        font-weight: 700;
        line-height: 1;
        color: var(--ink);
    }

    .ls-shell__stamp-label {
        display: block;
        margin-top: 7px;
        font-size: 12px;
        letter-spacing: .22em;
        color: var(--muted);
    }

    .ls-shell__toolbar {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 14px;
        margin: 18px 40px 6px;
    }

    .ls-shell__body {
        position: relative;
        z-index: 1;
        padding: 12px 40px 6px;
        min-height: 280px;
    }

    .ls-shell__footer {
        position: relative;
        z-index: 1;
        display: flex;
        justify-content: flex-end;
        padding: 10px 40px 6px;
    }

    .ls-shell__footer :deep(.el-pagination) {
        font-family: inherit;
    }

    .ls-shell__footer :deep(.el-pagination__total) {
        color: var(--muted);
        font-size: 12px;
        letter-spacing: .04em;
    }

    .ls-shell__footer :deep(.el-pagination .el-pager li),
    .ls-shell__footer :deep(.el-pagination button.btn-prev),
    .ls-shell__footer :deep(.el-pagination button.btn-next) {
        background: transparent;
        border-radius: 8px;
        color: var(--ink-soft);
        font-family: Georgia, "Times New Roman", serif;
        transition: all .2s;
    }

    .ls-shell__footer :deep(.el-pagination .el-pager li:hover),
    .ls-shell__footer :deep(.el-pagination button.btn-prev:hover),
    .ls-shell__footer :deep(.el-pagination button.btn-next:hover) {
        color: var(--brass-deep);
    }

    .ls-shell__footer :deep(.el-pagination .el-pager li.active) {
        background: var(--ink);
        color: #f7f3ea;
    }

    .ls-shell__footer :deep(.el-pagination__sizes .el-input .el-input__inner) {
        background: transparent;
        border: 1px solid var(--line);
        border-radius: 8px;
        color: var(--ink-soft);
        font-family: inherit;
        height: 30px;
    }

    .ls-shell__footer :deep(.el-pagination__jump) {
        color: var(--ink-soft);
        font-size: 12px;
    }

    .ls-shell__footer :deep(.el-pagination__jump .el-input__inner) {
        background: transparent;
        border: 1px solid var(--line);
        border-radius: 8px;
        color: var(--ink);
        font-family: inherit;
        height: 28px;
    }
</style>
