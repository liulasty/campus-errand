<template>
    <div class="message-center">
        <el-card shadow="hover">
            <div slot="header" class="clearfix">
                <span><i class="el-icon-bell"></i> 消息中心</span>
                <el-tabs v-model="activeTab" class="msg-tabs" @tab-click="handleTab">
                    <el-tab-pane label="全部" name="all"></el-tab-pane>
                    <el-tab-pane label="未读" name="unread"></el-tab-pane>
                </el-tabs>
            </div>
            <div v-loading="loading">
                <el-empty v-if="list.length === 0" description="暂无消息"></el-empty>
                <div v-for="item in list" :key="item.id" class="msg-item" :class="{ unread: !item.isRead }"
                    @click="handleRead(item)">
                    <div class="msg-title">
                        <span v-if="!item.isRead" class="dot"></span>
                        <span class="title">{{ item.title }}</span>
                        <el-tag v-if="item.notificationType" size="mini" type="info">{{ item.notificationType }}</el-tag>
                    </div>
                    <div class="msg-desc">{{ item.description }}</div>
                    <div class="msg-date">{{ formatDate(item.date) }}</div>
                </div>
                <el-pagination v-if="total > 0" @current-change="handleCurrentChange" :current-page="queryParams.pageNum"
                    :page-size="queryParams.pageSize" layout="total, prev, pager, next" :total="total" background
                    style="text-align:right; margin-top: 15px;">
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
<style scoped>
    .message-center {
        padding: 10px;
    }

    .msg-tabs {
        display: inline-block;
        margin-left: 24px;
        vertical-align: middle;
    }

    .msg-item {
        padding: 12px 8px;
        border-bottom: 1px dashed #ebeef5;
        cursor: pointer;
        transition: background 0.2s;
    }

    .msg-item:hover {
        background: #f5f7fa;
    }

    .msg-item.unread {
        background: #f0f7ff;
    }

    .msg-title {
        display: flex;
        align-items: center;
        gap: 8px;
    }

    .msg-title .title {
        font-weight: 600;
        color: #303133;
    }

    .dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #f56c6c;
        display: inline-block;
    }

    .msg-desc {
        color: #909399;
        font-size: 13px;
        margin-top: 4px;
    }

    .msg-date {
        color: #c0c4cc;
        font-size: 12px;
        margin-top: 4px;
    }
</style>
