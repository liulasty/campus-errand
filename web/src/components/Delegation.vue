<template>
    <div class="dl-form">
        <div class="dl-form__grid">
            <!-- 左侧：发布表单 -->
            <section class="dl-form__panel">
                <header class="dl-form__panel-head">
                    <h3 class="dl-form__panel-title"><i class="el-icon-edit-outline"></i>填写委托信息</h3>
                    <button type="button" class="dl-form__rule" @click="dialogRulesVisible = true"><i
                            class="el-icon-warning-outline"></i>发布规则</button>
                </header>

                <div class="dl-form__intro">{{ description }}</div>

                <el-form ref="form" :model="DelegationFrom" label-width="0" size="medium" class="dl-form__el">
                    <el-form-item prop="location">
                        <label class="dl-form__label">委托地点</label>
                        <el-select v-model="DelegationFrom.location" placeholder="请选择委托地点" style="width: 100%;">
                            <el-option v-for="item in options" :key="item.value" :label="item.label"
                                :value="item.value" />
                        </el-select>
                    </el-form-item>
                    <el-form-item prop="money">
                        <label class="dl-form__label">委托金额</label>
                        <el-input-number v-model="DelegationFrom.money" :precision="2" :step="1" :min="0"
                            placeholder="请输入委托金额" controls-position="right" />
                    </el-form-item>
                    <el-form-item prop="content">
                        <label class="dl-form__label">委托内容</label>
                        <el-input type="textarea" v-model="DelegationFrom.content" :rows="8"
                            placeholder="请详细描述您的委托内容，例如：具体要求、时间限制、报酬预算等..."
                            maxlength="200" show-word-limit />
                    </el-form-item>
                    <div class="dl-form__actions">
                        <button type="button" class="dl-form__btn dl-form__btn--primary"
                            @click="onSubmit"><i class="el-icon-check"></i>立即创建草稿</button>
                        <button type="button" class="dl-form__btn dl-form__btn--ghost"
                            @click="handlePreview"><i class="el-icon-view"></i>预览</button>
                        <button type="button" class="dl-form__btn dl-form__btn--ghost"
                            @click="resetForm('form')">重置</button>
                    </div>
                </el-form>
            </section>

            <!-- 右侧：统计 & 草稿箱 -->
            <aside class="dl-form__side">
                <section class="dl-form__stats">
                    <div class="dl-form__stat">
                        <span class="dl-form__stat-icon dl-form__stat-icon--draft"><i class="el-icon-document-copy"></i></span>
                        <span class="dl-form__stat-text">
                            <strong class="dl-form__stat-num">{{ tasks.filter(t => t.status === TASK_STATUS.DRAFT).length }}</strong>
                            <em class="dl-form__stat-label">待发布草稿</em>
                        </span>
                    </div>
                    <span class="dl-form__stat-divider"></span>
                    <div class="dl-form__stat">
                        <span class="dl-form__stat-icon dl-form__stat-icon--audit"><i class="el-icon-s-check"></i></span>
                        <span class="dl-form__stat-text">
                            <strong class="dl-form__stat-num">{{ tasks.filter(t => t.status === TASK_STATUS.AUDITING).length }}</strong>
                            <em class="dl-form__stat-label">审核中委托</em>
                        </span>
                    </div>
                </section>

                <section class="dl-form__drafts">
                    <header class="dl-form__drafts-head">
                        <h3 class="dl-form__drafts-title"><i class="el-icon-notebook-2"></i>我的草稿箱</h3>
                        <button type="button" class="dl-form__drafts-refresh" @click="refresh()"><i
                                class="el-icon-refresh"></i>刷新</button>
                    </header>

                    <div v-if="tasks.length" class="dl-form__draft-list">
                        <article v-for="row in tasks" :key="row.taskId" class="dl-form__draft">
                            <div class="dl-form__draft-head">
                                <span class="dl-form__draft-type">{{ row.type || '未分类' }}</span>
                                <span class="dl-form__draft-status" :class="statusClass(row.status)">{{ row.status }}</span>
                            </div>
                            <p class="dl-form__draft-desc">{{ row.description }}</p>
                            <div class="dl-form__draft-meta">
                                <span class="dl-form__draft-money"><i class="el-icon-money"></i>￥{{ row.money }}</span>
                                <span class="dl-form__draft-time"><i class="el-icon-time"></i>{{ draftTimeLabel(row) }}</span>
                            </div>
                            <div class="dl-form__draft-actions">
                                <template v-if="row.status === TASK_STATUS.DRAFT">
                                    <button type="button" class="dl-form__draft-act dl-form__draft-act--edit"
                                        @click="handleEdit(row)">编辑</button>
                                    <button type="button" class="dl-form__draft-act dl-form__draft-act--del"
                                        @click="confirmDelete(row)">删除</button>
                                    <button type="button" class="dl-form__draft-act dl-form__draft-act--ok"
                                        @click="handleOngoing(row)">去审核</button>
                                </template>
                                <template v-else-if="row.status === TASK_STATUS.AUDITING">
                                    <span class="dl-form__draft-auditing">审核中...</span>
                                </template>
                                <template v-else-if="row.status === TASK_STATUS.PENDING_RELEASE">
                                    <button type="button" class="dl-form__draft-act dl-form__draft-act--del"
                                        @click="confirmDelete(row)">删除</button>
                                    <button type="button" class="dl-form__draft-act dl-form__draft-act--ok"
                                        @click="handleAudit(row)">发布</button>
                                </template>
                                <template v-else-if="row.status === TASK_STATUS.AUDIT_FAILED">
                                    <button type="button" class="dl-form__draft-act dl-form__draft-act--edit"
                                        @click="handleEdit(row)">修改</button>
                                    <button type="button" class="dl-form__draft-act dl-form__draft-act--del"
                                        @click="confirmDelete(row)">删除</button>
                                    <button type="button" class="dl-form__draft-act dl-form__draft-act--ok"
                                        @click="handleDetail(row)">原因</button>
                                </template>
                            </div>
                        </article>
                    </div>
                    <div v-else class="dl-form__drafts-empty">暂无草稿</div>
                </section>
            </aside>
        </div>

        <!-- 弹窗：发布规则 -->
        <el-dialog title="发布须知" :visible.sync="dialogRulesVisible" width="540px" custom-class="dl-dialog"
            :close-on-click-modal="false">
            <div class="dl-rules">
                <div class="dl-rules__item"><i class="el-icon-s-promotion"></i>
                    <p>发布委托信息流程：先创建草稿，再申请发布委托，只有通过审核后，再发布委托。</p></div>
                <div class="dl-rules__item"><i class="el-icon-edit"></i>
                    <p>草稿创建后可以修改，发布后不可修改。</p></div>
                <div class="dl-rules__item"><i class="el-icon-warning"></i>
                    <p>内容合法合规：所有发布的信息必须符合国家法律法规和学校相关规定，不得含有违法、淫秽、暴力、歧视等不良内容。</p></div>
                <div class="dl-rules__item"><i class="el-icon-document-checked"></i>
                    <p>真实准确：发布的信息必须真实准确，不得故意虚假宣传、夸大事实或误导他人。</p></div>
                <div class="dl-rules__item"><i class="el-icon-lock"></i>
                    <p>尊重隐私：严禁发布他人隐私信息，包括但不限于手机号码、学号、家庭住址等个人敏感信息。</p></div>
                <div class="dl-rules__item"><i class="el-icon-shop"></i>
                    <p>适度宣传：允许校园内部组织、社团、团队等发布相关活动、招募信息，但不得进行过度商业宣传。</p></div>
            </div>
            <div slot="footer" class="dl-dialog__footer">
                <el-button class="dl-form__btn dl-form__btn--primary" @click="dialogRulesVisible = false">我已阅读并同意</el-button>
            </div>
        </el-dialog>

        <!-- 弹窗：预览 -->
        <el-dialog title="委托预览" :visible.sync="dialogPreviewVisible" width="540px" custom-class="dl-dialog"
            :close-on-click-modal="false">
            <div class="dl-preview">
                <div class="dl-preview__item"><em>委托类型</em><span>{{ DelegationType }}</span></div>
                <div class="dl-preview__item"><em>委托地点</em><span>{{ DelegationFrom.location || '未选择' }}</span></div>
                <div class="dl-preview__item"><em>委托金额</em><span>￥{{ DelegationFrom.money || '0.00' }}</span></div>
                <div class="dl-preview__item dl-preview__item--block"><em>委托内容</em>
                    <p>{{ DelegationFrom.content || '未填写内容' }}</p></div>
            </div>
            <div slot="footer" class="dl-dialog__footer">
                <el-button class="dl-form__btn dl-form__btn--ghost" @click="dialogPreviewVisible = false">关闭</el-button>
                <el-button class="dl-form__btn dl-form__btn--primary" @click="confirmPreviewAndSubmit">确认无误，创建草稿</el-button>
            </div>
        </el-dialog>

        <!-- 弹窗：编辑草稿 -->
        <el-dialog title="编辑草稿" :visible.sync="dialogVisibleEdit" width="540px" custom-class="dl-dialog"
            :close-on-click-modal="false">
            <el-form ref="DraftFrom" :model="DraftFrom" label-width="0" size="small" class="dl-form__el">
                <el-form-item>
                    <label class="dl-form__label">委托类型</label>
                    <el-select v-model="DraftFrom.type" placeholder="请选择委托类型" style="width: 100%;">
                        <el-option v-for="item in taskTypeOption" :key="item.value" :label="item.label"
                            :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <label class="dl-form__label">委托地点</label>
                    <el-select v-model="DraftFrom.location" placeholder="请选择委托地点" style="width: 100%;">
                        <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <label class="dl-form__label">委托金额</label>
                    <el-input-number v-model="DraftFrom.money" :precision="2" :step="1" :min="0"
                        placeholder="请输入委托金额" controls-position="right" />
                </el-form-item>
                <el-form-item>
                    <label class="dl-form__label">委托内容</label>
                    <el-input type="textarea" v-model="DraftFrom.description" :rows="4" maxlength="200"
                        show-word-limit />
                </el-form-item>
            </el-form>
            <div slot="footer" class="dl-dialog__footer">
                <el-button class="dl-form__btn dl-form__btn--ghost" @click="cancel('DraftFrom')">取 消</el-button>
                <el-button class="dl-form__btn dl-form__btn--primary" @click.prevent="committingChanges()">提交修改</el-button>
            </div>
        </el-dialog>

        <!-- 弹窗：审核结果 -->
        <el-dialog title="审核结果" :visible.sync="dialogVisibleReason" width="540px" custom-class="dl-dialog"
            :close-on-click-modal="false">
            <div class="dl-reason">
                <div class="dl-reason__head">
                    <span class="dl-reason__icon"><i class="el-icon-circle-close"></i></span>
                    <h3>审核未通过</h3>
                </div>
                <div class="dl-reason__body">
                    <div class="dl-reason__row"><em>审核人员</em><span>{{ reason.name }}</span></div>
                    <div class="dl-reason__row"><em>审核说明</em><span class="dl-reason__comment">{{ reason.reviewComment }}</span></div>
                    <div class="dl-reason__row"><em>审核时间</em><span>{{ reason.reviewTime | dateTime }}</span></div>
                </div>
            </div>
            <div slot="footer" class="dl-dialog__footer">
                <el-button class="dl-form__btn dl-form__btn--primary" @click="dialogVisibleReason = false">确 定</el-button>
            </div>
        </el-dialog>

        <!-- 弹窗：发布委托 -->
        <el-dialog title="发布委托" :visible.sync="dialogVisiblePublish" width="600px" custom-class="dl-dialog"
            :close-on-click-modal="false">
            <div class="dl-publish__alert"><i class="el-icon-warning-outline"></i>请确认以下发布信息准确无误</div>
            <div class="dl-publish__summary">
                <div class="dl-publish__row"><em>委托类型</em><span>{{ publishFrom.type }}</span></div>
                <div class="dl-publish__row"><em>委托地点</em><span>{{ publishFrom.location }}</span></div>
                <div class="dl-publish__row dl-publish__row--block"><em>委托内容</em>
                    <p>{{ publishFrom.description }}</p></div>
            </div>
            <div class="dl-publish__divider"><span>设置时间</span></div>
            <el-form ref="publishFrom" :model="publishFrom" label-width="0" size="small" class="dl-form__el">
                <el-form-item>
                    <label class="dl-form__label">发布时间</label>
                    <el-date-picker clearable v-model="publishFrom.startTime" type="datetime"
                        value-format="yyyy年MM月dd日HH:mm:ss" placeholder="请选择委托发布时间" style="width: 100%;" />
                </el-form-item>
                <el-form-item>
                    <label class="dl-form__label">截止时间</label>
                    <el-date-picker clearable v-model="publishFrom.endTime" type="datetime"
                        value-format="yyyy年MM月dd日HH:mm:ss" placeholder="请选择委托截止时间" style="width: 100%;" />
                </el-form-item>
            </el-form>
            <div slot="footer" class="dl-dialog__footer">
                <el-button class="dl-form__btn dl-form__btn--ghost" @click="dialogVisiblePublish = false">暂不发布</el-button>
                <el-button class="dl-form__btn dl-form__btn--primary" @click.prevent="publishDelegation()">确认发布</el-button>
            </div>
        </el-dialog>

        <confirm-dialog :visible="deleteVisible" @update:visible="deleteVisible = $event"
            title="删除草稿" :message="deleteMessage" confirm-text="确认删除" loading-text="删除中…"
            :loading="deleteLoading" type="danger" @confirm="onDeleteConfirm"
            @cancel="deleteVisible = false" />
    </div>
