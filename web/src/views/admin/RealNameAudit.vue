<template>
    <div class="realname-audit-page">
        <div class="ce-page-head">
            <div class="rna-head-left">
                <div class="rna-head-icon"><i class="el-icon-postcard"></i></div>
                <div>
                    <h2>实名认证审核</h2>
                    <p>审核用户的实名认证申请，通过后用户获得 L1 委托操作权限</p>
                </div>
            </div>
            <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadData">刷新</el-button>
        </div>

        <div class="rna-stat-grid">
            <div v-for="s in statCards" :key="s.name" class="rna-stat-card" :class="[s.cls, { 'is-active': activeTab === s.name }]"
                @click="switchTab(s.name)">
                <div class="rna-stat-icon"><i :class="s.icon"></i></div>
                <div class="rna-stat-info">
                    <div class="rna-stat-num">{{ statCount(s.name) }}</div>
                    <div class="rna-stat-label">{{ s.label }}</div>
                </div>
            </div>
        </div>

        <div class="rna-table-card">
            <el-table :data="records" v-loading="loading" stripe>
                <el-table-column label="用户" min-width="200">
                    <template slot-scope="scope">
                        <div class="rna-user-cell">
                            <div class="rna-avatar">{{ nameInitial(scope.row.name) }}</div>
                            <div>
                                <div class="rna-user-name">{{ scope.row.name }}</div>
                                <span class="rna-role-tag" :class="roleCls(scope.row.userRole)">{{ roleName(scope.row.userRole) }}</span>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="身份标识" width="180">
                    <template slot-scope="scope"><span class="rna-mono">{{ maskIdentity(scope.row.identityNo) }}</span></template>
                </el-table-column>
                <el-table-column label="证件照片" width="90" align="center">
                    <template slot-scope="scope">
                        <el-image v-if="scope.row.roleImgSrc" :src="photoSrc(scope.row.roleImgSrc)" fit="cover"
                            :preview-src-list="[photoSrc(scope.row.roleImgSrc)]" class="rna-photo">
                            <div slot="error" class="rna-photo-error">
                                <i class="el-icon-picture-outline"></i>
                            </div>
                        </el-image>
                        <span v-else class="rna-empty">—</span>
                    </template>
                </el-table-column>
                <el-table-column label="提交时间" width="160">
                    <template slot-scope="scope">{{ scope.row.certifieTime | dateTime }}</template>
                </el-table-column>
                <el-table-column label="状态" width="110" align="center">
                    <template slot-scope="scope">
                        <span class="rna-status-tag" :class="statusCls(scope.row.authStatus)">{{ statusText(scope.row.authStatus) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center">
                    <template slot-scope="scope">
                        <template v-if="scope.row.authStatus === '认证中'">
                            <el-button type="primary" size="small" icon="el-icon-check" @click="approve(scope.row)">通过</el-button>
                            <el-button type="danger" size="small" icon="el-icon-close" @click="reject(scope.row)">驳回</el-button>
                        </template>
                        <span v-else class="rna-empty">—</span>
                    </template>
                </el-table-column>
                <template slot="empty">
                    <div class="rna-empty-wrap">
                        <i class="el-icon-document"></i>
                        <p>暂无数据</p>
                    </div>
                </template>
            </el-table>
            <el-pagination class="rna-pagination" background layout="total, sizes, prev, pager, next, jumper"
                :total="total" :current-page="pageNum" :page-size="pageSize" :page-sizes="[10, 20, 50]"
                @size-change="handleSizeChange" @current-change="handleCurrentChange">
            </el-pagination>
        </div>
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
                records: [],
                total: 0,
                pageNum: 1,
                pageSize: 10,
                loading: false,
                statCounts: { AUTHENTICATING: 0, AUTHENTICATED: 0, AUTHENTICATION_FAILED: 0, ALL: 0 },
                statCards: [
                    { name: 'AUTHENTICATING', label: '待审核', cls: 'rna-st-warn', icon: 'el-icon-time' },
                    { name: 'AUTHENTICATED', label: '已通过', cls: 'rna-st-ok', icon: 'el-icon-circle-check' },
                    { name: 'AUTHENTICATION_FAILED', label: '已驳回', cls: 'rna-st-fail', icon: 'el-icon-circle-close' },
                    { name: 'ALL', label: '全部', cls: 'rna-st-all', icon: 'el-icon-tickets' }
                ]
            }
        },
        created() {
            this.loadData()
        },
        methods: {
            loadData() {
                this.loading = true
                const seq = (this._loadSeq = (this._loadSeq || 0) + 1)
                const authStatus = this.activeTab === 'ALL' ? undefined : this.activeTab
                getUserList({ pageNum: this.pageNum, pageSize: this.pageSize, authStatus }).then(res => {
                    if (seq !== this._loadSeq) return
                    if (res.data.code === SUCCESS_CODE) {
                        this.records = res.data.data.records || []
                        this.total = res.data.data.total || 0
                    }
                    this.loading = false
                }).catch(err => {
                    if (seq !== this._loadSeq) return
                    console.error('获取实名申请失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                    this.loading = false
                })
                this.loadStatCounts()
            },
            loadStatCounts() {
                const tabs = ['AUTHENTICATING', 'AUTHENTICATED', 'AUTHENTICATION_FAILED']
                Promise.all(tabs.map(t => this.countByStatus(t))).then(([a, b, c]) => {
                    this.statCounts.AUTHENTICATING = a
                    this.statCounts.AUTHENTICATED = b
                    this.statCounts.AUTHENTICATION_FAILED = c
                })
                this.countByStatus(undefined).then(n => { this.statCounts.ALL = n })
            },
            countByStatus(authStatus) {
                return getUserList({ pageNum: 1, pageSize: 1, authStatus }).then(res =>
                    res.data.code === SUCCESS_CODE ? (res.data.data.total || 0) : 0
                ).catch(() => 0)
            },
            switchTab(name) {
                if (this.activeTab !== name) {
                    this.activeTab = name
                    this.pageNum = 1
                    this.loadData()
                }
            },
            statCount(name) {
                if (name === 'ALL') return this.statCounts.ALL
                return this.statCounts[name] || 0
            },
            handleSizeChange(size) {
                this.pageSize = size
                this.pageNum = 1
                this.loadData()
            },
            handleCurrentChange(page) {
                this.pageNum = page
                this.loadData()
            },
            nameInitial(name) {
                return name ? name.charAt(0) : '?'
            },
            photoSrc(src) {
                if (!src) return ''
                return /^https?:\/\//.test(src) ? src : 'http://' + src
            },
            roleName(role) {
                const map = { student: '学生', teacher: '教师', other: '其他' }
                return map[role] || role || '—'
            },
            roleCls(role) {
                const map = { student: 'rna-role-student', teacher: 'rna-role-teacher', other: 'rna-role-other' }
                return map[role] || 'rna-role-other'
            },
            maskIdentity(no) {
                if (!no) return '—'
                if (no.length <= 6) return no.slice(0, 2) + '****'
                return no.slice(0, 4) + '*****' + no.slice(-2)
            },
            statusMeta(status) {
                const map = {
                    '认证中': { text: '待审核', cls: 'rna-status-pending' },
                    '认证通过': { text: '已通过', cls: 'rna-status-ok' },
                    '认证失败': { text: '已驳回', cls: 'rna-status-fail' },
                    '未认证': { text: '未认证', cls: 'rna-status-unauth' }
                }
                return map[status] || { text: status || '—', cls: '' }
            },
            statusText(status) {
                return this.statusMeta(status).text
            },
            statusCls(status) {
                return this.statusMeta(status).cls
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
        padding: 14px;
    }

    /* ---------- 页头 ---------- */
    .ce-page-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 14px;
    }
    .rna-head-left {
        display: flex;
        align-items: center;
        gap: 12px;
    }
    .rna-head-icon {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        background: var(--ce-primary-light);
        color: var(--ce-primary);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
        flex-shrink: 0;
    }
    .rna-head-left h2 {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: var(--ce-text);
    }
    .rna-head-left p {
        margin: 4px 0 0;
        font-size: 13px;
        color: var(--ce-text-2);
    }

    /* ---------- 统计卡（兼状态切换） ---------- */
    .rna-stat-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 12px;
        margin-bottom: 16px;
    }
    .rna-stat-card {
        display: flex;
        align-items: center;
        gap: 12px;
        background: #fff;
        border: 1px solid var(--ce-border);
        border-radius: 12px;
        padding: 14px 16px;
        cursor: pointer;
        box-shadow: var(--ce-shadow);
        transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease, background-color .25s ease;
    }
    .rna-stat-card:hover {
        transform: translateY(-2px);
        box-shadow: var(--ce-shadow-md);
    }
    .rna-stat-card.is-active {
        border-color: var(--ce-primary);
        background: var(--ce-primary-light);
    }
    .rna-stat-icon {
        width: 40px;
        height: 40px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        flex-shrink: 0;
    }
    .rna-stat-num {
        font-size: 22px;
        font-weight: 700;
        line-height: 1;
        color: var(--ce-text);
    }
    .rna-stat-label {
        font-size: 12px;
        color: var(--ce-text-2);
        margin-top: 4px;
    }
    .rna-st-warn .rna-stat-icon { background: #fdf3e2; color: #b4790a; }
    .rna-st-ok .rna-stat-icon { background: var(--ce-primary-light); color: var(--ce-primary); }
    .rna-st-fail .rna-stat-icon { background: #fdecec; color: #c13c3c; }
    .rna-st-all .rna-stat-icon { background: #eef1f5; color: #4b5563; }

    /* ---------- 表格卡片 ---------- */
    .rna-table-card {
        background: #fff;
        border: 1px solid var(--ce-border);
        border-radius: 12px;
        box-shadow: var(--ce-shadow);
        padding: 6px 16px 16px;
    }

    /* 用户列：头像 + 姓名 + 角色 */
    .rna-user-cell {
        display: flex;
        align-items: center;
        gap: 10px;
    }
    .rna-avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: var(--ce-primary-light);
        color: var(--ce-primary);
        font-weight: 600;
        font-size: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
    }
    .rna-user-name {
        font-weight: 600;
        color: var(--ce-text);
        line-height: 1.4;
    }
    .rna-role-tag {
        display: inline-flex;
        align-items: center;
        padding: 1px 8px;
        border-radius: 6px;
        font-size: 12px;
        line-height: 18px;
        margin-top: 2px;
    }
    .rna-role-student { background: var(--ce-primary-light); color: var(--ce-primary); }
    .rna-role-teacher { background: #fdf3e2; color: #b4790a; }
    .rna-role-other { background: #eef1f5; color: #4b5563; }

    /* 身份标识（等宽脱敏） */
    .rna-mono {
        font-family: Consolas, Menlo, monospace;
        letter-spacing: .5px;
        color: #4b5563;
    }

    /* 证件照片 */
    .rna-photo {
        width: 52px;
        height: 52px;
        border-radius: 8px;
        border: 1px solid var(--ce-border);
    }
    .rna-photo-error {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f6f7f9;
        color: #b6bcc4;
        font-size: 20px;
    }

    /* 状态胶囊 */
    .rna-status-tag {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 3px 10px;
        border-radius: 20px;
        font-size: 12px;
        font-weight: 500;
        white-space: nowrap;
    }
    .rna-status-pending { background: #fdf3e2; color: #b4790a; }
    .rna-status-ok { background: var(--ce-primary-light); color: var(--ce-primary); }
    .rna-status-fail { background: #fdecec; color: #c13c3c; }
    .rna-status-unauth { background: #eef1f5; color: #4b5563; }

    .rna-empty {
        color: var(--ce-text-2);
    }

    /* 空状态 */
    .rna-empty-wrap {
        padding: 40px 0;
        color: var(--ce-text-2);
        text-align: center;
    }
    .rna-empty-wrap i {
        font-size: 40px;
        color: #d5dae0;
        display: block;
        margin-bottom: 8px;
    }
    .rna-empty-wrap p {
        margin: 0;
        font-size: 13px;
    }

    .rna-pagination {
        display: flex;
        justify-content: flex-end;
        margin-top: 14px;
    }

    /* 入场动画 */
    @keyframes rna-fade-up {
        from { opacity: 0; transform: translateY(8px); }
        to { opacity: 1; transform: translateY(0); }
    }
    .rna-stat-card,
    .rna-table-card {
        animation: rna-fade-up .35s ease both;
    }
    .rna-stat-card:nth-child(1) { animation-delay: .02s; }
    .rna-stat-card:nth-child(2) { animation-delay: .06s; }
    .rna-stat-card:nth-child(3) { animation-delay: .1s; }
    .rna-stat-card:nth-child(4) { animation-delay: .14s; }
    .rna-table-card { animation-delay: .18s; }
</style>
