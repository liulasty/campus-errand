<template>
    <div class="realname-audit-page">
        <el-card shadow="hover">
            <div slot="header" class="clearfix">
                <span><i class="el-icon-postcard"></i> 实名认证审核</span>
            </div>
            <el-tabs v-model="activeTab" @tab-click="loadData">
                <el-tab-pane label="待审核" name="AUTHENTICATING"></el-tab-pane>
                <el-tab-pane label="已驳回" name="AUTHENTICATION_FAILED"></el-tab-pane>
                <el-tab-pane label="已通过" name="AUTHENTICATED"></el-tab-pane>
                <el-tab-pane label="全部" name="ALL"></el-tab-pane>
            </el-tabs>
            <el-table :data="filteredList" v-loading="loading" border stripe>
                <el-table-column label="姓名" prop="name" width="120" />
                <el-table-column label="角色" width="90">
                    <template slot-scope="scope">{{ roleName(scope.row.userRole) }}</template>
                </el-table-column>
                <el-table-column label="身份标识" width="140">
                    <template slot-scope="scope">{{ maskIdentity(scope.row.identityNo) }}</template>
                </el-table-column>
                <el-table-column label="证件照片" width="80">
                    <template slot-scope="scope">
                        <el-image v-if="scope.row.roleImgSrc" :src="scope.row.roleImgSrc" fit="cover"
                            :preview-src-list="[scope.row.roleImgSrc]"
                            style="width: 50px; height: 50px; border-radius: 4px;"></el-image>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column label="提交时间" width="150">
                    <template slot-scope="scope">{{ scope.row.certifieTime | dateTime }}</template>
                </el-table-column>
                <el-table-column label="操作" width="180">
                    <template slot-scope="scope">
                        <el-button v-if="activeTab === 'AUTHENTICATING'" type="primary" size="small"
                            @click="approve(scope.row)">通过</el-button>
                        <el-button v-if="activeTab === 'AUTHENTICATING'" type="danger" size="small"
                            @click="reject(scope.row)">驳回</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>
<script>
    import { getUserList, confirmToPassTheReview, refuseToPassReview } from '@/api/'
    import { SUCCESS_CODE } from '@/constants/http'
    export default {
        name: 'RealNameAudit',
        data() {
            return {
                activeTab: 'AUTHENTICATING',
                allUsers: [],
                loading: false
            }
        },
        computed: {
            filteredList() {
                if (this.activeTab === 'ALL') return this.allUsers
                return this.allUsers.filter(u => u.authStatus === this.activeTab)
            }
        },
        created() {
            this.loadData()
        },
        methods: {
            loadData() {
                this.loading = true
                getUserList({ page: 1, size: 200 }).then(res => {
                    if (res.data.code === SUCCESS_CODE) {
                        this.allUsers = res.data.data.records || []
                    }
                    this.loading = false
                }).catch(err => {
                    console.error('获取实名申请失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                    this.loading = false
                })
            },
            roleName(role) {
                const map = { student: '学生', teacher: '教师', other: '其他' }
                return map[role] || role || '—'
            },
            maskIdentity(no) {
                if (!no) return '—'
                if (no.length <= 6) return no.slice(0, 2) + '****'
                return no.slice(0, 4) + '*****' + no.slice(-2)
            },
            approve(row) {
                this.$confirm('确认通过该用户实名认证？', '提示', { type: 'success' }).then(() => {
                    confirmToPassTheReview(row.userId).then(res => {
                        if (res.data.code === SUCCESS_CODE) {
                            this.$message.success('认证已通过')
                            this.loadData()
                        } else {
                            this.$message.error(res.data.msg || '操作失败')
                        }
                    }).catch(err => {
                        console.error('通过认证失败：', err)
                        this.$message.error('请求异常，请稍后重试')
                    })
                }).catch(() => {})
            },
            reject(row) {
                this.$prompt('请输入驳回原因', '驳回认证', {
                    confirmButtonText: '确认驳回',
                    cancelButtonText: '取消',
                    inputValidator: v => !!v || '请输入驳回原因'
                }).then(({ value }) => {
                    refuseToPassReview(row.userId, value).then(res => {
                        if (res.data.code === SUCCESS_CODE) {
                            this.$message.success('已驳回')
                            this.loadData()
                        } else {
                            this.$message.error(res.data.msg || '操作失败')
                        }
                    }).catch(err => {
                        console.error('驳回认证失败：', err)
                        this.$message.error('请求异常，请稍后重试')
                    })
                }).catch(() => {})
            }
        }
    }
</script>
<style scoped>
    .realname-audit-page {
        padding: 10px;
    }
</style>
