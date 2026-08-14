<template>
    <div class="bd-admin">
        <header class="bd-admin__header">
            <div class="bd-admin__heading">
                <span class="bd-admin__eyebrow">ANNOUNCEMENT BOARD</span>
                <h1 class="bd-admin__title">公告管理</h1>
                <p class="bd-admin__sub">系统公告的发布与归档视图</p>
            </div>
            <div class="bd-admin__count">
                <strong class="bd-admin__count-num">{{ total }}</strong>
                <span class="bd-admin__count-label">条公告</span>
            </div>
        </header>

        <section class="bd-admin__toolbar">
            <div class="bd-admin__filters">
                <el-input v-model="queryParams.description" placeholder="搜索公告内容关键词" clearable class="bd-admin__search"
                    @keyup.enter.native="handleQuery" />
                <el-select v-model="queryParams.status" placeholder="公告状态" clearable class="bd-admin__select">
                    <el-option label="草稿" value="DRAFT" />
                    <el-option label="已发布" value="PUBLISHED" />
                    <el-option label="已撤回" value="WITHDRAWN" />
                </el-select>
                <el-select v-model="queryParams.queryRules" placeholder="排序规则" clearable class="bd-admin__select">
                    <el-option label="发布时间" value="1" />
                    <el-option label="生效时间" value="2" />
                    <el-option label="更新时间" value="3" />
                    <el-option label="创建时间" value="4" />
                </el-select>
                <el-button type="primary" icon="el-icon-search" class="bd-admin__btn bd-admin__btn--primary"
                    @click="handleQuery">检索</el-button>
                <el-button icon="el-icon-refresh" class="bd-admin__btn bd-admin__btn--ghost"
                    @click="resetQuery">重置</el-button>
            </div>
            <el-button type="primary" icon="el-icon-plus" class="bd-admin__btn bd-admin__btn--add"
                @click="addDialogSystemBulletin">添加公告</el-button>
        </section>

        <main class="bd-admin__list" v-loading="loading" element-loading-text="载入中…"
            element-loading-background="rgba(246,243,236,0.72)">
            <article v-for="(item, index) in list" :key="'bd' + item.announcementId"
                class="bd-admin__card" :class="{ 'is-pinned': item.isPinned }"
                :style="{ animationDelay: (index * 70) + 'ms' }">
                <div class="bd-admin__card-head">
                    <span class="bd-admin__badge" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
                    <span class="bd-admin__card-marks">
                        <span v-if="item.isPinned" class="bd-admin__pinned"><i class="el-icon-top"></i>置顶</span>
                        <span class="bd-admin__no">NO.{{ item.announcementId }}</span>
                    </span>
                </div>
                <h3 class="bd-admin__card-title">{{ item.title || '（无标题公告）' }}</h3>
                <p class="bd-admin__card-content">{{ item.content || '—' }}</p>

                <div class="bd-admin__card-meta">
                    <span class="bd-admin__meta"><em>发布人</em>{{ item.publisherId || '—' }}</span>
                    <span class="bd-admin__meta"><em>更新人</em>{{ item.updatedBy || '—' }}</span>
                    <span class="bd-admin__meta"><em>发布时间</em>{{ item.publishTime | dateTime }}</span>
                    <span class="bd-admin__meta"><em>生效</em>{{ item.startEffectiveTime | dateTime }} 至 {{ item.endEffectiveTime | dateTime }}</span>
                </div>

                <div class="bd-admin__card-foot">
                    <span class="bd-admin__time"><i class="el-icon-date"></i>创建 {{ item.createdAt | dateTime }}</span>
                    <span class="bd-admin__time"><i class="el-icon-edit-outline"></i>更新 {{ item.updatedAt | dateTime }}</span>
                    <span class="bd-admin__actions">
                        <button type="button" class="bd-admin__action bd-admin__action--edit"
                            @click="handleUpdate(item.announcementId)"><i class="el-icon-edit"></i>编辑</button>
                        <button type="button" class="bd-admin__action bd-admin__action--del"
                            @click="confirmDelete(item)"><i class="el-icon-delete"></i>删除</button>
                    </span>
                </div>
            </article>

            <div v-if="!loading && list.length === 0" class="bd-admin__empty">
                <span class="bd-admin__empty-mark">∅</span>
                <p>暂无公告</p>
            </div>
        </main>

        <footer class="bd-admin__footer">
            <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
                :current-page="queryParams.pageNum" :page-sizes="[5, 7, 10]" :page-size="queryParams.pageSize"
                layout="total, sizes, prev, pager, next, jumper" :total="total">
            </el-pagination>
        </footer>

        <el-dialog :title="dialogMode === 'add' ? '新增系统公告' : '更改系统公告'" :visible.sync="open" width="760px"
            custom-class="bd-admin__dialog" :close-on-click-modal="false">
            <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="bd-admin__form">
                <el-row :gutter="16">
                    <el-col :span="16">
                        <el-form-item label="公告主题" prop="title">
                            <el-input v-model="form.title" placeholder="请输入公告主题" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="公告状态" prop="status">
                            <el-select v-model="form.status" placeholder="请选择">
                                <el-option label="草稿" value="DRAFT" />
                                <el-option label="已发布" value="PUBLISHED" />
                                <el-option label="已撤回" value="WITHDRAWN" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-form-item label="公告内容" prop="content">
                    <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入公告内容" />
                </el-form-item>
                <el-form-item label="公告生效时间" prop="startEffectiveTime">
                    <el-date-picker v-model="dateTimeRange" type="datetimerange" range-separator="至"
                        start-placeholder="开始日期" end-placeholder="结束日期" style="width: 100%" />
                </el-form-item>
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="公告发布时间" prop="publishTime">
                            <el-date-picker v-model="form.publishTime" type="datetime" placeholder="选择日期时间"
                                style="width: 100%" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="是否置顶" prop="isPinned">
                            <el-switch v-model="form.isPinned" active-color="#5f8a6f" inactive-color="#c8c2b2" />
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <div slot="footer" class="bd-admin__dialog-footer">
                <el-button class="bd-admin__btn bd-admin__btn--ghost" @click="open = false">取 消</el-button>
                <el-button type="primary" class="bd-admin__btn bd-admin__btn--primary"
                    @click="handleUpdateSubmit">确 定</el-button>
            </div>
        </el-dialog>

        <confirm-dialog :visible="deleteVisible" @update:visible="deleteVisible = $event"
            title="删除公告" :message="deleteMessage" confirm-text="确认删除" loading-text="删除中…"
            :loading="deleteLoading" type="danger" @confirm="onDeleteConfirm"
            @cancel="deleteVisible = false" />
    </div>
