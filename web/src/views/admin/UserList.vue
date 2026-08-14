<template>
    <list-shell title="用户管理" eyebrow="USER DIRECTORY" subtitle="用户账号、实名认证与启停管理"
        :count="total" count-label="个用户" :loading="loading" :total="total"
        :page.sync="userInfoConfig.pageNum" :page-size.sync="userInfoConfig.pageSize"
        :page-sizes="[5, 7, 10, 15]" @current-change="handleCurrentChange"
        @size-change="handleSizeChange">
        <template #toolbar>
            <div class="ul-toolbar">
                <div class="ul-filters">
                    <el-input v-model="userInfoConfig.username" placeholder="账号关键词" clearable class="ul-input"
                        @keyup.enter.native="getUserPage" />
                    <el-input v-model="userInfoConfig.email" placeholder="邮箱关键词" clearable class="ul-input"
                        @keyup.enter.native="getUserPage" />
                    <el-select v-model="userInfoConfig.isActive" placeholder="是否激活" clearable class="ul-select">
                        <el-option label="激活" value="TRUE" />
                        <el-option label="未激活" value="FALSE" />
                    </el-select>
                    <el-button type="primary" icon="el-icon-search" class="ul-btn ul-btn--primary"
                        @click="getUserPage">查询</el-button>
                </div>
                <div class="ul-actions">
                    <el-button icon="el-icon-delete" class="ul-btn ul-btn--danger" @click="deleteAndSelectAllAccounts">删除选中账号</el-button>
                    <el-button icon="el-icon-close" class="ul-btn ul-btn--ghost" @click="clearSelection">取消选择</el-button>
                    <el-button icon="el-icon-download" class="ul-btn ul-btn--ghost" @click="handleExportUser">导出 Excel</el-button>
                </div>
            </div>
        </template>

        <data-list ref="userDataList" :data="tableData" :loading="loading" mode="table" :config="listConfig"
            row-key="userId" @selection-change="handleSelectionChange">
            <template #cell-identityNo="{ row }">
                <span>{{ maskIdentity(row.identityNo) }}</span>
            </template>
            <template #cell-activeTime="{ row }">
                <span v-if="row.isActive === '已激活'">{{ row.activeTime | dateTime }}</span>
                <el-button v-else size="small" class="ul-link" @click="auxiliaryActivation(row.userId)">辅助激活</el-button>
            </template>
            <template #cell-authStatus="{ row }">
                <span v-if="row.role === 'admin'">管理员</span>
                <span v-else class="ul-badge" :class="authStatusTone(row.authStatus)">{{ row.authStatus }}</span>
            </template>
            <template #cell-operate="{ row }">
                <div class="ul-ops">
                    <el-button v-if="row.role !== 'admin'" type="warning" size="small" round
                        @click="handleDelete(row.userId)">删除</el-button>
                    <template v-if="row.authStatus === AUTH_STATUS.AUTHENTICATING">
                        <el-button type="primary" size="small"
                            @click="reviewAndCertificationInformation(row.userId)">审核认证信息</el-button>
                    </template>
                    <template v-else-if="row.authStatus === AUTH_STATUS.AUTHENTICATION_FAILED">
                        <el-button type="primary" size="small" disabled>认证失败</el-button>
                        <el-button type="primary" size="small"
                            @click="deleteRecords(row.userId)">删除认证记录</el-button>
                    </template>
                    <template v-else-if="row.authStatus === AUTH_STATUS.AUTHENTICATED">
                        <el-button type="primary" size="small"
                            @click="viewCertificationInformation(row.userId)">查看认证信息</el-button>
                    </template>
                    <el-button v-if="row.role !== 'ADMIN'" type="warning" size="small" round
                        @click="handleEnable(row)">{{ row.isEnabled }}</el-button>
                </div>
            </template>
        </data-list>

        <el-drawer :title="title" :before-close="handleClose" :visible.sync="dialog" direction="rtl"
            custom-class="demo-drawer" ref="drawer" size="40%" append-to-body>
            <el-card shadow="never">
                <el-form :model="form" label-position="left" size="small">
                    <el-form-item label="姓名" :label-width="formLabelWidth">
                        <span>{{ form.name }}</span>
                    </el-form-item>
                    <el-form-item label="手机号码" :label-width="formLabelWidth">
                        <span>{{ form.phoneNumber }}</span>
                    </el-form-item>
                    <el-form-item label="QQ 号码" :label-width="formLabelWidth">
                        <span>{{ form.qqNumber }}</span>
                    </el-form-item>
                    <el-form-item label="认证图片" :label-width="formLabelWidth">
                        <el-image style="width: 200px; height: 200px" :src="form.roleImgSrc" fit="fit"
                            :preview-src-list="srcList"></el-image>
                    </el-form-item>
                    <el-form-item label="用户角色" :label-width="formLabelWidth">
                        <span>{{ form.userRole }}</span>
                    </el-form-item>
                    <el-form-item label="认证时间" :label-width="formLabelWidth">
                        <span>{{ form.certifieTime | dateTime }}</span>
                    </el-form-item>
                    <el-form-item :label-width="formLabelWidth">
                        <div v-show="form.authStatus == AUTH_STATUS.AUTHENTICATING">
                            <div style="margin-bottom: 10px;">
                                <span class="el-form-item__label" style="width: 80px;">驳回原因</span>
                                <el-radio-group v-model="rejectReason">
                                    <el-radio label="照片不清晰">照片不清晰</el-radio>
                                    <el-radio label="学号/工号不匹配">学号/工号不匹配</el-radio>
                                    <el-radio label="身份信息不符">身份信息不符</el-radio>
                                    <el-radio label="重复提交">重复提交</el-radio>
                                    <el-radio label="其他">其他</el-radio>
                                </el-radio-group>
                                <el-input v-if="rejectReason === '其他'" v-model="rejectReasonOther"
                                    placeholder="请输入具体原因" style="margin-top: 6px;"></el-input>
                            </div>
                            <el-button @click="cancelForm" size="medium">认 证 不 通 过</el-button>
                            <el-button type="primary" @click="approvedCard()" size="medium">通 过 认 证</el-button>
                        </div>
                        <div v-show="form.authStatus == AUTH_STATUS.AUTHENTICATED">
                            <el-button type="warning" @click="cancelUserAuthentication(form.userId)"
                                size="medium">取消该用户认证</el-button>
                        </div>
                    </el-form-item>
                </el-form>
            </el-card>
        </el-drawer>
    </list-shell>
