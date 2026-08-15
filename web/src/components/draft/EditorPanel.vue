<template>
    <section class="ep">
        <header class="ep__bar">
            <h3 class="ep__bar-title">{{ barTitle }}</h3>
            <div class="ep__bar-actions">
                <button type="button" class="ep__text-btn" @click="rulesVisible = true">
                    <i class="el-icon-warning-outline"></i>发布规则</button>
                <button type="button" class="ep__text-btn" @click="previewVisible = true">
                    <i class="el-icon-view"></i>预览</button>
                <button type="button" class="ep__text-btn" @click="resetForm">
                    <i class="el-icon-refresh-left"></i>重置</button>
            </div>
        </header>

        <div class="ep__scroll">
            <div v-if="mode === 'publish'" class="ep__warn">
                <i class="el-icon-warning-outline"></i>
                <span>修改内容后点「保存草稿」，委托将转为<b>草稿</b>状态，需重新提交审核后才能再次发布；仅设置发布时间/截止时间不影响状态。</span>
            </div>

            <div v-if="mode === 'auditing'" class="ep__notice">
                <i class="el-icon-s-check"></i>
                <p>该委托正在审核中，暂不可编辑。</p>
            </div>

            <template v-else>
                <div class="ep__group">
                    <h4 class="ep__group-title"><i class="el-icon-document"></i>委托基本信息</h4>
                    <div class="ep__field">
                        <label class="ep__label">委托类型</label>
                        <el-select v-model="form.type" placeholder="请选择委托类型" style="width: 100%;">
                            <el-option v-for="opt in taskTypeOption" :key="opt.value" :label="opt.label"
                                :value="opt.value" />
                        </el-select>
                    </div>
                    <div class="ep__row">
                        <div class="ep__field">
                            <label class="ep__label">委托地点</label>
                            <el-select v-model="form.location" placeholder="请选择委托地点" style="width: 100%;">
                                <el-option v-for="opt in locationOptions" :key="opt.value" :label="opt.label"
                                    :value="opt.value" />
                            </el-select>
                        </div>
                        <div class="ep__field">
                            <label class="ep__label">委托金额</label>
                            <el-input-number v-model="form.money" :min="0" :precision="2" :step="1"
                                controls-position="right" style="width: 100%;" />
                        </div>
                    </div>
                </div>

                <div class="ep__group">
                    <h4 class="ep__group-title"><i class="el-icon-edit-outline"></i>委托详情</h4>
                    <div class="ep__field">
                        <label class="ep__label">委托内容</label>
                        <el-input type="textarea" v-model="form.description" :rows="10" maxlength="200"
                            show-word-limit placeholder="请详细描述您的委托内容，例如：具体要求、时间限制、报酬预算等..." />
                    </div>
                </div>
            </template>

            <div v-if="mode === 'publish'" class="ep__publish">
                <h4 class="ep__publish-title"><i class="el-icon-time"></i>发布设置</h4>
                <div class="ep__row">
                    <div class="ep__field">
                        <label class="ep__label">发布时间</label>
                        <el-date-picker v-model="publishForm.startTime" type="datetime"
                            value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择委托发布时间" style="width: 100%;" />
                    </div>
                    <div class="ep__field">
                        <label class="ep__label">截止时间</label>
                        <el-date-picker v-model="publishForm.endTime" type="datetime"
                            value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择委托截止时间" style="width: 100%;" />
                    </div>
                </div>
            </div>
        </div>

        <footer v-if="mode !== 'auditing'" class="ep__footer">
            <button type="button" class="ep__btn ep__btn--ghost" @click="onSaveDraft">
                <i class="el-icon-document-checked"></i>保存草稿</button>
            <button v-if="mode === 'new' || mode === 'edit'" type="button"
                class="ep__btn ep__btn--primary" @click="onSubmitAudit">
                <i class="el-icon-s-promotion"></i>提交审核</button>
            <button v-else-if="mode === 'publish'" type="button"
                class="ep__btn ep__btn--primary" @click="onPublish">
                <i class="el-icon-s-promotion"></i>确认发布</button>
        </footer>

        <el-drawer title="委托预览" :visible.sync="previewVisible" size="46%" custom-class="ep-drawer" append-to-body>
            <div class="ep-preview">
                <div class="ep-preview__row"><em>委托类型</em><span>{{ typeLabel(form.type) }}</span></div>
                <div class="ep-preview__row"><em>委托地点</em><span>{{ form.location || '未选择' }}</span></div>
                <div class="ep-preview__row"><em>委托金额</em><span>￥{{ fmtMoney(form.money) }}</span></div>
                <div class="ep-preview__row ep-preview__row--block"><em>委托内容</em>
                    <p>{{ form.description || '未填写内容' }}</p></div>
            </div>
        </el-drawer>

        <el-drawer title="发布须知" :visible.sync="rulesVisible" size="46%" custom-class="ep-drawer" append-to-body>
            <div class="ep-rules">
                <div class="ep-rules__item"><i class="el-icon-s-promotion"></i>
                    <p>发布委托信息流程：先创建草稿，再申请发布委托，只有通过审核后，再发布委托。</p></div>
                <div class="ep-rules__item"><i class="el-icon-edit"></i>
                    <p>草稿创建后可以修改，发布后不可修改。</p></div>
                <div class="ep-rules__item"><i class="el-icon-refresh-left"></i>
                    <p>待发布（审核通过）的委托如需修改内容，保存后将自动转为草稿，需重新提交审核；仅设置发布时间/截止时间不改变其状态。</p></div>
                <div class="ep-rules__item"><i class="el-icon-warning"></i>
                    <p>内容合法合规：所有发布的信息必须符合国家法律法规和学校相关规定，不得含有违法、淫秽、暴力、歧视等不良内容。</p></div>
                <div class="ep-rules__item"><i class="el-icon-document-checked"></i>
                    <p>真实准确：发布的信息必须真实准确，不得故意虚假宣传、夸大事实或误导他人。</p></div>
                <div class="ep-rules__item"><i class="el-icon-lock"></i>
                    <p>尊重隐私：严禁发布他人隐私信息，包括但不限于手机号码、学号、家庭住址等个人敏感信息。</p></div>
                <div class="ep-rules__item"><i class="el-icon-shop"></i>
                    <p>适度宣传：允许校园内部组织、社团、团队等发布相关活动、招募信息，但不得进行过度商业宣传。</p></div>
            </div>
        </el-drawer>
    </section>