</template>

<script>
    import { getSystemBulletinList, deleteSystemBulletin, getSystemBulletinById, updateSystemBulletin, createSystemBulletin } from "@/api/";
    import ConfirmDialog from '@/components/ConfirmDialog'
    import { ANNOUNCEMENT_STATUS } from '@/constants/enums'
    export default {
        name: "SystemBulletinList",
        components: { ConfirmDialog },
        data() {
            return {
                ANNOUNCEMENT_STATUS,
                list: [],
                open: false,
                dialogMode: 'edit',
                loading: true,
                total: 0,
                queryParams: {
                    description: undefined,
                    status: undefined,
                    queryRules: '1',
                    pageNum: 1,
                    pageSize: 5
                },
                form: {},
                dateTimeRange: [],
                // 删除确认
                deleteVisible: false,
                deleteLoading: false,
                deleteTarget: null,
                rules: {
                    title: [
                        { required: true, message: "公告主题不能为空", trigger: "blur" }
                    ],
                    content: [
                        { required: true, message: "公告内容不能为空", trigger: "blur" }
                    ],
                    status: [
                        { required: true, message: "公告状态不能为空", trigger: "change" }
                    ]
                }
            }
        },
        computed: {
            deleteMessage() {
                const t = this.deleteTarget ? (this.deleteTarget.title || '无标题公告') : '';
                return `确认删除公告「${t}」？删除后不可恢复。`;
            }
        },
        created() {
            this.getList();
        },
        methods: {
            handleQuery() {
                this.queryParams.pageNum = 1;
                this.getList();
            },
            resetQuery() {
                this.queryParams = {
                    description: undefined,
                    status: undefined,
                    queryRules: '1',
                    pageNum: 1,
                    pageSize: this.queryParams.pageSize
                };
                this.handleQuery();
            },
            /** 获取公告列表 */
            getList() {
                this.loading = true;
                getSystemBulletinList(this.queryParams).then(response => {
                    if (response.data.code == 1) {
                        this.list = response.data.data.records;
                        this.total = response.data.data.total;
                        this.loading = false;
                    } else {
                        this.$message({
                            message: response.data.msg,
                            type: 'error'
                        });
                        this.loading = false;
                    }
                }).catch(err => {
                    console.error('获取公告列表失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                    this.loading = false;
                });
            },
            handleUpdate(id) {
                getSystemBulletinById(id).then(response => {
                    if (response.data.code == 1) {
                        this.dialogMode = 'edit';
                        this.form = response.data.data;
                        this.dateTimeRange = [
                            this.form.startEffectiveTime ? new Date(this.form.startEffectiveTime) : null,
                            this.form.endEffectiveTime ? new Date(this.form.endEffectiveTime) : null
                        ];
                        this.form.publishTime = this.form.publishTime ? new Date(this.form.publishTime) : null;
                        this.open = true;
                    } else {
                        this.$message({
                            message: response.data.msg,
                            type: 'error'
                        });
                    }
                });
            },
            confirmDelete(item) {
                this.deleteTarget = item;
                this.deleteVisible = true;
            },
            async onDeleteConfirm() {
                const item = this.deleteTarget;
                if (!item) {
                    return;
                }
                this.deleteLoading = true;
                try {
                    const response = await deleteSystemBulletin(item.announcementId);
                    if (response.data.code === 1) {
                        this.$message.success(response.data.msg || '删除成功');
                        this.deleteVisible = false;
                        this.getList();
                    } else {
                        this.$message.error(response.data.msg || '删除失败');
                    }
                } catch (err) {
                    console.error('删除公告失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                } finally {
                    this.deleteLoading = false;
                }
            },
            handleSizeChange(val) {
                this.queryParams.pageSize = val;
                this.getList();
            },
            handleCurrentChange(val) {
                this.queryParams.pageNum = val;
                this.getList();
            },
            handleUpdateSubmit() {
                this.$refs["form"].validate(valid => {
                    if (!valid) {
                        return false;
                    }
                    if (!this.dateTimeRange || this.dateTimeRange.length !== 2 || !this.dateTimeRange[0] || !this.dateTimeRange[1]) {
                        this.$message.error("请选择完整的公告生效时间");
                        return false;
                    }
                    if (this.form.publishTime && this.dateTimeRange[0] < this.form.publishTime) {
                        this.$message.error("公告发布时间不能晚于生效时间");
                        return false;
                    }
                    this.form.startEffectiveTime = this.dateTimeRange[0];
                    this.form.endEffectiveTime = this.dateTimeRange[1];
                    this.form.createdAt = null;
                    this.form.updatedAt = null;

                    if (this.dialogMode === 'add') {
                        this.form.announcementId = undefined;
                        createSystemBulletin(this.form).then(response => {
                            if (response.data.code == 1) {
                                this.$message({
                                    message: response.data.msg,
                                    type: 'success'
                                });
                                this.open = false;
                                this.getList();
                            } else {
                                this.$message({
                                    message: response.data.msg,
                                    type: 'error'
                                });
                            }
                        }).catch(err => {
                            console.error('新增系统公告失败：', err)
                            this.$message.error('请求异常，请稍后重试')
                        });
                    } else {
                        updateSystemBulletin(this.form).then(response => {
                            if (response.data.code == 1) {
                                this.$message({
                                    message: response.data.msg,
                                    type: 'success'
                                });
                                this.open = false;
                                this.getList();
                            } else {
                                this.$message({
                                    message: response.data.msg,
                                    type: 'error'
                                });
                            }
                        }).catch(err => {
                            console.error('更新系统公告失败：', err)
                            this.$message.error('请求异常，请稍后重试')
                        });
                    }
                });
            },
            addDialogSystemBulletin() {
                this.dialogMode = 'add';
                this.form = {
                    title: '',
                    content: '',
                    status: 'DRAFT',
                    isPinned: false,
                    publishTime: null,
                    startEffectiveTime: null,
                    endEffectiveTime: null
                };
                this.dateTimeRange = [];
                this.open = true;
            },
            statusLabel(status) {
                const map = {
                    DRAFT: ANNOUNCEMENT_STATUS.DRAFT,
                    PUBLISHED: ANNOUNCEMENT_STATUS.PUBLISHED,
                    WITHDRAWN: ANNOUNCEMENT_STATUS.WITHDRAWN
                };
                return map[status] || status;
            },
            statusClass(status) {
                return 'badge--' + (status || 'draft').toLowerCase();
            }
        }
    }
</script>

<style lang="less" scoped>
    .bd-admin {
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

        /* 颗粒层置于内容之下：避免 fixed 弹层被 z-index 困住导致整页卡死 */
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

    /* ===== 页头 ===== */
    .bd-admin__header {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        padding: 34px 40px 22px;
    }

    .bd-admin__eyebrow {
        display: block;
        margin-bottom: 10px;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 11px;
        letter-spacing: .34em;
        text-transform: uppercase;
        color: var(--brass);
    }

    .bd-admin__title {
        margin: 0;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 34px;
        font-weight: 700;
        line-height: 1.12;
        letter-spacing: .02em;
    }

    .bd-admin__sub {
        margin: 9px 0 0;
        font-size: 13px;
        letter-spacing: .08em;
        color: var(--muted);
    }

    .bd-admin__count {
        text-align: right;
        padding-left: 18px;
        border-left: 2px solid var(--brass);
    }

    .bd-admin__count-num {
        display: block;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 42px;
        font-weight: 700;
        line-height: 1;
        color: var(--ink);
    }

    .bd-admin__count-label {
        display: block;
        margin-top: 7px;
        font-size: 12px;
        letter-spacing: .22em;
        color: var(--muted);
    }

    /* ===== 工具栏 ===== */
    .bd-admin__toolbar {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 14px;
        margin: 22px 40px 6px;
    }

    .bd-admin__filters {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;
    }

    .bd-admin__search {
        width: 250px;
    }

    .bd-admin__select {
        width: 140px;
    }

    .bd-admin__search :deep(.el-input__inner),
    .bd-admin__select :deep(.el-input__inner) {
        height: 34px;
        background: rgba(255, 252, 245, .72);
        border: 1px solid var(--line);
        border-radius: 8px;
        color: var(--ink);
        font-family: inherit;
        font-size: 13px;
        transition: border-color .2s, box-shadow .2s, background .2s;
    }

    .bd-admin__search :deep(.el-input__inner:hover),
    .bd-admin__select :deep(.el-input__inner:hover) {
        border-color: var(--line-strong);
    }

    .bd-admin__search :deep(.el-input__inner:focus),
    .bd-admin__select :deep(.el-input__inner:focus) {
        border-color: var(--brass);
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13);
        background: var(--card);
    }

    .bd-admin__search :deep(.el-input__inner::placeholder),
    .bd-admin__select :deep(.el-input__inner::placeholder) {
        color: var(--muted);
    }

    .bd-admin__btn {
        height: 34px;
        padding: 0 18px;
        border-radius: 8px;
        font-family: inherit;
        letter-spacing: .06em;
        font-size: 13px;
        transition: transform .18s, box-shadow .18s, background .18s, border-color .18s;
    }

    .bd-admin__btn--primary {
        background: var(--ink);
        border-color: var(--ink);
        color: #f7f3ea;
    }

    .bd-admin__btn--primary:hover,
    .bd-admin__btn--primary:focus {
        background: #33493c;
        border-color: #33493c;
        color: #fff;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px -4px rgba(42, 58, 48, .5);
    }

    .bd-admin__btn--ghost {
        background: transparent;
        border: 1px solid var(--line);
        color: var(--ink-soft);
    }

    .bd-admin__btn--ghost:hover,
    .bd-admin__btn--ghost:focus {
        border-color: var(--brass);
        color: var(--brass-deep);
        background: rgba(255, 252, 245, .6);
    }

    .bd-admin__btn--add {
        background: var(--brass);
        border-color: var(--brass);
        color: #fdf9ec;
        padding: 0 20px;
    }

    .bd-admin__btn--add:hover,
    .bd-admin__btn--add:focus {
        background: var(--brass-deep);
        border-color: var(--brass-deep);
        color: #fff;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px -4px rgba(150, 112, 31, .5);
    }

    /* ===== 卡片列表 ===== */
    .bd-admin__list {
        position: relative;
        z-index: 1;
        padding: 14px 40px 6px;
        min-height: 320px;
    }

    .bd-admin__card {
        position: relative;
        background: var(--card);
        border: 1px solid var(--line);
        border-left: 3px solid var(--line-strong);
        border-radius: 12px;
        padding: 18px 22px 14px;
        margin-bottom: 14px;
        box-shadow: 0 1px 2px rgba(42, 58, 48, .05), 0 10px 26px -20px rgba(42, 58, 48, .22);
        animation: bdIn .5s ease both;
        transition: transform .25s, box-shadow .25s, border-color .25s;
    }

    .bd-admin__card:hover {
        transform: translateY(-2px);
        border-color: var(--line-strong);
        box-shadow: 0 8px 20px -10px rgba(42, 58, 48, .28);
    }

    .bd-admin__card.is-pinned {
        border-left-color: var(--brass);
    }

    @keyframes bdIn {
        from {
            opacity: 0;
            transform: translateY(14px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    .bd-admin__card-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 9px;
    }

    .bd-admin__badge {
        font-size: 11px;
        letter-spacing: .1em;
        padding: 3px 11px;
        border-radius: 999px;
        background: #efe9d7;
        color: #7a6a3a;
    }

    .bd-admin__badge.badge--draft {
        background: #ece8de;
        color: #807a68;
    }

    .bd-admin__badge.badge--published {
        background: #dce8e0;
        color: #3e6b50;
    }

    .bd-admin__badge.badge--withdrawn {
        background: #f1e3dc;
        color: #a0523b;
    }

    .bd-admin__card-marks {
        display: inline-flex;
        align-items: center;
        gap: 10px;
    }

    .bd-admin__pinned {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 11px;
        letter-spacing: .06em;
        color: var(--brass-deep);
        background: rgba(185, 137, 44, .12);
        padding: 2px 9px;
        border-radius: 999px;
    }

    .bd-admin__no {
        font-family: Georgia, "Times New Roman", serif;
        font-size: 12px;
        letter-spacing: .08em;
        color: var(--muted);
    }

    .bd-admin__card-title {
        margin: 0 0 6px;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 18px;
        font-weight: 700;
        line-height: 1.4;
        letter-spacing: .01em;
        color: var(--ink);
    }

    .bd-admin__card-content {
        margin: 0 0 12px;
        font-size: 13px;
        line-height: 1.7;
        color: var(--ink-soft);
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .bd-admin__card-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 8px 24px;
        padding-top: 10px;
        border-top: 1px dashed var(--line);
        font-size: 12px;
        color: var(--ink-soft);
    }

    .bd-admin__meta em {
        font-style: normal;
        margin-right: 6px;
        letter-spacing: .04em;
        color: var(--muted);
    }

    .bd-admin__card-foot {
        display: flex;
        align-items: center;
        gap: 18px;
        margin-top: 10px;
        font-size: 12px;
        color: var(--muted);
    }

    .bd-admin__time i {
        margin-right: 5px;
    }

    .bd-admin__actions {
        margin-left: auto;
        display: inline-flex;
        align-items: center;
        gap: 6px;
    }

    .bd-admin__action {
        appearance: none;
        border: 1px solid transparent;
        background: transparent;
        display: inline-flex;
        align-items: center;
        gap: 5px;
        padding: 5px 12px;
        border-radius: 7px;
        font-family: inherit;
        font-size: 12px;
        letter-spacing: .04em;
        cursor: pointer;
        transition: background .2s, border-color .2s, color .2s;
    }

    .bd-admin__action--edit {
        color: var(--brass-deep);
    }

    .bd-admin__action--edit:hover {
        background: rgba(185, 137, 44, .1);
        border-color: rgba(185, 137, 44, .28);
    }

    .bd-admin__action--del {
        color: var(--terra);
    }

    .bd-admin__action--del:hover {
        background: rgba(180, 84, 58, .09);
        border-color: rgba(180, 84, 58, .28);
    }

    /* 空态 */
    .bd-admin__empty {
        text-align: center;
        padding: 64px 0;
        color: var(--muted);
    }

    .bd-admin__empty-mark {
        display: block;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 48px;
        line-height: 1;
        color: var(--line-strong);
    }

    .bd-admin__empty p {
        margin: 14px 0 0;
        font-size: 13px;
        letter-spacing: .1em;
    }

    /* ===== 分页 ===== */
    .bd-admin__footer {
        position: relative;
        z-index: 1;
        display: flex;
        justify-content: flex-end;
        padding: 8px 40px 6px;
    }

    .bd-admin__footer :deep(.el-pagination) {
        font-family: inherit;
    }

    .bd-admin__footer :deep(.el-pagination__total) {
        color: var(--muted);
        font-size: 12px;
        letter-spacing: .04em;
    }

    .bd-admin__footer :deep(.el-pagination .el-pager li),
    .bd-admin__footer :deep(.el-pagination button.btn-prev),
    .bd-admin__footer :deep(.el-pagination button.btn-next) {
        background: transparent;
        border-radius: 8px;
        color: var(--ink-soft);
        font-family: Georgia, "Times New Roman", serif;
        transition: all .2s;
    }

    .bd-admin__footer :deep(.el-pagination .el-pager li:hover),
    .bd-admin__footer :deep(.el-pagination button.btn-prev:hover),
    .bd-admin__footer :deep(.el-pagination button.btn-next:hover) {
        color: var(--brass-deep);
    }

    .bd-admin__footer :deep(.el-pagination .el-pager li.active) {
        background: var(--ink);
        color: #f7f3ea;
    }

    .bd-admin__footer :deep(.el-pagination__sizes .el-input .el-input__inner) {
        background: transparent;
        border: 1px solid var(--line);
        border-radius: 8px;
        color: var(--ink-soft);
        font-family: inherit;
        height: 30px;
    }

    .bd-admin__footer :deep(.el-pagination__jump) {
        color: var(--ink-soft);
        font-size: 12px;
    }

    .bd-admin__footer :deep(.el-pagination__jump .el-input__inner) {
        background: transparent;
        border: 1px solid var(--line);
        border-radius: 8px;
        color: var(--ink);
        font-family: inherit;
        height: 28px;
    }

    /* ===== 添加/编辑弹窗 ===== */
    .bd-admin :deep(.bd-admin__dialog) {
        background: var(--card);
        border-radius: 16px;
        box-shadow: 0 24px 60px -20px rgba(42, 58, 48, .4);
        overflow: hidden;
    }

    .bd-admin :deep(.bd-admin__dialog .el-dialog__header) {
        padding: 22px 28px 18px;
        border-bottom: 1px solid var(--line);
        background: rgba(255, 252, 245, .6);
    }

    .bd-admin :deep(.bd-admin__dialog .el-dialog__title) {
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 20px;
        font-weight: 700;
        color: var(--ink);
        letter-spacing: .02em;
    }

    .bd-admin :deep(.bd-admin__dialog .el-dialog__headerbtn .el-dialog__close) {
        color: var(--muted);
    }

    .bd-admin :deep(.bd-admin__dialog .el-dialog__body) {
        padding: 24px 28px 8px;
    }

    .bd-admin__form :deep(.el-form-item__label) {
        color: var(--ink-soft);
        font-size: 13px;
        letter-spacing: .03em;
    }

    .bd-admin__form :deep(.el-form-item) {
        margin-bottom: 20px;
    }

    .bd-admin__form :deep(.el-input__inner),
    .bd-admin__form :deep(.el-textarea__inner) {
        background: rgba(255, 252, 245, .85);
        border: 1px solid var(--line);
        border-radius: 8px;
        color: var(--ink);
        font-family: inherit;
        font-size: 13px;
        transition: border-color .2s, box-shadow .2s, background .2s;
    }

    .bd-admin__form :deep(.el-input__inner:hover),
    .bd-admin__form :deep(.el-textarea__inner:hover) {
        border-color: var(--line-strong);
    }

    .bd-admin__form :deep(.el-input__inner:focus),
    .bd-admin__form :deep(.el-textarea__inner:focus) {
        border-color: var(--brass);
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13);
        background: var(--card);
    }

    .bd-admin__form :deep(.el-input__inner::placeholder),
    .bd-admin__form :deep(.el-textarea__inner::placeholder) {
        color: var(--muted);
    }

    .bd-admin__dialog-footer {
        text-align: right;
        padding: 16px 28px 22px;
        border-top: 1px solid var(--line);
        background: rgba(255, 252, 245, .6);
    }

    .bd-admin__dialog-footer .bd-admin__btn {
        margin-left: 10px;
    }
</style>