</template>

<script>
    import ListShell from '@/components/list/ListShell'
    import DataList from '@/components/list/DataList'
    import { getUserInfo, getUserList, confirmToPassTheReview, refuseToPassReview, deleteCertificationRecords, cancelUserInfoAuthentication, adminActivation, deleteAccounts, handleEnableAdmin, handleDisableAdmin, exportUserList } from '@/api';
    import { executeConfirmedRequest } from '@/utils/globalConfirmAction'
    import { AUTH_STATUS } from '@/constants/enums'

    export default {
        name: "UserList",
        components: { ListShell, DataList },
        data() {
            return {
                AUTH_STATUS,
                hide_on_single_page: true,
                userInfoConfig: {
                    username: "",
                    email: "",
                    isActive: "",
                    pageSize: 5,
                    pageNum: 1
                },
                total: 0,
                rejectReason: '照片不清晰',
                rejectReasonOther: '',
                authLevelFilters: [
                    { text: 'L2 校园卡', value: 2 },
                    { text: 'L1 实名', value: 1 },
                    { text: '未认证', value: 0 }
                ],
                creditScoreFilters: [
                    { text: '≥80 优秀', value: 'high' },
                    { text: '60-79 良好', value: 'mid' },
                    { text: '<60 待提升', value: 'low' }
                ],
                tableData: [],
                multipleSelection: [],
                loading: false,
                dialog: false,
                form: {
                    name: '',
                    phoneNumber: '',
                    qqNumber: '',
                    roleImgSrc: '',
                    userRole: '',
                    certifieTime: ''
                },
                srcList: [],
                formLabelWidth: '200px',
                timer: null,
                title: ''
            }
        },
        computed: {
            listConfig() {
                return [
                    { type: 'selection', width: 48 },
                    { label: '账号', field: 'username', width: 120 },
                    {
                        label: '角色', field: 'role', type: 'badge', width: 80,
                        badgeMap: {
                            ADMIN: { text: '管理员', tone: 'warning' },
                            admin: { text: '管理员', tone: 'warning' },
                            USER: { text: '用户', tone: 'info' },
                            user: { text: '用户', tone: 'info' }
                        }
                    },
                    {
                        label: '认证等级', field: 'authLevel', type: 'badge', width: 100,
                        filters: this.authLevelFilters, filterMethod: this.filterAuthLevel,
                        badgeMap: {
                            0: { text: '未认证', tone: 'info' },
                            1: { text: 'L1 实名', tone: 'default' },
                            2: { text: 'L2 校园卡', tone: 'warning' }
                        }
                    },
                    { label: '身份标识', field: 'identityNo', width: 140 },
                    {
                        label: '信用分', field: 'creditScore', width: 90,
                        filters: this.creditScoreFilters, filterMethod: this.filterCreditScore
                    },
                    { label: '邮箱', field: 'email', minWidth: 180 },
                    { label: '注册日期', field: 'createTime', type: 'date', width: 170 },
                    {
                        label: '是否激活', field: 'isActive', type: 'badge', width: 90,
                        badgeMap: {
                            '已激活': { text: '已激活', tone: 'success' },
                            '未激活': { text: '未激活', tone: 'info' }
                        }
                    },
                    { label: '激活日期', field: 'activeTime', width: 170 },
                    { label: '认证状态', field: 'authStatus', width: 100 },
                    { label: '操作', type: 'operate', width: 340 }
                ]
            }
        },
        mounted() {
            this.getUserPage();
        },
        methods: {
            handleExportUser() {
                exportUserList().then(res => {
                    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
                    const url = URL.createObjectURL(blob)
                    const a = document.createElement('a')
                    a.href = url
                    a.download = '用户列表.xlsx'
                    a.click()
                    URL.revokeObjectURL(url)
                }).catch(err => {
                    console.error('导出用户列表失败：', err)
                    this.$message.error('导出失败，请稍后重试')
                })
            },
            getUserPage() {
                this.loading = true;
                getUserList(this.userInfoConfig).then((res) => {
                    this.tableData = this.handleValue(res.data.data.records)
                    this.total = res.data.data.total
                    this.loading = false;
                }).catch(err => {
                    console.error('查询用户失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                    this.loading = false;
                });
            },
            handleValue(data) {
                (data || []).forEach(item => {
                    item.isActive = item.isActive == true ? "已激活" : "未激活"
                    item.isEnabled = item.isEnabled == false ? "启用" : "禁用"
                })
                return data
            },
            handleDelete(id) {
                deleteAccounts(id).then((res) => {
                    if (res.data.code === 1) {
                        this.$message.success(res.data.msg || '删除成功');
                        this.getUserPage()
                    } else {
                        this.$message.error(res.data.msg || '删除失败');
                    }
                }).catch(err => {
                    console.error('删除用户失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            clearSelection() {
                if (this.$refs.userDataList) {
                    this.$refs.userDataList.clearSelection();
                }
            },
            handleSelectionChange(val) {
                this.multipleSelection = val;
            },
            deleteAndSelectAllAccounts() {
                if (this.multipleSelection.length == 0) {
                    this.$message.warning('请选择要删除的用户');
                    return;
                }
                executeConfirmedRequest(
                    deleteAccounts,
                    this.multipleSelection.map(item => item.userId),
                    "确认删除选中的用户吗？",
                    "提示信息",
                    "操作成功",
                    "操作警告",
                    "操作失败，请稍后重试",
                    "操作已取消"
                ).then(ok => {
                    if (ok) this.getUserPage();
                });
            },
            handleSizeChange(val) {
                this.userInfoConfig.pageSize = val;
                this.getUserPage();
            },
            handleCurrentChange(val) {
                this.userInfoConfig.pageNum = val;
                this.getUserPage();
            },
            reviewAndCertificationInformation(id) {
                this.dialog = true;
                this.title = "审核认证信息"
                getUserInfo(id).then((res) => {
                    if (res.data.code == 1) {
                        const info = res.data.data
                        info.roleImgSrc = "http://" + info.roleImgSrc
                        this.srcList = [info.roleImgSrc]
                        this.form = info
                    } else {
                        this.dialog = false;
                        this.$message.warning('获取认证信息失败');
                    }
                }).catch(err => {
                    console.error('获取认证信息失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            handleClose() {
                if (this.loading) return;
                this.loading = false;
                this.dialog = false;
                clearTimeout(this.timer);
            },
            viewCertificationInformation(id) {
                this.dialog = true;
                this.title = "已认证审核信息"
                getUserInfo(id).then((res) => {
                    if (res.data.code == 1) {
                        const info = res.data.data
                        info.roleImgSrc = "http://" + info.roleImgSrc
                        this.srcList = [info.roleImgSrc]
                        this.form = info
                    } else {
                        this.dialog = false;
                        this.$message.warning('获取认证信息失败');
                    }
                }).catch(err => {
                    console.error('获取认证信息失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            maskIdentity(no) {
                if (!no) return '—'
                if (no.length <= 6) return no.slice(0, 2) + '****'
                return no.slice(0, 4) + '*****' + no.slice(-2)
            },
            filterAuthLevel(value, row) {
                return row.authLevel === value
            },
            filterCreditScore(value, row) {
                const s = row.creditScore != null ? row.creditScore : 60
                if (value === 'high') return s >= 80
                if (value === 'mid') return s >= 60 && s < 80
                return s < 60
            },
            authStatusTone(status) {
                const map = {
                    [AUTH_STATUS.UNAUTHORIZED]: 'ul-badge--info',
                    [AUTH_STATUS.AUTHENTICATING]: 'ul-badge--warning',
                    [AUTH_STATUS.AUTHENTICATION_FAILED]: 'ul-badge--danger',
                    [AUTH_STATUS.AUTHENTICATED]: 'ul-badge--success'
                };
                return map[status] || 'ul-badge--info';
            },
            async cancelForm() {
                const reason = this.rejectReason === '其他' ? (this.rejectReasonOther || '其他') : this.rejectReason
                const ok = await this.$confirm('是否确认拒绝通过审核？', '提示', {
                    confirmButtonText: '确认驳回',
                    cancelButtonText: '取消',
                    type: 'warning'
                }).then(() => true).catch(() => false)
                if (!ok) return
                const res = await refuseToPassReview(this.form.userId, reason)
                if (res.data && res.data.code === 1) {
                    this.$message.success('操作成功')
                }
                this.handleClose();
                this.getUserPage()
            },
            async approvedCard() {
                const ok = await executeConfirmedRequest(confirmToPassTheReview, this.form.userId);
                if (ok) {
                    this.handleClose();
                    this.getUserPage()
                }
            },
            async deleteRecords(id) {
                const ok = await executeConfirmedRequest(deleteCertificationRecords, this.form.userId, "是否确认删除该认证记录？", "提示", "警告", "操作警告", "操作失败，请稍后重试", "操作已取消");
                if (ok) {
                    this.handleClose();
                    this.getUserPage()
                }
            },
            async cancelUserAuthentication() {
                const ok = await executeConfirmedRequest(cancelUserInfoAuthentication, this.form.userId, "是否确认取消该用户认证？", "提示", "警告", "操作警告", "操作失败，请稍后重试", "操作已取消");
                if (ok) {
                    this.handleClose();
                    this.getUserPage()
                }
            },
            async auxiliaryActivation(id) {
                const ok = await executeConfirmedRequest(adminActivation, id, "是否确认激活该用户账号用于登录网站使用？", "提示", "警告", "操作警告", "操作失败，请稍后重试", "操作已取消");
                if (ok) {
                    this.getUserPage()
                }
            },
            async handleEnable(data) {
                let ok = false;
                if (data.isEnabled === '禁用') {
                    ok = await executeConfirmedRequest(handleDisableAdmin, data.userId, "是否确认禁用该用户账号？", "提示", "警告", "操作警告", "操作失败，请稍后重试", "操作已取消");
                } else {
                    ok = await executeConfirmedRequest(handleEnableAdmin, data.userId, "是否确认启用该用户账号？", "提示", "警告", "操作警告", "操作失败，请稍后重试", "操作已取消");
                }
                if (ok) {
                    this.getUserPage()
                }
            }
        }
    }
</script>

<style lang="less" scoped>
    .ul-toolbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 12px;
        width: 100%;
    }

    .ul-filters,
    .ul-actions {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;
    }

    .ul-input {
        width: 180px;
    }

    .ul-select {
        width: 130px;
    }

    .ul-input :deep(.el-input__inner),
    .ul-select :deep(.el-input__inner) {
        height: 34px;
        background: rgba(255, 252, 245, .72);
        border: 1px solid #e6ddc9;
        border-radius: 8px;
        color: #2a3a30;
        font-family: inherit;
        font-size: 13px;
        transition: border-color .2s, box-shadow .2s, background .2s;
    }

    .ul-input :deep(.el-input__inner:focus),
    .ul-select :deep(.el-input__inner:focus) {
        border-color: #b9892c;
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13);
        background: #fffcf5;
    }

    .ul-btn {
        height: 34px;
        padding: 0 16px;
        border-radius: 8px;
        font-family: inherit;
        letter-spacing: .05em;
        font-size: 13px;
        transition: transform .18s, box-shadow .18s, background .18s, border-color .18s;
    }

    .ul-btn--primary {
        background: #2a3a30;
        border-color: #2a3a30;
        color: #f7f3ea;
    }

    .ul-btn--primary:hover {
        background: #33493c;
        border-color: #33493c;
        color: #fff;
        transform: translateY(-1px);
    }

    .ul-btn--ghost {
        background: transparent;
        border: 1px solid #e6ddc9;
        color: #5f6b62;
    }

    .ul-btn--ghost:hover {
        border-color: #b9892c;
        color: #96701f;
    }

    .ul-btn--danger {
        background: transparent;
        border: 1px solid rgba(180, 84, 58, .4);
        color: #b4543a;
    }

    .ul-btn--danger:hover {
        background: rgba(180, 84, 58, .08);
        border-color: #b4543a;
    }

    .ul-ops {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 6px;
    }

    .ul-link {
        color: #96701f;
    }

    .ul-badge {
        display: inline-flex;
        align-items: center;
        font-size: 11px;
        letter-spacing: .06em;
        padding: 2px 10px;
        border-radius: 999px;
    }

    .ul-badge--success {
        background: #dce8e0;
        color: #3e6b50;
    }

    .ul-badge--warning {
        background: #f1e9d3;
        color: #8a6d26;
    }

    .ul-badge--danger {
        background: #f1e3dc;
        color: #a0523b;
    }

    .ul-badge--info {
        background: #ece8de;
        color: #807a68;
    }
</style>