</template>

<script>
    import { TASK_STATUS } from '@/constants/enums'

    export default {
        name: 'EditorPanel',
        props: {
            task: { type: Object, default: null },
            taskTypeOption: { type: Array, default: () => [] }
        },
        data() {
            return {
                TASK_STATUS,
                previewVisible: false,
                rulesVisible: false,
                locationOptions: [
                    { value: '教学楼', label: '教学楼' },
                    { value: '图书馆', label: '图书馆' },
                    { value: '食堂', label: '食堂' },
                    { value: '运动场', label: '运动场' },
                    { value: '实验室', label: '实验室' },
                    { value: '其他', label: '其他' }
                ],
                form: { type: null, location: '', money: 0, description: '' },
                publishForm: { startTime: null, endTime: null }
            }
        },
        computed: {
            mode() {
                if (!this.task) return 'new';
                if (this.task.status === TASK_STATUS.AUDITING) return 'auditing';
                if (this.task.status === TASK_STATUS.PENDING_RELEASE) return 'publish';
                return 'edit';
            },
            barTitle() {
                if (this.mode === 'new') return '新建委托';
                if (this.mode === 'publish') return `发布委托 · #${this.task.taskId}`;
                if (this.mode === 'auditing') return `审核中 · #${this.task.taskId}`;
                return `编辑草稿 · #${this.task.taskId}`;
            }
        },
        watch: {
            task: {
                immediate: true,
                handler(val) {
                    if (val && val.taskType != null) {
                        this.form = {
                            type: val.taskType,
                            location: val.location || '',
                            money: val.money == null ? 0 : Number(val.money),
                            description: val.description || ''
                        };
                        this.publishForm = { startTime: null, endTime: null };
                    } else {
                        this.form = { type: null, location: '', money: 0, description: '' };
                        this.publishForm = { startTime: null, endTime: null };
                    }
                }
            }
        },
        methods: {
            typeLabel(taskType) {
                if (taskType === null || taskType === undefined) return '未选择';
                const opt = this.taskTypeOption.find(o => o.value === Number(taskType));
                return opt ? opt.label : String(taskType);
            },
            fmtMoney(v) {
                if (v === null || v === undefined || v === '') return '0.00';
                return Number(v).toFixed(2);
            },
            resetForm() {
                this.form = { type: null, location: '', money: 0, description: '' };
                this.publishForm = { startTime: null, endTime: null };
            },
            validateForm() {
                if (!this.form.type) { this.$message.error('请选择委托类型'); return false; }
                if (!this.form.location) { this.$message.error('请选择委托地点'); return false; }
                if (!this.form.description || !this.form.description.trim()) { this.$message.error('请填写委托内容'); return false; }
                return true;
            },
            onSaveDraft() {
                if (!this.validateForm()) return;
                const payload = {
                    taskId: this.task ? this.task.taskId : null,
                    type: this.form.type,
                    location: this.form.location,
                    money: this.form.money,
                    description: this.form.description
                };
                // 待发布委托保存后后端会置回草稿，需重新审核；先明确告知
                if (this.task && this.task.status === TASK_STATUS.PENDING_RELEASE) {
                    this.$confirm('保存后将转为「草稿」状态，需重新提交审核后才能再次发布。是否继续？', '转为草稿', {
                        confirmButtonText: '转为草稿并保存',
                        cancelButtonText: '取消',
                        type: 'warning'
                    }).then(() => {
                        this.$emit('save-draft', payload);
                    }).catch(() => {});
                    return;
                }
                this.$emit('save-draft', payload);
            },
            onSubmitAudit() {
                if (!this.validateForm()) return;
                this.$emit('submit-audit', {
                    taskId: this.task ? this.task.taskId : null,
                    form: {
                        type: this.form.type,
                        location: this.form.location,
                        money: this.form.money,
                        description: this.form.description
                    }
                });
            },
            onPublish() {
                if (!this.publishForm.startTime || !this.publishForm.endTime) {
                    this.$message.error('请选择发布时间与截止时间');
                    return;
                }
                this.$emit('publish', {
                    taskId: this.task.taskId,
                    startTime: this.publishForm.startTime,
                    endTime: this.publishForm.endTime
                });
            }
        }
    }
