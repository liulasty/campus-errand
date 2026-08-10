<template>
    <div class="message-center">
        <!-- 页头 -->
        <header class="mc-header">
            <div class="mc-heading">
                <div class="mc-title-row">
                    <h2 class="mc-title"><i class="el-icon-bell"></i> 消息中心</h2>
                    <span class="mc-live"><i></i> 实时同步</span>
                </div>
                <p class="mc-sub">委托进展、系统公告与个人信息，集中一处查看</p>
            </div>
            <div class="mc-stats">
                <div class="mc-stat">
                    <span class="mc-stat-num">{{ allList.length }}</span>
                    <span class="mc-stat-label">全部</span>
                </div>
                <div class="mc-stat warn" :class="{ hot: unreadTotal > 0 }">
                    <span class="mc-stat-num">{{ unreadTotal }}</span>
                    <span class="mc-stat-label">未读</span>
                </div>
                <div class="mc-stat">
                    <span class="mc-stat-num">{{ readTotal }}</span>
                    <span class="mc-stat-label">已读</span>
                </div>
            </div>
        </header>

        <!-- 分段切换 -->
        <div class="mc-tabs" role="tablist">
            <button type="button" class="mc-tab" :class="{ active: activeTab === 'all' }" @click="switchTab('all')">全部</button>
            <button type="button" class="mc-tab" :class="{ active: activeTab === 'unread' }" @click="switchTab('unread')">
                未读
                <span v-if="unreadTotal > 0" class="mc-tab-badge">{{ unreadTotal }}</span>
            </button>
        </div>

        <!-- 消息列表 -->
        <div v-loading="loading" class="mc-body">
            <div v-if="!loading && list.length === 0" class="mc-empty">
                <div class="mc-empty-icon"><i class="el-icon-bell"></i></div>
                <p>{{ activeTab === 'unread' ? '没有未读消息，休息一下吧' : '暂无消息，新的委托进展会出现在这里' }}</p>
            </div>

            <div v-for="(item, index) in list" :key="item.id" class="mc-item" :class="{ unread: !item.isRead }"
                @click="openDetail(item)" :style="{ 'animation-delay': index * 40 + 'ms' }">
                <div class="mc-item-head">
                    <span v-if="!item.isRead" class="mc-dot"></span>
                    <el-tag v-if="item.notificationType" :type="typeMeta(item.notificationType).tag" size="mini" effect="light"
                        class="mc-type">{{ typeMeta(item.notificationType).label }}</el-tag>
                    <span class="mc-item-title">{{ item.title }}</span>
                    <span class="mc-item-date">{{ item.date | relativeTime }}</span>
                </div>
                <p class="mc-item-desc">{{ item.description }}</p>
            </div>

            <div v-if="total > 0" class="mc-footer">
                <el-pagination @current-change="handleCurrentChange" :current-page="queryParams.pageNum"
                    :page-size="queryParams.pageSize" layout="total, prev, pager, next" :total="total" background>
                </el-pagination>
            </div>
        </div>

        <!-- 详情弹窗（el-dialog 替换 $alert：关闭切换 visible，避免未处理 Promise rejection） -->
        <el-dialog :visible.sync="detailVisible" custom-class="mc-detail" width="520px" append-to-body>
            <div slot="title" class="mc-detail-title">
                <el-tag v-if="detailItem.notificationType" :type="typeMeta(detailItem.notificationType).tag" size="mini"
                    effect="light">{{ typeMeta(detailItem.notificationType).label }}</el-tag>
                <span class="mc-detail-heading">{{ detailItem.title }}</span>
            </div>
            <div class="mc-detail-body">
                <div class="mc-detail-meta">
                    <span><i class="el-icon-time"></i> {{ detailItem.date | dateTime }}</span>
                    <span class="mc-detail-status" :class="{ read: detailItem.isRead }">
                        {{ detailItem.isRead ? '已读' : '未读' }}
                    </span>
                </div>
                <div class="mc-detail-content">{{ detailItem.description }}</div>
            </div>
            <div slot="footer" class="mc-detail-footer">
                <el-button type="primary" size="small" @click="closeDetail">知道了</el-button>
            </div>
        </el-dialog>
    </div>
