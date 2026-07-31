<template>
    <div class="progress-page">
        <el-card shadow="hover" class="info-card">
            <div slot="header">
                <span><i class="el-icon-s-flag"></i> 履约进度 · 任务 #{{ taskId }}</span>
            </div>
            <div v-if="task.description" class="task-brief">
                <div><b>委托内容：</b>{{ task.description }}</div>
                <div><b>地点：</b>{{ task.location }}</div>
                <div><b>状态：</b><el-tag size="small">{{ task.status }}</el-tag></div>
            </div>
            <el-empty v-if="!task.description && !loading" description="任务不存在"></el-empty>
        </el-card>

        <el-card shadow="hover" style="margin-top: 15px;">
            <div slot="header">
                <span><i class="el-icon-sort"></i> 三节点履约打卡</span>
            </div>
            <el-steps :active="activeStep" align-center>
                <el-step v-for="node in TASK_NODE_TYPES" :key="node.dbValue"
                    :title="node.label" :icon="node.icon" :description="stepDesc(node)">
                </el-step>
            </el-steps>
        </el-card>

        <el-card shadow="hover" style="margin-top: 15px;">
            <div slot="header">
                <span><i class="el-icon-tickets"></i> 打卡明细</span>
            </div>
            <div v-loading="loading">
                <el-empty v-if="nodeRecords.length === 0" description="暂无打卡记录"></el-empty>
                <div v-for="record in nodeRecords" :key="record.updateId" class="record-card">
                    <div class="record-header">
                        <i :class="nodeMeta(record.updateType).icon"></i>
                        <el-tag :color="nodeMeta(record.updateType).color" size="small" effect="dark">{{ nodeMeta(record.updateType).label }}</el-tag>
                        <span class="record-time">{{ formatDate(record.updateTime) }}</span>
                    </div>
                    <div v-if="record.updateDescription && record.updateDescription !== nodeMeta(record.updateType).label"
                        class="record-remark">{{ record.updateDescription }}</div>
                    <div v-if="record.location" class="record-location"><i class="el-icon-location-outline"></i> {{ record.location }}</div>
                    <el-image v-if="record.imgUrl" :src="record.imgUrl" class="record-img" fit="cover"
                        :preview-src-list="[record.imgUrl]"></el-image>
                </div>
            </div>
        </el-card>
    </div>
</template>
<script>
    import { listDelegateUpdateRecords } from '@/api/'
import { getTaskAndPublishUserInfoByTaskId } from '@/api/user.js'
    import { TASK_NODE_TYPES, getNodeMeta } from '@/utils/taskNode.js'
    import { SUCCESS_CODE } from '@/constants/http'
    export default {
        name: 'MyDelegationProgress',
        data() {
            return {
                taskId: this.$route.query.taskId || '',
                task: {},
                nodeRecords: [],
                loading: false,
                TASK_NODE_TYPES: TASK_NODE_TYPES
            }
        },
        computed: {
            activeStep() {
                let step = 0
                const set = new Set(this.nodeRecords.map(r => getNodeMeta(r.updateType) && getNodeMeta(r.updateType).dbValue))
                if (set.has('CONTACTED')) step = 1
                if (set.has('PICKED_UP')) step = 2
                if (set.has('DELIVERED')) step = 3
                return step
            }
        },
        created() {
            if (!this.taskId) {
                this.$message.warning('请从「我承接的订单」进入履约进度')
                this.$router.replace('/myDelegationAcceptList')
                return
            }
            this.loadData()
        },
        methods: {
            loadData() {
                this.loading = true
                getTaskAndPublishUserInfoByTaskId(this.taskId).then(res => {
                    if (res.data.code === SUCCESS_CODE) {
                        this.task = res.data.data.task || {}
                    }
                }).catch(err => {
                    console.error('获取任务详情失败：', err)
                })
                listDelegateUpdateRecords({ taskId: this.taskId, pageNum: 1, pageSize: 100 }).then(res => {
                    if (res.data.code === SUCCESS_CODE) {
                        const records = res.data.data.records || []
                        this.nodeRecords = records.filter(r => getNodeMeta(r.updateType))
                    } else {
                        this.$message.error(res.data.msg || '获取打卡记录失败')
                    }
                    this.loading = false
                }).catch(err => {
                    console.error('获取打卡记录失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                    this.loading = false
                })
            },
            nodeMeta(updateType) {
                return getNodeMeta(updateType)
            },
            stepDesc(node) {
                const record = this.nodeRecords.find(r => getNodeMeta(r.updateType) && getNodeMeta(r.updateType).dbValue === node.dbValue)
                return record ? (record.location || '已打卡') : '未打卡'
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
    .progress-page {
        padding: 10px;
    }

    .task-brief div {
        margin-bottom: 6px;
        color: #606266;
    }

    .record-card {
        border: 1px solid #ebeef5;
        border-radius: 6px;
        padding: 12px;
        margin-bottom: 10px;
    }

    .record-header {
        display: flex;
        align-items: center;
        gap: 8px;
    }

    .record-header i {
        font-size: 16px;
        color: #606266;
    }

    .record-time {
        color: #909399;
        font-size: 12px;
        margin-left: auto;
    }

    .record-remark {
        color: #303133;
        margin-top: 6px;
    }

    .record-location {
        color: #909399;
        font-size: 13px;
        margin-top: 4px;
    }

    .record-img {
        width: 120px;
        height: 120px;
        border-radius: 6px;
        border: 1px solid #ebeef5;
        margin-top: 8px;
    }
</style>