</script>

<style lang="less" scoped>
    .ep {
        --ink: #2a3a30;
        --ink-soft: #5f6b62;
        --muted: #9aa198;
        --line: #e6ddc9;
        --line-strong: #d8ccb2;
        --brass: #b9892c;
        --brass-deep: #96701f;
        --sage: #5f8a6f;
        --terra: #b4543a;
        --card: #fffcf5;

        display: flex;
        flex-direction: column;
        height: 100%;
        min-height: 0;
        background: var(--card);
        border: 1px solid var(--line);
        border-radius: 14px;
        box-shadow: 0 18px 40px -24px rgba(42, 58, 48, .35);
        font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans SC", sans-serif;
    }

    .ep__bar {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 18px 24px 14px;
        border-bottom: 1px solid var(--line);
    }

    .ep__bar-title {
        margin: 0;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 18px;
        font-weight: 700;
        color: var(--ink);
        letter-spacing: .02em;
    }

    .ep__bar-actions { display: flex; gap: 4px; }

    .ep__text-btn {
        appearance: none;
        border: 0;
        background: transparent;
        font-family: inherit;
        font-size: 12px;
        letter-spacing: .04em;
        color: var(--brass-deep);
        cursor: pointer;
        padding: 5px 10px;
        border-radius: 6px;
        transition: background .2s;
    }
    .ep__text-btn i { margin-right: 4px; }
    .ep__text-btn:hover { background: rgba(185, 137, 44, .1); }

    .ep__scroll {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        padding: 18px 24px;
    }

    .ep__group { margin-bottom: 22px; }

    .ep__group-title {
        margin: 0 0 14px;
        padding-bottom: 10px;
        border-bottom: 1px dashed var(--line);
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 15px;
        font-weight: 700;
        color: var(--ink);
    }
    .ep__group-title i { margin-right: 6px; color: var(--brass); }

    .ep__field { margin-bottom: 16px; }

    .ep__label {
        display: block;
        margin-bottom: 7px;
        font-size: 13px;
        letter-spacing: .04em;
        color: var(--ink-soft);
    }

    .ep__row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

    .ep :deep(.el-input__inner),
    .ep :deep(.el-textarea__inner),
    .ep :deep(.el-input-number) {
        width: 100%;
        background: rgba(255, 252, 245, .85);
        border: 1px solid var(--line);
        border-radius: 9px;
        color: var(--ink);
        font-family: inherit;
        font-size: 13px;
        box-shadow: inset 0 1px 3px rgba(42, 58, 48, .05);
        transition: border-color .2s, box-shadow .2s, background .2s;
    }
    .ep :deep(.el-input__inner:hover),
    .ep :deep(.el-textarea__inner:hover) { border-color: var(--line-strong); }
    .ep :deep(.el-input__inner:focus),
    .ep :deep(.el-textarea__inner:focus) {
        border-color: var(--brass);
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13), inset 0 1px 3px rgba(42, 58, 48, .05);
        background: var(--card);
    }

    .ep__publish {
        margin: 4px 0 18px;
        padding: 16px 18px;
        background: rgba(220, 232, 224, .3);
        border: 1px solid rgba(95, 138, 111, .3);
        border-radius: 12px;
    }

    .ep__publish-title {
        margin: 0 0 14px;
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 15px;
        font-weight: 700;
        color: var(--sage);
    }
    .ep__publish-title i { margin-right: 6px; }

    .ep__notice {
        text-align: center;
        padding: 80px 0;
        color: var(--muted);
    }
    .ep__notice i { font-size: 40px; display: block; margin-bottom: 12px; color: var(--brass); }

    .ep__warn {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        margin-bottom: 18px;
        padding: 12px 14px;
        background: rgba(185, 137, 44, .1);
        border: 1px solid rgba(185, 137, 44, .3);
        border-radius: 9px;
        font-size: 13px;
        line-height: 1.6;
        color: var(--brass-deep);
    }
    .ep__warn i { font-size: 16px; margin-top: 2px; flex-shrink: 0; }
    .ep__warn b { color: var(--terra); }

    .ep__footer {
        flex-shrink: 0;
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        padding: 16px 24px 20px;
        border-top: 1px solid var(--line);
    }

    .ep__btn {
        appearance: none;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 22px;
        border-radius: 9px;
        border: 1px solid transparent;
        font-family: inherit;
        font-size: 13px;
        letter-spacing: .06em;
        cursor: pointer;
        transition: transform .18s, box-shadow .18s, background .18s, border-color .18s, color .18s;
    }
    .ep__btn:active { transform: scale(.97); }
    .ep__btn--primary {
        background: var(--ink);
        color: #f7f3ea;
    }
    .ep__btn--primary:hover {
        background: #33493c;
        transform: translateY(-1px);
        box-shadow: 0 6px 14px -6px rgba(42, 58, 48, .55);
    }
    .ep__btn--ghost {
        background: transparent;
        border-color: var(--line-strong);
        color: var(--ink-soft);
    }
    .ep__btn--ghost:hover {
        border-color: var(--brass);
        color: var(--brass-deep);
        background: rgba(255, 252, 245, .6);
    }
</style>

<style lang="less">
    .ep-drawer { background: #fffcf5; }
    .ep-preview { padding: 6px 24px 24px; }
    .ep-preview__row { display: flex; margin-bottom: 14px; font-size: 13px; }
    .ep-preview__row em { font-style: normal; width: 72px; flex-shrink: 0; color: #9aa198; }
    .ep-preview__row span, .ep-preview__row p { color: #2a3a30; margin: 0; line-height: 1.6; word-break: break-all; }
    .ep-preview__row--block { padding: 12px 14px; background: rgba(255, 252, 245, .8); border: 1px dashed #e6ddc9; border-radius: 8px; }
    .ep-rules { padding: 6px 24px 24px; }
    .ep-rules__item { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 14px; }
    .ep-rules__item i { font-size: 17px; color: #b9892c; margin-top: 2px; flex-shrink: 0; }
    .ep-rules__item p { margin: 0; font-size: 13px; line-height: 1.7; color: #5f6b62; }
</style>
