<template>
    <div class="msg-admin">
        <header class="msg-admin__header">
            <div class="msg-admin__heading">
                <span class="msg-admin__eyebrow">NOTICE ARCHIVE</span>
                <h1 class="msg-admin__title">消息管理</h1>
                <p class="msg-admin__sub">阅读记录与系统通知的归档视图</p>
            </div>
            <div class="msg-admin__count">
                <strong class="msg-admin__count-num">{{ total }}</strong>
                <span class="msg-admin__count-label">条记录</span>
            </div>
        </header>

        <nav class="msg-admin__tabs">
            <button type="button" class="msg-admin__tab" :class="{ 'is-active': activeTab === 'READ' }"
                @click="switchTab('READ')">消息阅读记录</button>
            <button type="button" class="msg-admin__tab" :class="{ 'is-active': activeTab === 'USER' }"
                @click="switchTab('USER')">用户消息</button>
        </nav>

        <section class="msg-admin__toolbar">
            <div class="msg-admin__filters">
                <el-input v-model="queryParams.description" placeholder="搜索标题或内容关键词" clearable class="msg-admin__search"
                    @keyup.enter.native="handleQuery" />
                <el-select v-model="queryParams.messageType" placeholder="消息类型" clearable class="msg-admin__select">
                    <el-option v-for="(value, index) in messageType" :key="index" :label="value" :value="index" />
                </el-select>
                <el-date-picker v-model="queryParams.createdAt" type="date" value-format="yyyy-MM-dd"
                    placeholder="发送日期" clearable class="msg-admin__date" />
                <el-button type="primary" icon="el-icon-search" class="msg-admin__btn msg-admin__btn--primary"
                    @click="handleQuery">检索</el-button>
                <el-button icon="el-icon-refresh" class="msg-admin__btn msg-admin__btn--ghost"
                    @click="resetQuery">重置</el-button>
            </div>
            <div v-if="activeTab === 'READ'" class="msg-admin__status-group">
                <button type="button" v-for="f in readTypes" :key="String(f.value)" class="msg-admin__status"
                    :class="{ 'is-active': statusFilter === f.value }"
                    @click="statusFilter = f.value">{{ f.text }}</button>
                <button type="button" class="msg-admin__status" :class="{ 'is-active': statusFilter === null }"
                    @click="statusFilter = null">全部</button>
            </div>
        </section>

        <main class="msg-admin__list" v-loading="loading" element-loading-text="载入中…"
            element-loading-background="rgba(246,243,236,0.72)">
            <article v-for="(item, index) in filteredList" :key="cardKey(item)"
                class="msg-admin__card" :class="{ 'is-unread': activeTab === 'READ' && item.isRead === false }"
                :style="{ animationDelay: (index * 70) + 'ms' }">
                <div class="msg-admin__card-head">
                    <span class="msg-admin__badge" :class="badgeClass(item)">{{ typeLabel(item) || '未分类' }}</span>
                    <span v-if="activeTab === 'READ'" class="msg-admin__read-state">
                        <i class="msg-admin__read-dot"></i>{{ item.isRead ? '已读' : '未读' }}
                    </span>
                </div>
                <h3 class="msg-admin__card-title">{{ item.title || '（无主题消息）' }}</h3>
                <p class="msg-admin__card-msg">{{ item.message || '—' }}</p>

                <div class="msg-admin__card-meta">
                    <span v-if="activeTab === 'READ'" class="msg-admin__meta"><em>记录ID</em>{{ item.id }}</span>
                    <span class="msg-admin__meta"><em>通知ID</em>{{ item.notificationId }}</span>
                    <span v-if="activeTab === 'READ'" class="msg-admin__meta"><em>接收账号</em>{{ item.username || '—' }}</span>
                    <span class="msg-admin__meta"><em>{{ activeTab === 'READ' ? '接收用户' : '发送用户' }}</em>{{ item.userId }}</span>
                </div>

                <div class="msg-admin__card-foot">
                    <span class="msg-admin__time"><i class="el-icon-time"></i>发送 {{ item.notificationTime | dateTime }}</span>
                    <span v-if="activeTab === 'READ'" class="msg-admin__time"><i class="el-icon-view"></i>查看 {{ item.readTime | dateTime }}</span>
                    <button type="button" class="msg-admin__delete" @click="confirmDelete(item)">
                        <i class="el-icon-delete"></i>删除
                    </button>
                </div>
            </article>

            <div v-if="!loading && filteredList.length === 0" class="msg-admin__empty">
                <span class="msg-admin__empty-mark">∅</span>
                <p>暂无{{ activeTab === 'READ' ? '消息阅读记录' : '系统通知' }}</p>
            </div>
        </main>

        <footer class="msg-admin__footer">
            <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
                :current-page="queryParams.pageNum" :page-sizes="[3, 5, 7, 10]" :page-size="queryParams.pageSize"
                layout="total, sizes, prev, pager, next, jumper" :total="total">
            </el-pagination>
        </footer>

        <confirm-dialog :visible="deleteVisible" @update:visible="deleteVisible = $event"
            :title="deleteTitle" :message="deleteMessage" confirm-text="确认删除" loading-text="删除中…"
            :loading="deleteLoading" type="danger" @confirm="onDeleteConfirm"
            @cancel="deleteVisible = false" />
    </div>