</template>
<script>
    import { getMyNotifications, markNotificationRead } from '@/api/'
    export default {
        name: 'MessageCenter',
        data() {
            return {
                activeTab: 'all',
                allList: [],
                list: [],
                total: 0,
                loading: false,
                queryParams: {
                    pageNum: 1,
                    pageSize: 10
                },
                detailVisible: false,
                detailItem: {}
            }
        },
        computed: {
            // 基于全量缓存的准确计数（服务端无未读过滤，一次拉全量后本地计算）
            unreadTotal() {
                return this.allList.filter(i => !i.isRead).length
            },
            readTotal() {
                return this.allList.length - this.unreadTotal
            }
        },
        created() {
            this.getList()
        },
        methods: {
            async getList() {
                this.loading = true
                try {
                    // 后端不支持未读过滤：先取总数，再按总数拉全量，客户端统一计数与分页，
                    // 保证「全部/未读」两 tab 的计数与分页数据一致
                    const meta = await getMyNotifications({ pageNum: 1, pageSize: 1 })
                    if (meta.data.code !== 1) {
                        this.$message.error(meta.data.msg || '获取消息失败')
                        return
                    }
                    const total = meta.data.data.total || 0
                    this.allList = []
                    if (total > 0) {
                        const res = await getMyNotifications({ pageNum: 1, pageSize: total })
                        if (res.data.code === 1) {
                            this.allList = res.data.data.records || []
                        } else {
                            this.$message.error(res.data.msg || '获取消息失败')
                        }
                    }
                    this.applyView()
                } catch (err) {
                    console.error('获取消息失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                } finally {
                    this.loading = false
                }
            },
            applyView() {
                const view = this.activeTab === 'unread' ? this.allList.filter(i => !i.isRead) : this.allList
                this.total = view.length
                const maxPage = Math.max(1, Math.ceil(this.total / this.queryParams.pageSize))
                if (this.queryParams.pageNum > maxPage) this.queryParams.pageNum = maxPage
                this.list = view.slice((this.queryParams.pageNum - 1) * this.queryParams.pageSize,
                    this.queryParams.pageNum * this.queryParams.pageSize)
            },
            switchTab(tab) {
                this.activeTab = tab
                this.queryParams.pageNum = 1
                this.applyView()
            },
            handleCurrentChange(val) {
                this.queryParams.pageNum = val
                this.applyView()
            },
            openDetail(item) {
                if (!item.isRead) {
                    markNotificationRead(item.id).then(() => {
                        item.isRead = true
                        // 未读 tab 下已读项即时移出视图，计数/分页同步刷新
                        this.applyView()
                    }).catch(err => {
                        console.error('标记已读失败：', err)
                    })
                }
                this.detailItem = item
                this.detailVisible = true
            },
            closeDetail() {
                this.detailVisible = false
            },
            typeMeta(type) {
                const map = {
                    OWN: { label: '个人信息', tag: 'success' },
                    USER: { label: '委托信息', tag: 'warning' },
                    SYSTEM: { label: '系统信息', tag: 'info' },
                    MARKETING: { label: '营销信息', tag: 'danger' }
                }
                return map[type] || { label: '通知', tag: 'info' }
            }
        }
    }
</script>
<style lang="less" scoped>
    .message-center {
        padding: 8px 4px 28px;
    }

    /* ---------- 页头 ---------- */
    .mc-header {
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        gap: 16px;
        margin-bottom: 22px;

        .mc-title-row {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .mc-title {
            margin: 0;
            font-size: 22px;
            font-weight: 700;
            color: var(--ce-text);
            display: flex;
            align-items: center;
            gap: 12px;

            i {
                width: 42px;
                height: 42px;
                border-radius: 12px;
                background: linear-gradient(135deg, var(--ce-primary) 0%, #14a387 100%);
                color: #fff;
                font-size: 20px;
                display: inline-flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 6px 14px rgba(14, 124, 102, .28);
            }
        }

        .mc-live {
            font-size: 12px;
            color: var(--ce-primary);
            background: var(--ce-primary-light);
            border: 1px solid #bfe0d8;
            padding: 3px 10px;
            border-radius: 20px;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            white-space: nowrap;

            i {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: var(--ce-primary);
                display: inline-block;
                animation: mc-pulse 1.6s infinite;
            }
        }

        .mc-sub {
            margin: 8px 0 0;
            font-size: 13px;
            color: var(--ce-text-2);
        }

        .mc-stats {
            display: flex;
            gap: 8px;
            flex-shrink: 0;

            .mc-stat {
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 4px;
                min-width: 68px;
                padding: 10px 12px;
                border-radius: 12px;
                background: #fff;
                border: 1px solid var(--ce-border);
                box-shadow: var(--ce-shadow);
                transition: all .25s;

                .mc-stat-num {
                    font-size: 20px;
                    font-weight: 700;
                    line-height: 1;
                    color: var(--ce-text);
                    font-variant-numeric: tabular-nums;
                }

                .mc-stat-label {
                    font-size: 12px;
                    color: var(--ce-text-2);
                }

                &.warn.hot {
                    border-color: #f3d9a8;
                    background: #fff9ef;

                    .mc-stat-num {
                        color: #b4790a;
                    }
                }
            }
        }
    }

    /* ---------- 分段切换 ---------- */
    .mc-tabs {
        display: inline-flex;
        gap: 4px;
        padding: 4px;
        background: #eef1f3;
        border-radius: 12px;
        margin-bottom: 16px;

        .mc-tab {
            border: none;
            background: transparent;
            padding: 7px 18px;
            font-size: 13px;
            color: var(--ce-text-2);
            border-radius: 9px;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all .2s;

            &:hover {
                color: var(--ce-text);
            }

            &.active {
                background: #fff;
                color: var(--ce-primary);
                font-weight: 600;
                box-shadow: 0 1px 4px rgba(16, 24, 40, .12);
            }
        }

        .mc-tab-badge {
            min-width: 16px;
            height: 16px;
            padding: 0 5px;
            border-radius: 8px;
            background: var(--ce-accent);
            color: #fff;
            font-size: 11px;
            line-height: 16px;
            text-align: center;
            font-weight: 600;
        }
    }

    /* ---------- 消息列表 ---------- */
    .mc-body {
        min-height: 120px;
    }

    .mc-item {
        background: #fff;
        border: 1px solid var(--ce-border);
        border-radius: var(--ce-radius);
        padding: 14px 16px;
        margin-bottom: 10px;
        cursor: pointer;
        transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease;
        animation: mc-fade-up .4s ease both;

        &:hover {
            transform: translateY(-1px);
            border-color: #bfe0d8;
            box-shadow: 0 6px 18px rgba(16, 24, 40, .08);
        }

        &.unread {
            background: linear-gradient(90deg, var(--ce-primary-light) 0%, #fff 62%);
            border-left: 3px solid var(--ce-primary);
        }

        .mc-item-head {
            display: flex;
            align-items: center;
            gap: 8px;
            min-width: 0;
        }

        .mc-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: var(--ce-primary);
            flex-shrink: 0;
        }

        .mc-type {
            flex-shrink: 0;
        }

        .mc-item-title {
            flex: 1;
            min-width: 0;
            font-size: 14px;
            font-weight: 600;
            color: var(--ce-text);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .mc-item-date {
            flex-shrink: 0;
            font-size: 12px;
            color: #9aa3ad;
            font-variant-numeric: tabular-nums;
        }

        .mc-item-desc {
            margin: 6px 0 0 15px;
            font-size: 13px;
            color: var(--ce-text-2);
            line-height: 1.6;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }
    }

    .mc-footer {
        display: flex;
        justify-content: flex-end;
        margin-top: 16px;
    }

    .mc-empty {
        padding: 60px 0;
        text-align: center;
        color: #9aa3ad;

        .mc-empty-icon {
            width: 64px;
            height: 64px;
            margin: 0 auto 14px;
            border-radius: 50%;
            background: #f2f5f4;
            display: flex;
            align-items: center;
            justify-content: center;

            i {
                font-size: 30px;
                color: #c4cdd2;
            }
        }

        p {
            margin: 0;
            font-size: 14px;
        }
    }

    @keyframes mc-fade-up {
        from {
            opacity: 0;
            transform: translateY(8px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    @keyframes mc-pulse {
        0%,
        100% {
            opacity: 1;
        }
        50% {
            opacity: .35;
        }
    }
</style>
<style lang="less">
    /* 详情弹窗：append-to-body 渲染在 body 下，须用非 scoped 样式 */
    .mc-detail {
        border-radius: 16px;
        overflow: hidden;

        .el-dialog__header {
            padding: 18px 22px;
            background: linear-gradient(135deg, var(--ce-primary-light) 0%, #fff 72%);
            border-bottom: 1px solid var(--ce-border);
        }

        .el-dialog__headerbtn {
            top: 16px;

            .el-dialog__close {
                color: var(--ce-text-2);
                font-size: 18px;

                &:hover {
                    color: var(--ce-primary);
                }
            }
        }

        .el-dialog__body {
            padding: 20px 22px;
        }

        .mc-detail-title {
            display: flex;
            align-items: center;
            gap: 10px;
            min-width: 0;
            padding-right: 30px;

            .mc-detail-heading {
                font-size: 16px;
                font-weight: 700;
                color: var(--ce-text);
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
        }

        .mc-detail-body {
            .mc-detail-meta {
                display: flex;
                align-items: center;
                justify-content: space-between;
                font-size: 12px;
                color: #9aa3ad;
                margin-bottom: 14px;

                i {
                    margin-right: 4px;
                }

                .mc-detail-status {
                    padding: 2px 10px;
                    border-radius: 10px;
                    background: #fdf3e2;
                    color: #b4790a;
                    font-weight: 600;

                    &.read {
                        background: var(--ce-primary-light);
                        color: var(--ce-primary);
                    }
                }
            }

            .mc-detail-content {
                font-size: 14px;
                line-height: 1.8;
                color: var(--ce-text);
                white-space: pre-wrap;
                word-break: break-word;
                background: #fafbfc;
                border: 1px solid var(--ce-border);
                border-radius: var(--ce-radius);
                padding: 14px 16px;
                max-height: 320px;
                overflow-y: auto;
            }
        }

        .el-dialog__footer {
            padding: 12px 22px 18px;
        }
    }
</style>
