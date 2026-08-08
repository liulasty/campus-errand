<template>
    <div class="message-center">
        <el-card shadow="never" class="msg-card">
            <div slot="header" class="clearfix">
                <span class="card-title"><i class="el-icon-bell"></i> 消息中心</span>
            </div>
            <el-tabs v-model="activeTab" class="msg-tabs" @tab-click="handleTab">
                <el-tab-pane label="全部" name="all"></el-tab-pane>
                <el-tab-pane label="未读" name="unread"></el-tab-pane>
            </el-tabs>
            <div v-loading="loading" class="msg-body">
                <el-empty v-if="list.length === 0" description="暂无消息"></el-empty>
                <div v-for="item in list" :key="item.id" class="msg-item" :class="{ unread: !item.isRead }"
                    @click="handleRead(item)">
                    <div class="msg-title">
                        <span v-if="!item.isRead" class="dot"></span>
                        <span class="title">{{ item.title }}</span>
                        <el-tag v-if="item.notificationType" size="mini" class="type-tag">{{ item.notificationType }}</el-tag>
                    </div>
                    <div class="msg-desc">{{ item.description }}</div>
                    <div class="msg-date">{{ formatDate(item.date) }}</div>
                </div>
                <el-pagination v-if="total > 0" @current-change="handleCurrentChange" :current-page="queryParams.pageNum"
                    :page-size="queryParams.pageSize" layout="total, prev, pager, next" :total="total" background
                    class="msg-pagination">
                </el-pagination>
            </div>
        </el-card>
    </div>
</template>
<script>
    import { getMyNotifications, markNotificationRead } from '@/api/'
    export default {
        name: 'MessageCenter',
        data() {
            return {
                activeTab: 'all',
                list: [],
                total: 0,
                loading: false,
                queryParams: {
                    pageNum: 1,
                    pageSize: 10
                }
            }
        },
        created() {
            this.getList()
        },
        methods: {
            getList() {
                this.loading = true
                const params = { ...this.queryParams }
                if (this.activeTab === 'unread') {
                    // 后端暂不支持未读过滤，前端过滤后分页
                    params.pageSize = 100
                }
                getMyNotifications(params).then(response => {
                    if (response.data.code === 1) {
                        let records = response.data.data.records
                        if (this.activeTab === 'unread') {
                            records = records.filter(item => !item.isRead)
                            this.total = records.length
                        } else {
                            this.total = response.data.data.total
                        }
                        this.list = records.slice((this.queryParams.pageNum - 1) * this.queryParams.pageSize,
                            this.queryParams.pageNum * this.queryParams.pageSize)
                        this.loading = false
                    } else {
                        this.$message.error(response.data.msg || '获取消息失败')
                        this.loading = false
                    }
                }).catch(err => {
                    console.error('获取消息失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                    this.loading = false
                })
            },
            handleTab() {
                this.queryParams.pageNum = 1
                this.getList()
            },
            handleCurrentChange(val) {
                this.queryParams.pageNum = val
                this.getList()
            },
            handleRead(item) {
                if (!item.isRead) {
                    markNotificationRead(item.id).then(() => {
                        item.isRead = true
                    }).catch(err => {
                        console.error('标记已读失败：', err)
                    })
                }
                this.$alert(item.description, item.title, { confirmButtonText: '知道了' })
            },
            formatDate(date) {
                if (!date) return ''
                const d = new Date(date)
                const pad = n => (n < 10 ? '0' + n : n)
                return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
            }
        }
    }
</script>
<style lang="less" scoped>
    .message-center {
        padding: 12px;
    }

    .msg-card {
        border-radius: 12px;

        .card-title {
            font-weight: 600;
            color: var(--ce-text);

            i {
                margin-right: 6px;
                color: var(--ce-primary);
            }
        }
    }

    .msg-tabs {
        margin-bottom: 8px;

        /deep/ .el-tabs__item.is-active {
            color: var(--ce-primary);
        }

        /deep/ .el-tabs__active-bar {
            background-color: var(--ce-primary);
        }
    }

    .msg-body {
        min-height: 60px;
    }

    .msg-item {
        padding: 14px 12px;
        border-radius: 10px;
        border-bottom: 1px dashed var(--ce-border);
        cursor: pointer;
        transition: background .2s;

        &:hover {
            background: #f7faf9;
        }

        &.unread {
            background: #f0f7f5;
            border-left: 3px solid var(--ce-primary);
        }

        .msg-title {
            display: flex;
            align-items: center;
            gap: 8px;

            .title {
                font-weight: 600;
                color: var(--ce-text);
            }

            .type-tag {
                flex-shrink: 0;
            }
        }

        .dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: var(--ce-accent);
            display: inline-block;
            flex-shrink: 0;
        }

        .msg-desc {
            color: var(--ce-text-2);
            font-size: 13px;
            margin-top: 4px;
            line-height: 1.6;
        }

        .msg-date {
            color: #9aa3ad;
            font-size: 12px;
            margin-top: 4px;
        }
    }

    .msg-pagination {
        text-align: right;
        margin-top: 15px;
    }
</style>