</template>

<script>
    import { listNotificationReadRecords, listNotifications, getNotificationsType, delNotification, delNotificationReadRecord } from "@/api/";
    import ConfirmDialog from '@/components/ConfirmDialog'
    export default {
        name: "NotificationReadStatusAdmin",
        components: { ConfirmDialog },
        data() {
            return {
                // 遮罩层
                loading: true,
                // 消息 Tab：READ=阅读记录，USER=用户消息
                activeTab: 'READ',
                // 阅读状态客户端过滤：null=全部
                statusFilter: null,
                // 删除确认
                deleteVisible: false,
                deleteLoading: false,
                deleteTarget: null,
                // 记录数据
                List: [],
                // 总条数
                total: 0,
                // 查询参数
                queryParams: {
                    description: undefined,
                    messageType: undefined,
                    createdAt: undefined,
                    pageNum: 1,
                    pageSize: 5
                },
                // 消息类型字典（dbValue -> webValue）
                messageType: {},
                // 是否已读标签
                readTypes: [
                    { value: false, text: "未读" },
                    { value: true, text: "已读" }
                ]
            };
        },
        computed: {
            // READ tab 的已读/未读为客户端快速过滤（分页总数仍以服务端为准）
            filteredList() {
                const list = this.List || [];
                if (this.activeTab !== 'READ' || this.statusFilter === null) {
                    return list;
                }
                return list.filter(item => item.isRead === this.statusFilter);
            },
            deleteTitle() {
                return this.activeTab === 'READ' ? '删除阅读记录' : '删除通知';
            },
            deleteMessage() {
                const t = this.deleteTarget ? (this.deleteTarget.title || '无标题消息') : '';
                return `确认删除${this.activeTab === 'READ' ? '该消息阅读记录' : '该通知'}「${t}」？`;
            }
        },
        created() {
            this.getList();
            this.getMessageType();
        },
        methods: {
            // 获取消息类型
            getMessageType() {
                getNotificationsType().then(response => {
                    this.messageType = response.data.data;
                });
            },
            // 切换 Tab：重置过滤与页码后重新拉取
            switchTab(tab) {
                if (this.activeTab === tab) {
                    return;
                }
                this.activeTab = tab;
                this.statusFilter = null;
                this.queryParams.pageNum = 1;
                this.getList();
            },
            // 获取消息列表（按 Tab 切换数据源）
            getList() {
                this.loading = true;
                const api = this.activeTab === 'USER' ? listNotifications : listNotificationReadRecords
                api(this.queryParams).then(response => {
                    this.List = response.data.data.records;
                    this.total = response.data.data.total;
                    this.loading = false;
                }).catch(err => {
                    console.error('获取消息失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                    this.loading = false
                });
            },
            // 搜索按钮操作
            handleQuery() {
                this.queryParams.pageNum = 1;
                this.getList();
            },
            // 重置按钮操作
            resetQuery() {
                this.queryParams = {
                    description: undefined,
                    messageType: undefined,
                    createdAt: undefined,
                    pageNum: 1,
                    pageSize: this.queryParams.pageSize
                };
                this.statusFilter = null;
                this.handleQuery();
            },
            handleSizeChange(val) {
                this.queryParams.pageSize = val;
                this.getList();
            },
            handleCurrentChange(val) {
                this.queryParams.pageNum = val;
                this.getList();
            },
            // 删除：打开确认弹窗（READ=删阅读记录，USER=删通知）
            confirmDelete(item) {
                this.deleteTarget = item;
                this.deleteVisible = true;
            },
            async onDeleteConfirm() {
                const item = this.deleteTarget;
                if (!item) {
                    return;
                }
                const isReadTab = this.activeTab === 'READ';
                const id = isReadTab ? item.id : item.notificationId;
                if (id == null) {
                    this.$message.warning('缺少可删除的记录标识');
                    this.deleteVisible = false;
                    return;
                }
                this.deleteLoading = true;
                try {
                    const api = isReadTab ? delNotificationReadRecord : delNotification;
                    const response = await api(id);
                    if (response.data.code === 1) {
                        this.$message.success(response.data.msg || '删除成功');
                        this.deleteVisible = false;
                        this.getList();
                    } else {
                        this.$message.error(response.data.msg || '删除失败');
                    }
                } catch (err) {
                    console.error('删除失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                } finally {
                    this.deleteLoading = false;
                }
            },
            // 卡片 key（按 tab 用不同字段保证唯一）
            cardKey(item) {
                return this.activeTab === 'READ' ? 'r' + item.id : 'u' + item.notificationId;
            },
            // 类型中文文案（READ 用 type，USER 用 notificationType）
            typeLabel(item) {
                return this.activeTab === 'READ' ? item.type : item.notificationType;
            },
            // 类型徽标配色
            badgeClass(item) {
                const t = this.typeLabel(item) || '';
                if (t.indexOf('系统') > -1) return 'badge--system';
                if (t.indexOf('委托') > -1 || t.indexOf('任务') > -1) return 'badge--task';
                if (t.indexOf('营销') > -1) return 'badge--market';
                if (t.indexOf('个人') > -1) return 'badge--own';
                return 'badge--default';
            }
        }
    }
</script>

<style lang="less" scoped>
    .msg-admin {
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
        --unread: #c07b32;
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
    .msg-admin__header {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        padding: 34px 40px 22px;
    }

    .msg-admin__eyebrow {
        display: block;
        margin-bottom: 10px;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 11px;
        letter-spacing: .34em;
        text-transform: uppercase;
        color: var(--brass);
    }

    .msg-admin__title {
        margin: 0;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 34px;
        font-weight: 700;
        line-height: 1.12;
        letter-spacing: .02em;
    }

    .msg-admin__sub {
        margin: 9px 0 0;
        font-size: 13px;
        letter-spacing: .08em;
        color: var(--muted);
    }

    .msg-admin__count {
        text-align: right;
        padding-left: 18px;
        border-left: 2px solid var(--brass);
    }

    .msg-admin__count-num {
        display: block;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 42px;
        font-weight: 700;
        line-height: 1;
        color: var(--ink);
    }

    .msg-admin__count-label {
        display: block;
        margin-top: 7px;
        font-size: 12px;
        letter-spacing: .22em;
        color: var(--muted);
    }

    /* ===== Tab 切换 ===== */
    .msg-admin__tabs {
        position: relative;
        z-index: 1;
        display: flex;
        gap: 4px;
        margin: 0 40px;
        border-bottom: 1px solid var(--line);
    }

    .msg-admin__tab {
        appearance: none;
        border: 0;
        background: transparent;
        position: relative;
        padding: 13px 20px 15px;
        font-family: inherit;
        font-size: 15px;
        letter-spacing: .05em;
        color: var(--ink-soft);
        cursor: pointer;
        transition: color .25s;
    }

    .msg-admin__tab::after {
        content: "";
        position: absolute;
        left: 20px;
        right: 20px;
        bottom: -1px;
        height: 2px;
        background: var(--brass);
        transform: scaleX(0);
        transform-origin: left;
        transition: transform .32s ease;
    }

    .msg-admin__tab:hover {
        color: var(--ink);
    }

    .msg-admin__tab.is-active {
        color: var(--ink);
        font-weight: 600;
    }

    .msg-admin__tab.is-active::after {
        transform: scaleX(1);
    }

    /* ===== 工具栏 ===== */
    .msg-admin__toolbar {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 14px;
        margin: 22px 40px 6px;
    }

    .msg-admin__filters {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;
    }

    .msg-admin__search {
        width: 250px;
    }

    .msg-admin__select {
        width: 150px;
    }

    .msg-admin__date {
        width: 158px;
    }

    .msg-admin__search :deep(.el-input__inner),
    .msg-admin__select :deep(.el-input__inner),
    .msg-admin__date :deep(.el-input__inner) {
        height: 34px;
        background: rgba(255, 252, 245, .72);
        border: 1px solid var(--line);
        border-radius: 8px;
        color: var(--ink);
        font-family: inherit;
        font-size: 13px;
        transition: border-color .2s, box-shadow .2s, background .2s;
    }

    .msg-admin__search :deep(.el-input__inner:hover),
    .msg-admin__select :deep(.el-input__inner:hover),
    .msg-admin__date :deep(.el-input__inner:hover) {
        border-color: var(--line-strong);
    }

    .msg-admin__search :deep(.el-input__inner:focus),
    .msg-admin__select :deep(.el-input__inner:focus),
    .msg-admin__date :deep(.el-input__inner:focus) {
        border-color: var(--brass);
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13);
        background: var(--card);
    }

    .msg-admin__search :deep(.el-input__inner::placeholder),
    .msg-admin__select :deep(.el-input__inner::placeholder),
    .msg-admin__date :deep(.el-input__inner::placeholder) {
        color: var(--muted);
    }

    .msg-admin__btn {
        height: 34px;
        padding: 0 18px;
        border-radius: 8px;
        font-family: inherit;
        letter-spacing: .06em;
        font-size: 13px;
        transition: transform .18s, box-shadow .18s, background .18s, border-color .18s;
    }

    .msg-admin__btn--primary {
        background: var(--ink);
        border-color: var(--ink);
        color: #f7f3ea;
    }

    .msg-admin__btn--primary:hover,
    .msg-admin__btn--primary:focus {
        background: #33493c;
        border-color: #33493c;
        color: #fff;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px -4px rgba(42, 58, 48, .5);
    }

    .msg-admin__btn--ghost {
        background: transparent;
        border: 1px solid var(--line);
        color: var(--ink-soft);
    }

    .msg-admin__btn--ghost:hover,
    .msg-admin__btn--ghost:focus {
        border-color: var(--brass);
        color: var(--brass-deep);
        background: rgba(255, 252, 245, .6);
    }

    /* 已读/未读快速过滤 */
    .msg-admin__status-group {
        display: flex;
        align-items: center;
        gap: 2px;
        padding: 3px;
        background: rgba(255, 252, 245, .82);
        border: 1px solid var(--line);
        border-radius: 9px;
    }

    .msg-admin__status {
        appearance: none;
        border: 0;
        background: transparent;
        padding: 5px 13px;
        border-radius: 7px;
        font-family: inherit;
        font-size: 12px;
        letter-spacing: .04em;
        color: var(--ink-soft);
        cursor: pointer;
        transition: background .2s, color .2s;
    }

    .msg-admin__status.is-active {
        background: var(--ink);
        color: #f7f3ea;
    }

    /* ===== 卡片列表 ===== */
    .msg-admin__list {
        position: relative;
        z-index: 1;
        padding: 14px 40px 6px;
        min-height: 320px;
    }

    .msg-admin__card {
        position: relative;
        background: var(--card);
        border: 1px solid var(--line);
        border-left: 3px solid var(--sage);
        border-radius: 12px;
        padding: 18px 22px 14px;
        margin-bottom: 14px;
        box-shadow: 0 1px 2px rgba(42, 58, 48, .05), 0 10px 26px -20px rgba(42, 58, 48, .22);
        animation: msgIn .5s ease both;
        transition: transform .25s, box-shadow .25s, border-color .25s;
    }

    .msg-admin__card:hover {
        transform: translateY(-2px);
        border-color: var(--line-strong);
        box-shadow: 0 8px 20px -10px rgba(42, 58, 48, .28);
    }

    .msg-admin__card.is-unread {
        border-left-color: var(--unread);
    }

    @keyframes msgIn {
        from {
            opacity: 0;
            transform: translateY(14px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    .msg-admin__card-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 9px;
    }

    .msg-admin__badge {
        font-size: 11px;
        letter-spacing: .1em;
        padding: 3px 11px;
        border-radius: 999px;
        background: #efe9d7;
        color: #7a6a3a;
    }

    .msg-admin__badge.badge--system {
        background: #e2e7dc;
        color: #4d6b4a;
    }

    .msg-admin__badge.badge--task {
        background: #ece5d0;
        color: #8a6d26;
    }

    .msg-admin__badge.badge--market {
        background: #f1e3dc;
        color: #a0523b;
    }

    .msg-admin__badge.badge--own {
        background: #dce8e0;
        color: #3e6b50;
    }

    .msg-admin__read-state {
        display: inline-flex;
        align-items: center;
        gap: 7px;
        font-size: 12px;
        letter-spacing: .06em;
        color: var(--sage);
    }

    .msg-admin__read-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: var(--sage);
        box-shadow: 0 0 0 3px rgba(95, 138, 111, .16);
    }

    .msg-admin__card.is-unread .msg-admin__read-state {
        color: var(--unread);
    }

    .msg-admin__card.is-unread .msg-admin__read-dot {
        background: var(--unread);
        box-shadow: 0 0 0 3px rgba(192, 123, 50, .18);
    }

    .msg-admin__card-title {
        margin: 0 0 6px;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 18px;
        font-weight: 700;
        line-height: 1.4;
        letter-spacing: .01em;
        color: var(--ink);
    }

    .msg-admin__card-msg {
        margin: 0 0 12px;
        font-size: 13px;
        line-height: 1.7;
        color: var(--ink-soft);
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .msg-admin__card-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 8px 24px;
        padding-top: 10px;
        border-top: 1px dashed var(--line);
        font-size: 12px;
        color: var(--ink-soft);
    }

    .msg-admin__meta em {
        font-style: normal;
        margin-right: 6px;
        letter-spacing: .04em;
        color: var(--muted);
    }

    .msg-admin__card-foot {
        display: flex;
        align-items: center;
        gap: 18px;
        margin-top: 10px;
        font-size: 12px;
        color: var(--muted);
    }

    .msg-admin__time i {
        margin-right: 5px;
    }

    .msg-admin__delete {
        margin-left: auto;
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
        color: var(--terra);
        cursor: pointer;
        transition: background .2s, border-color .2s;
    }

    .msg-admin__delete:hover {
        background: rgba(180, 84, 58, .09);
        border-color: rgba(180, 84, 58, .28);
    }

    /* 空态 */
    .msg-admin__empty {
        text-align: center;
        padding: 64px 0;
        color: var(--muted);
    }

    .msg-admin__empty-mark {
        display: block;
        font-family: Georgia, "Times New Roman", serif;
        font-size: 48px;
        line-height: 1;
        color: var(--line-strong);
    }

    .msg-admin__empty p {
        margin: 14px 0 0;
        font-size: 13px;
        letter-spacing: .1em;
    }

    /* ===== 分页 ===== */
    .msg-admin__footer {
        position: relative;
        z-index: 1;
        display: flex;
        justify-content: flex-end;
        padding: 8px 40px 6px;
    }

    .msg-admin__footer :deep(.el-pagination) {
        font-family: inherit;
    }

    .msg-admin__footer :deep(.el-pagination__total) {
        color: var(--muted);
        font-size: 12px;
        letter-spacing: .04em;
    }

    .msg-admin__footer :deep(.el-pagination .el-pager li),
    .msg-admin__footer :deep(.el-pagination button.btn-prev),
    .msg-admin__footer :deep(.el-pagination button.btn-next) {
        background: transparent;
        border-radius: 8px;
        color: var(--ink-soft);
        font-family: Georgia, "Times New Roman", serif;
        transition: all .2s;
    }

    .msg-admin__footer :deep(.el-pagination .el-pager li:hover),
    .msg-admin__footer :deep(.el-pagination button.btn-prev:hover),
    .msg-admin__footer :deep(.el-pagination button.btn-next:hover) {
        color: var(--brass-deep);
    }

    .msg-admin__footer :deep(.el-pagination .el-pager li.active) {
        background: var(--ink);
        color: #f7f3ea;
    }

    .msg-admin__footer :deep(.el-pagination__sizes .el-input .el-input__inner) {
        background: transparent;
        border: 1px solid var(--line);
        border-radius: 8px;
        color: var(--ink-soft);
        font-family: inherit;
        height: 30px;
    }

    .msg-admin__footer :deep(.el-pagination__jump) {
        color: var(--ink-soft);
        font-size: 12px;
    }

    .msg-admin__footer :deep(.el-pagination__jump .el-input__inner) {
        background: transparent;
        border: 1px solid var(--line);
        border-radius: 8px;
        color: var(--ink);
        font-family: inherit;
        height: 28px;
    }
</style>