</template>
<script>
    import {
        addTaskDraft, updateTaskDraft, getDraftDetailsBasedOnCommissionId, deleteTaskDraft, submitTaskDraft,
        confirmTask, getReason, publishingDelegation
    } from "@/api/index"
    import ConfirmDialog from '@/components/ConfirmDialog'
    import { executeConfirmedRequest } from '@/utils/globalConfirmAction'
    import { SUCCESS_CODE } from '@/constants/http'
    import { TASK_STATUS } from '@/constants/enums'

    export default {
        name: 'Delegation',
        components: { ConfirmDialog },
        props: {
            DelegationType: {
                type: String,
                default: 'abc'
            },
            id: {
                type: Number,
                default: 1
            },
            description: {
                type: String,
                default: 'abc'
            },
            tasks: {
                type: Array,
                required: true
            },
            taskTypeOption: {
                type: Array,
                required: false,
                default() {
                    return [{
                        label: "校园带去",
                        value: 1
                    }, {
                        label: "校园代买",
                        value: 2
                    }]
                }
            }
        },
        data() {
            return {
                TASK_STATUS,
                dialogRulesVisible: false,
                dialogPreviewVisible: false,
                dialogVisibleEdit: false,
                dialogVisibleReason: false,
                dialogVisiblePublish: false,
                // 删除确认
                deleteVisible: false,
                deleteLoading: false,
                deleteTarget: null,
                DelegationFrom: {
                    content: '',
                    location: '',
                    money: 0.0
                },
                publishFrom: {},
                DraftFrom: {
                    taskId: 0,
                    location: "教学楼",
                    description: "教学事故研究会",
                    type: 1,
                    createdAt: "2024-04-13 09:41:25",
                    money: 0.0
                },
                options: [
                    { value: '教学楼', label: '教学楼' },
                    { value: '图书馆', label: '图书馆' },
                    { value: '食堂', label: '食堂' },
                    { value: '运动场', label: '运动场' },
                    { value: '实验室', label: '实验室' },
                    { value: '其他', label: '其他' }
                ],
                reason: {
                    name: "",
                    reviewStatus: "",
                    reviewComment: "",
                    reviewTime: ""
                }
            }
        },
        computed: {
            deleteMessage() {
                const t = this.deleteTarget ? (this.deleteTarget.description || '该草稿') : '';
                return `确认删除草稿「${t}」？删除后不可恢复。`;
            }
        },
        methods: {
            // 添加草稿
            async onSubmit() {
                this.DelegationFrom.type = this.DelegationType;
                this.DelegationFrom.ownerId = this.$store.state.userInfo.userId;

                const ok = await executeConfirmedRequest(
                    addTaskDraft,
                    this.DelegationFrom,
                    "确认添加该委托信息？",
                    "提示信息",
                    "确认添加",
                    "添加成功",
                    "添加失败",
                    "取消添加草稿"
                );
                if (ok) {
                    this.refresh();
                }
            },
            handlePreview() {
                this.dialogPreviewVisible = true;
            },
            confirmPreviewAndSubmit() {
                this.dialogPreviewVisible = false;
                this.onSubmit();
            },
            // 重置表单
            resetForm(formName) {
                this.$refs[formName].resetFields();
            },
            // 更新草稿
            handleEdit(row) {
                getDraftDetailsBasedOnCommissionId(row.taskId).then((data) => {
                    if (data.data.code === SUCCESS_CODE) {
                        this.DraftFrom = data.data.data
                        this.dialogVisibleEdit = true;
                    } else {
                        this.$message({
                            message: data.data.msg,
                            type: 'error'
                        });
                    }
                }).catch(err => {
                    console.error('获取草稿详情失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            // 刷新委托列表
            refresh() {
                this.$emit('childEvent');
            },
            // 提交修改
            committingChanges() {
                updateTaskDraft(this.DraftFrom).then((data) => {
                    if (data.data.code === 1) {
                        this.$message({
                            message: data.data.msg,
                            type: 'success'
                        });
                        this.dialogVisibleEdit = false;
                        this.refresh()
                    } else {
                        this.$message({
                            message: data.data.msg,
                            type: 'error'
                        });
                    }
                }).catch(err => {
                    console.error('更新草稿失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            // 删除草稿：打开确认弹窗
            confirmDelete(row) {
                this.deleteTarget = row;
                this.deleteVisible = true;
            },
            async onDeleteConfirm() {
                const row = this.deleteTarget;
                if (!row) {
                    return;
                }
                this.deleteLoading = true;
                try {
                    const data = await deleteTaskDraft(row.taskId);
                    if (data.data.code === 1) {
                        this.$message.success(data.data.msg || '删除成功');
                        this.deleteVisible = false;
                        this.refresh();
                    } else {
                        this.$message.error(data.data.msg || '删除失败');
                    }
                } catch (err) {
                    console.error('删除草稿失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                } finally {
                    this.deleteLoading = false;
                }
            },
            // 提交草稿审核
            handleOngoing(data) {
                submitTaskDraft(data.taskId).then((data) => {
                    if (data.data.code === SUCCESS_CODE) {
                        this.$message({
                            type: 'success',
                            message: data.data.msg
                        });
                    } else {
                        if (!this.handleAuthGateError(data.data.msg)) {
                            this.$message({
                                type: 'error',
                                message: data.data.msg
                            });
                        }
                    }
                    this.$emit('childEvent');
                }).catch(err => {
                    console.error('提交审核失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            // 打开确认发布窗口
            handleAudit(data) {
                this.dialogVisiblePublish = true;
                confirmTask(data.taskId).then(data => {
                    if (data.data.code === SUCCESS_CODE) {
                        this.$message({
                            type: 'success',
                            message: data.data.msg
                        });
                        this.publishFrom = data.data.data;
                        this.publishFrom.type = this.taskTypeOption[this.publishFrom.type - 1].label;
                    } else {
                        this.dialogVisiblePublish = false;
                        if (!this.handleAuthGateError(data.data.msg)) {
                            this.$message({
                                type: 'error',
                                message: data.data.msg
                            });
                        }
                    }
                }).catch(err => {
                    console.error('获取发布信息失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            // L1 实名认证门禁：命中提示则引导去认证
            handleAuthGateError(msg) {
                if (msg && msg.indexOf('L1实名认证') !== -1) {
                    this.$confirm('发布委托需完成 L1 实名认证，是否前往认证？', '提示', {
                        confirmButtonText: '去认证',
                        cancelButtonText: '取消',
                        type: 'warning'
                    }).then(() => {
                        this.$router.push('/myInfo')
                    }).catch(() => {})
                    return true
                }
                return false
            },
            // 发布委托
            publishDelegation() {
                const publish = {
                    id: this.publishFrom.taskId,
                    start: this.publishFrom.startTime,
                    end: this.publishFrom.endTime
                }
                publishingDelegation(publish).then((data) => {
                    if (data.data.code === 1) {
                        this.$message({
                            message: data.data.msg,
                            type: 'success'
                        });
                    } else {
                        this.$message({
                            message: data.data.msg,
                            type: 'error'
                        });
                    }
                    this.$emit('childEvent');
                    this.dialogVisiblePublish = false;
                }).catch(err => {
                    console.error('发布委托失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            // 查看审核未通过原因
            handleDetail(val) {
                getReason(val.taskId).then((data) => {
                    if (data.data.code == 1) {
                        this.reason = data.data.data;
                        this.dialogVisibleReason = true;
                    } else {
                        this.$message({
                            type: 'error',
                            message: data.data.msg
                        });
                    }
                }).catch(err => {
                    console.error('查看驳回原因失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            cancel(form) {
                this.resetForm(form);
            },
            // 草稿列表辅助
            draftTimeLabel(row) {
                return row.createTime || row.createdAt || '—';
            },
            statusClass(status) {
                const map = {
                    [TASK_STATUS.DRAFT]: 'st--draft',
                    [TASK_STATUS.AUDITING]: 'st--auditing',
                    [TASK_STATUS.PENDING_RELEASE]: 'st--pending',
                    [TASK_STATUS.AUDIT_FAILED]: 'st--failed'
                };
                return map[status] || '';
            }
        }
    }
</script>
<style lang="less" scoped>
    .dl-form {
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
        color: var(--ink);
        font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans SC", sans-serif;
    }

    .dl-form__grid {
        display: grid;
        grid-template-columns: 1.55fr 1fr;
        gap: 20px;
        align-items: start;
    }

    /* ===== 左侧表单面板 ===== */
    .dl-form__panel {
        background: var(--card);
        border: 1px solid var(--line);
        border-radius: 14px;
        padding: 20px 24px 24px;
        box-shadow: 0 10px 26px -20px rgba(42, 58, 48, .22);
    }

    .dl-form__panel-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-bottom: 14px;
        border-bottom: 1px solid var(--line);
        margin-bottom: 16px;
    }

    .dl-form__panel-title {
        margin: 0;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 18px;
        font-weight: 700;
        letter-spacing: .02em;
        color: var(--ink);
    }

    .dl-form__panel-title i {
        margin-right: 8px;
        color: var(--brass);
    }

    .dl-form__rule {
        appearance: none;
        border: 0;
        background: transparent;
        font-family: inherit;
        font-size: 12px;
        letter-spacing: .04em;
        color: var(--brass-deep);
        cursor: pointer;
        padding: 4px 8px;
        border-radius: 6px;
        transition: background .2s;
    }

    .dl-form__rule i {
        margin-right: 4px;
    }

    .dl-form__rule:hover {
        background: rgba(185, 137, 44, .1);
    }

    .dl-form__intro {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 22px;
        padding: 12px 16px;
        background: rgba(220, 232, 224, .35);
        border: 1px solid rgba(95, 138, 111, .25);
        border-radius: 10px;
        font-size: 13px;
        letter-spacing: .04em;
        color: var(--sage);
    }

    .dl-form__el {
        padding: 0 2px;
    }

    .dl-form__el :deep(.el-form-item) {
        margin-bottom: 22px;
    }

    .dl-form__label {
        display: block;
        margin-bottom: 8px;
        font-size: 13px;
        letter-spacing: .05em;
        color: var(--ink-soft);
    }

    .dl-form__el :deep(.el-input__inner),
    .dl-form__el :deep(.el-textarea__inner) {
        background: rgba(255, 252, 245, .85);
        border: 1px solid var(--line);
        border-radius: 9px;
        color: var(--ink);
        font-family: inherit;
        font-size: 13px;
        transition: border-color .2s, box-shadow .2s, background .2s;
    }

    .dl-form__el :deep(.el-input__inner:hover),
    .dl-form__el :deep(.el-textarea__inner:hover) {
        border-color: var(--line-strong);
    }

    .dl-form__el :deep(.el-input__inner:focus),
    .dl-form__el :deep(.el-textarea__inner:focus) {
        border-color: var(--brass);
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13);
        background: var(--card);
    }

    .dl-form__el :deep(.el-input__inner::placeholder),
    .dl-form__el :deep(.el-textarea__inner::placeholder) {
        color: var(--muted);
    }

    .dl-form__el :deep(.el-input-number) {
        width: 100%;
    }

    .dl-form__el :deep(.el-input-number .el-input__inner) {
        text-align: left;
        padding-left: 12px;
    }

    .dl-form__el :deep(.el-input-number__decrease),
    .dl-form__el :deep(.el-input-number__increase) {
        background: rgba(255, 252, 245, .8);
        border-color: var(--line);
        color: var(--ink-soft);
    }

    .dl-form__actions {
        display: flex;
        gap: 10px;
        margin-top: 8px;
        padding-top: 18px;
        border-top: 1px dashed var(--line);
    }

    .dl-form__btn {
        appearance: none;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 36px;
        padding: 0 20px;
        border-radius: 9px;
        border: 1px solid transparent;
        font-family: inherit;
        font-size: 13px;
        letter-spacing: .06em;
        cursor: pointer;
        transition: transform .18s, box-shadow .18s, background .18s, border-color .18s, color .18s;
    }

    .dl-form__btn i {
        font-size: 14px;
    }

    .dl-form__btn--primary {
        background: var(--ink);
        color: #f7f3ea;
    }

    .dl-form__btn--primary:hover {
        background: #33493c;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px -4px rgba(42, 58, 48, .5);
    }

    .dl-form__btn--ghost {
        background: transparent;
        border-color: var(--line);
        color: var(--ink-soft);
    }

    .dl-form__btn--ghost:hover {
        border-color: var(--brass);
        color: var(--brass-deep);
        background: rgba(255, 252, 245, .6);
    }

    /* ===== 右侧栏 ===== */
    .dl-form__side {
        display: flex;
        flex-direction: column;
        gap: 16px;
    }

    .dl-form__stats {
        display: flex;
        align-items: center;
        background: var(--card);
        border: 1px solid var(--line);
        border-radius: 14px;
        padding: 16px 18px;
        box-shadow: 0 10px 26px -20px rgba(42, 58, 48, .22);
    }

    .dl-form__stat {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 12px;
    }

    .dl-form__stat-divider {
        width: 1px;
        height: 42px;
        background: var(--line);
    }

    .dl-form__stat-icon {
        width: 46px;
        height: 46px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
        color: #fff;
        flex-shrink: 0;
    }

    .dl-form__stat-icon--draft {
        background: var(--sage);
        box-shadow: 0 8px 18px -8px rgba(95, 138, 111, .6);
    }

    .dl-form__stat-icon--audit {
        background: var(--brass);
        box-shadow: 0 8px 18px -8px rgba(185, 137, 44, .6);
    }

    .dl-form__stat-text {
        display: flex;
        flex-direction: column;
    }

    .dl-form__stat-num {
        font-family: Georgia, "Times New Roman", serif;
        font-size: 26px;
        font-weight: 700;
        line-height: 1.1;
        color: var(--ink);
    }

    .dl-form__stat-label {
        font-style: normal;
        font-size: 12px;
        letter-spacing: .06em;
        color: var(--muted);
    }

    /* 草稿箱 */
    .dl-form__drafts {
        background: var(--card);
        border: 1px solid var(--line);
        border-radius: 14px;
        padding: 16px 18px 18px;
        box-shadow: 0 10px 26px -20px rgba(42, 58, 48, .22);
    }

    .dl-form__drafts-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-bottom: 12px;
        border-bottom: 1px solid var(--line);
        margin-bottom: 14px;
    }

    .dl-form__drafts-title {
        margin: 0;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 16px;
        font-weight: 700;
        color: var(--ink);
    }

    .dl-form__drafts-title i {
        margin-right: 8px;
        color: var(--brass);
    }

    .dl-form__drafts-refresh {
        appearance: none;
        border: 0;
        background: transparent;
        font-family: inherit;
        font-size: 12px;
        letter-spacing: .04em;
        color: var(--brass-deep);
        cursor: pointer;
        padding: 4px 8px;
        border-radius: 6px;
        transition: background .2s;
    }

    .dl-form__drafts-refresh:hover {
        background: rgba(185, 137, 44, .1);
    }

    .dl-form__draft-list {
        max-height: 430px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 10px;
        padding-right: 4px;
    }

    .dl-form__draft {
        background: rgba(255, 252, 245, .7);
        border: 1px solid var(--line);
        border-left: 3px solid var(--line-strong);
        border-radius: 10px;
        padding: 12px 14px;
        transition: transform .2s, border-color .2s, box-shadow .2s;
    }

    .dl-form__draft:hover {
        transform: translateY(-1px);
        border-color: var(--line-strong);
        box-shadow: 0 6px 16px -10px rgba(42, 58, 48, .25);
    }

    .dl-form__draft-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 6px;
    }

    .dl-form__draft-type {
        font-size: 11px;
        letter-spacing: .1em;
        color: #7a6a3a;
        background: #efe9d7;
        padding: 2px 9px;
        border-radius: 999px;
    }

    .dl-form__draft-status {
        font-size: 11px;
        letter-spacing: .05em;
        padding: 2px 9px;
        border-radius: 999px;
    }

    .dl-form__draft-status.st--draft {
        background: #ece8de;
        color: #807a68;
    }

    .dl-form__draft-status.st--auditing {
        background: #f1e9d3;
        color: #8a6d26;
    }

    .dl-form__draft-status.st--pending {
        background: #dce8e0;
        color: #3e6b50;
    }

    .dl-form__draft-status.st--failed {
        background: #f1e3dc;
        color: #a0523b;
    }

    .dl-form__draft-desc {
        margin: 0 0 8px;
        font-size: 13px;
        line-height: 1.6;
        color: var(--ink);
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .dl-form__draft-meta {
        display: flex;
        align-items: center;
        gap: 14px;
        font-size: 12px;
        color: var(--muted);
    }

    .dl-form__draft-money {
        color: var(--brass-deep);
        font-weight: 600;
    }

    .dl-form__draft-meta i {
        margin-right: 4px;
    }

    .dl-form__draft-actions {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-top: 10px;
        padding-top: 8px;
        border-top: 1px dashed var(--line);
    }

    .dl-form__draft-act {
        appearance: none;
        border: 1px solid transparent;
        background: transparent;
        font-family: inherit;
        font-size: 12px;
        padding: 3px 9px;
        border-radius: 6px;
        cursor: pointer;
        transition: background .2s, border-color .2s, color .2s;
    }

    .dl-form__draft-act--edit {
        color: var(--brass-deep);
    }

    .dl-form__draft-act--edit:hover {
        background: rgba(185, 137, 44, .1);
        border-color: rgba(185, 137, 44, .28);
    }

    .dl-form__draft-act--del {
        color: var(--terra);
    }

    .dl-form__draft-act--del:hover {
        background: rgba(180, 84, 58, .09);
        border-color: rgba(180, 84, 58, .28);
    }

    .dl-form__draft-act--ok {
        color: var(--sage);
    }

    .dl-form__draft-act--ok:hover {
        background: rgba(95, 138, 111, .1);
        border-color: rgba(95, 138, 111, .28);
    }

    .dl-form__draft-auditing {
        font-size: 12px;
        color: var(--muted);
    }

    .dl-form__drafts-empty {
        text-align: center;
        padding: 34px 0;
        color: var(--muted);
        font-size: 13px;
        letter-spacing: .1em;
    }

    /* ===== 弹窗通用 ===== */
    .dl-form :deep(.dl-dialog) {
        background: var(--card);
        border-radius: 16px;
        box-shadow: 0 24px 60px -20px rgba(42, 58, 48, .4);
        overflow: hidden;
    }

    .dl-form :deep(.dl-dialog .el-dialog__header) {
        padding: 20px 28px 16px;
        border-bottom: 1px solid var(--line);
        background: rgba(255, 252, 245, .6);
    }

    .dl-form :deep(.dl-dialog .el-dialog__title) {
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 19px;
        font-weight: 700;
        color: var(--ink);
        letter-spacing: .02em;
    }

    .dl-form :deep(.dl-dialog .el-dialog__headerbtn .el-dialog__close) {
        color: var(--muted);
    }

    .dl-form :deep(.dl-dialog .el-dialog__body) {
        padding: 22px 28px 6px;
    }

    .dl-dialog__footer {
        text-align: right;
        padding: 16px 28px 22px;
        border-top: 1px solid var(--line);
        background: rgba(255, 252, 245, .6);
    }

    .dl-dialog__footer .dl-form__btn {
        margin-left: 10px;
    }

    /* 发布规则 */
    .dl-rules__item {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        margin-bottom: 14px;
    }

    .dl-rules__item i {
        font-size: 17px;
        color: var(--brass);
        margin-top: 2px;
        flex-shrink: 0;
    }

    .dl-rules__item p {
        margin: 0;
        font-size: 13px;
        line-height: 1.7;
        color: var(--ink-soft);
    }

    /* 预览 */
    .dl-preview__item {
        display: flex;
        margin-bottom: 14px;
        font-size: 13px;
    }

    .dl-preview__item em {
        font-style: normal;
        width: 72px;
        flex-shrink: 0;
        color: var(--muted);
        letter-spacing: .04em;
    }

    .dl-preview__item span,
    .dl-preview__item p {
        color: var(--ink);
        margin: 0;
        line-height: 1.6;
        word-break: break-all;
    }

    .dl-preview__item--block {
        padding: 12px 14px;
        background: rgba(255, 252, 245, .7);
        border: 1px dashed var(--line);
        border-radius: 8px;
    }

    /* 审核结果 */
    .dl-reason {
        text-align: center;
    }

    .dl-reason__head {
        margin-bottom: 18px;
    }

    .dl-reason__icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 54px;
        height: 54px;
        border-radius: 50%;
        background: rgba(180, 84, 58, .12);
        color: var(--terra);
        font-size: 30px;
        margin-bottom: 10px;
    }

    .dl-reason__head h3 {
        margin: 0;
        font-size: 16px;
        color: var(--ink);
    }

    .dl-reason__body {
        text-align: left;
        background: rgba(255, 252, 245, .7);
        border: 1px solid var(--line);
        border-radius: 10px;
        padding: 16px 18px;
    }

    .dl-reason__row {
        display: flex;
        margin-bottom: 10px;
        font-size: 13px;
    }

    .dl-reason__row:last-child {
        margin-bottom: 0;
    }

    .dl-reason__row em {
        font-style: normal;
        width: 70px;
        flex-shrink: 0;
        color: var(--muted);
        letter-spacing: .04em;
    }

    .dl-reason__row span {
        color: var(--ink-soft);
        line-height: 1.6;
        word-break: break-all;
    }

    .dl-reason__comment {
        color: var(--ink) !important;
        font-weight: 500;
    }

    /* 发布确认 */
    .dl-publish__alert {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 18px;
        padding: 11px 14px;
        background: rgba(185, 137, 44, .1);
        border: 1px solid rgba(185, 137, 44, .25);
        border-radius: 9px;
        font-size: 13px;
        color: var(--brass-deep);
    }

    .dl-publish__summary {
        background: rgba(255, 252, 245, .7);
        border: 1px solid var(--line);
        border-radius: 10px;
        padding: 14px 16px;
        margin-bottom: 18px;
    }

    .dl-publish__row {
        display: flex;
        margin-bottom: 10px;
        font-size: 13px;
    }

    .dl-publish__row:last-child {
        margin-bottom: 0;
    }

    .dl-publish__row em {
        font-style: normal;
        width: 70px;
        flex-shrink: 0;
        color: var(--muted);
        letter-spacing: .04em;
    }

    .dl-publish__row span,
    .dl-publish__row p {
        color: var(--ink);
        margin: 0;
        line-height: 1.6;
        word-break: break-all;
    }

    .dl-publish__divider {
        display: flex;
        align-items: center;
        gap: 10px;
        margin: 18px 0;
        font-size: 12px;
        letter-spacing: .1em;
        color: var(--muted);
    }

    .dl-publish__divider::after {
        content: "";
        flex: 1;
        height: 1px;
        background: var(--line);
    }
</style>
