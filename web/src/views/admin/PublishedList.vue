<template>
    <div class="published-list">
        <el-card shadow="hover">
            <div slot="header" class="clearfix">
                <span><i class="el-icon-s-order"></i> 全部委托列表</span>
                <el-button type="success" icon="el-icon-download" size="mini" style="float: right;" @click="handleExport">导出 Excel</el-button>
            </div>
            <el-table v-loading="loading" :data="list" stripe border>
                <el-table-column prop="taskId" label="任务ID" width="80" align="center" />
                <el-table-column label="发布者" width="110" align="center">
                    <template slot-scope="scope">{{ scope.row.ownerName || scope.row.ownerId }}</template>
                </el-table-column>
                <el-table-column label="信用分" width="80" align="center">
                    <template slot-scope="scope">
                        <el-tag :type="(scope.row.ownerCredit != null ? scope.row.ownerCredit : 60) >= 80 ? 'success' : (scope.row.ownerCredit != null ? scope.row.ownerCredit : 60) >= 60 ? 'primary' : 'info'"
                            size="small">{{ scope.row.ownerCredit != null ? scope.row.ownerCredit : 60 }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="类型" width="110" align="center">
                    <template slot-scope="scope">
                        <el-tag size="small">{{ typeName(scope.row.taskType) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="description" label="内容" show-overflow-tooltip />
                <el-table-column prop="location" label="地点" width="100" />
                <el-table-column prop="money" label="金额" width="80" align="center" />
                <el-table-column prop="status" label="状态" width="100" align="center" />
                <el-table-column label="发布时间" width="150">
                    <template slot-scope="scope">{{ scope.row.startTime | dateTime }}</template>
                </el-table-column>
                <el-table-column label="截止时间" width="150">
                    <template slot-scope="scope">{{ scope.row.endTime | dateTime }}</template>
                </el-table-column>
            </el-table>
            <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
                :current-page="queryParams.pageNum" :page-sizes="[5, 10, 20, 50]" :page-size="queryParams.pageSize"
                layout="total, sizes, prev, pager, next, jumper" :total="total" background
                style="text-align: right; margin-top: 15px;">
            </el-pagination>
        </el-card>
    </div>
</template>
<script>
    import { listDelegateRecords, exportTaskList, getTaskCategories } from '@/api/'
    export default {
        name: 'PublishedList',
        data() {
            return {
                list: [],
                total: 0,
                loading: false,
                typeMap: {},
                queryParams: {
                    pageNum: 1,
                    pageSize: 10
                }
            }
        },
        created() {
            this.loadTypes()
            this.getList()
        },
        methods: {
            loadTypes() {
                getTaskCategories().then(res => {
                    if (res.data.code === 1) {
                        const map = {}
                        res.data.data.forEach(c => { map[c.id] = c.name })
                        this.typeMap = map
                    }
                }).catch(err => {
                    console.error('加载委托分类失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            typeName(id) {
                return this.typeMap[id] || '未知'
            },
            getList() {
                this.loading = true
                listDelegateRecords(this.queryParams).then(res => {
                    if (res.data.code === 1) {
                        this.list = res.data.data.records
                        this.total = res.data.data.total
                    }
                    this.loading = false
                }).catch(err => {
                    console.error('获取委托列表失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                    this.loading = false
                })
            },
            handleSizeChange(val) {
                this.queryParams.pageSize = val
                this.getList()
            },
            handleCurrentChange(val) {
                this.queryParams.pageNum = val
                this.getList()
            },
            handleExport() {
                exportTaskList().then(res => {
                    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
                    const url = URL.createObjectURL(blob)
                    const a = document.createElement('a')
                    a.href = url
                    a.download = '委托列表.xlsx'
                    a.click()
                    URL.revokeObjectURL(url)
                }).catch(err => {
                    console.error('导出委托列表失败：', err)
                    this.$message.error('导出失败，请稍后重试')
                })
            }
        }
    }
</script>
<style scoped>
    .published-list {
        padding: 10px;
    }
</style>
