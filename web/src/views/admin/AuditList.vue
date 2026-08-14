<template>
    <list-shell title="委托审核" eyebrow="AUDIT DESK" subtitle="草稿提审与委托发布的审核管理"
        :count="total" count-label="条记录" :loading="loading" :total="total"
        :page.sync="queryParams.pageNum" :page-size.sync="queryParams.pageSize"
        :page-sizes="[5, 7, 10, 15]" @current-change="handleCurrentChange"
        @size-change="handleSizeChange">
        <template #toolbar>
            <div class="au-toolbar">
                <el-input v-model="queryParams.Description" placeholder="委托内容关键词" clearable class="au-input"
                    @keyup.enter.native="handleQuery" />
                <el-select v-model="queryParams.taskType" placeholder="委托类型" clearable class="au-select">
                    <el-option v-for="dict in taskTypeOption" :key="dict.value" :label="dict.label"
                        :value="dict.value" />
                </el-select>
                <el-select v-model="queryParams.Location" placeholder="委托地点" clearable class="au-select">
                    <el-option v-for="dict in locationType" :key="dict.value" :label="dict.label"
                        :value="dict.value" />
                </el-select>
                <el-date-picker v-model="queryParams.CreatedAt" type="date" value-format="yyyy-MM-dd"
                    placeholder="委托创建时间" clearable class="au-date" />
                <el-button type="primary" icon="el-icon-search" class="au-btn au-btn--primary"
                    @click="handleQuery">检索</el-button>
                <el-button icon="el-icon-refresh" class="au-btn au-btn--ghost" @click="resetQuery">重置</el-button>
            </div>
        </template>

        <nav class="au-tabs">
            <button type="button" class="au-tab" :class="{ 'is-active': activeTab === 'DRAFT' }"
                @click="switchTab('DRAFT')">草稿待提交</button>
            <button type="button" class="au-tab" :class="{ 'is-active': activeTab === 'AUDIT' }"
                @click="switchTab('AUDIT')">用户提交待审核</button>
        </nav>

        <data-list :data="delegateRecordsList" :loading="loading" mode="table" :config="listConfig" row-key="taskId"
            @action="onAction" />

        <!-- 委托详情弹窗 -->
        <el-dialog :title="title" :visible.sync="open" width="560px" custom-class="au-dialog"
            append-to-body :close-on-click-modal="false">
            <el-form ref="form" :model="form" label-width="100px">
                <el-form-item label="委托任务ID">
                    {{ form.taskId }}
                </el-form-item>
                <el-form-item label="委托内容">
                    {{ form.description }}
                </el-form-item>
                <el-form-item label="委托地点">
                    {{ form.location }}
                </el-form-item>
            </el-form>
            <div slot="footer" class="au-dialog__footer">
                <el-button @click="open = false">关 闭</el-button>
                <el-button v-if="operation.title" type="primary"
                    @click="handleButtonClick(operation.click)">{{ operation.title }}</el-button>
            </div>
        </el-dialog>
    </list-shell>
</template>

<script>
    import ListShell from '@/components/list/ListShell'
    import DataList from '@/components/list/DataList'
    import {
        listDelegateRecords, delDelegate, getDelegateByTaskID, getTaskCategories, withdrawReleaseByTaskIDAdmin
    } from "@/api/";
    import { executeConfirmedRequest } from '@/utils/globalConfirmAction'
    import { TASK_PHASE } from '@/constants/enums'

    export default {
        name: "AuditList",
        components: { ListShell, DataList },
        data() {
            return {
                TASK_PHASE,
                loading: true,
                total: 0,
                delegateRecordsList: [],
                title: "",
                open: false,
                activeTab: 'AUDIT',
                queryParams: {
                    pageNum: 1,
                    pageSize: 10
                },
                locationType: [
                    { value: '教学楼', label: '教学楼' },
                    { value: '图书馆', label: '图书馆' },
                    { value: '食堂', label: '食堂' },
                    { value: '运动场', label: '运动场' },
                    { value: '实验室', label: '实验室' },
                    { value: '其他', label: '其他' }
                ],
                taskTypeOption: [
                    { label: "委托", value: 1 },
                    { label: "取消委托", value: 2 }
                ],
                taskType: {},
                operations: {
                    "委托发布中": {
                        title: ["撤销发布", "退为草稿"],
                        type: ["warning", "warning"],
                        click: ["withdrawReleaseAdmin", "fallbackDraftAdmin"]
                    },
                    "已接收": {
                        title: ["删除记录"],
                        type: ["warning"],
                        click: ["deleteRecordAdmin"]
                    }
                },
                operation: {},
                form: {}
            }
        },
        computed: {
            listConfig() {
                const statusMap = {
                    '草稿': { text: '草稿', tone: 'default' },
                    '审核中': { text: '审核中', tone: 'warning' },
                    '审核未通过': { text: '审核未通过', tone: 'danger' },
                    '等待发布': { text: '等待发布', tone: 'success' },
                    '委托发布中': { text: '发布中', tone: 'success' },
                    '已接收': { text: '已接收', tone: 'warning' },
                    '已完成': { text: '已完成', tone: 'success' }
                };
                return [
                    { label: '任务ID', field: 'taskId', width: 90 },
                    { label: '委托内容', field: 'description', minWidth: 220 },
                    { label: '发布者', field: 'ownerName', width: 130, emptyText: '—' },
                    { label: '信用分', field: 'ownerCredit', width: 90 },
                    { label: '创建时间', field: 'createdAt', type: 'date', width: 160 },
                    { label: '类型', field: 'type', type: 'badge', width: 110, badgeMap: {} },
                    { label: '状态', field: 'status', type: 'badge', width: 110, badgeMap: statusMap },
                    {
                        label: '操作', type: 'operate', width: 110,
                        actions: [{ key: 'view', label: '查看', tone: 'primary' }]
                    }
                ];
            }
        },
        created() {
            this.handleType();
            this.getList();
        },
        methods: {
            switchTab(tab) {
                if (this.activeTab === tab) return;
                this.activeTab = tab;
                this.queryParams.pageNum = 1;
                this.getList();
            },
            getList() {
                this.loading = true;
                this.queryParams.TypePhase = this.activeTab === 'DRAFT' ? TASK_PHASE.EDITING_AND_AUDITING : TASK_PHASE.PUBLISHING_AND_EXECUTION;
                listDelegateRecords(this.queryParams).then((response) => {
                    if (response.data.code === 1) {
                        this.delegateRecordsList = response.data.data.records.map((record) => {
                            record.type = this.taskType[`${record.taskType}`];
                            return record;
                        });
                        this.total = response.data.data.total;
                    } else {
                        this.$message.error(response.data.msg || '查询委托失败');
                    }
                    this.loading = false;
                }).catch(err => {
                    console.error('查询委托失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                    this.loading = false;
                });
            },
            handleType() {
                getTaskCategories().then((data) => {
                    if (data.data.code === 1 && data.data.data.length > 0) {
                        this.taskTypeOption = [];
                        const taskCategories = data.data.data;
                        for (let i = 0; i < taskCategories.length; i++) {
                            this.taskType[`${taskCategories[i].id}`] = `${taskCategories[i].name}`;
                            this.taskTypeOption.push({ label: taskCategories[i].name, value: taskCategories[i].id });
                        }
                    }
                });
            },
            onAction({ key, row }) {
                if (key === 'view') {
                    this.handleView(row.taskId);
                }
            },
            handleView(id) {
                getDelegateByTaskID(id).then(response => {
                    if (response.data.code === 1) {
                        this.form = response.data.data;
                        this.operation = this.operations[response.data.data.status] || {};
                        this.title = "查看委托信息";
                        this.open = true;
                    } else {
                        this.$message.error(response.data.msg || '获取委托详情失败');
                    }
                }).catch(err => {
                    console.error('查看委托详情失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                });
            },
            handleButtonClick(actionName) {
                if (typeof this[actionName] === 'function') {
                    this[actionName]();
                }
            },
            async deleteRecordAdmin() {
                const id = this.form.taskId;
                const ok = await executeConfirmedRequest(delDelegate, id, "是否确认删除该委托？", "提示", "警告", "操作警告", "操作失败，请稍后重试", "操作已取消");
                if (ok) {
                    this.open = false;
                    this.getList();
                }
            },
            async withdrawReleaseAdmin() {
                const ok = await executeConfirmedRequest(withdrawReleaseByTaskIDAdmin, this.form.taskId, "是否确认撤销发布?", "提示", "警告", "操作警告", "操作失败，请稍后重试", "操作已取消");
                if (ok) {
                    this.getList();
                    this.open = false;
                }
            },
            handleQuery() {
                this.queryParams.pageNum = 1;
                this.getList();
            },
            resetQuery() {
                this.queryParams = {
                    Description: undefined,
                    taskType: undefined,
                    Location: undefined,
                    CreatedAt: undefined,
                    pageNum: 1,
                    pageSize: this.queryParams.pageSize
                };
                this.handleQuery();
            },
            handleSizeChange(val) {
                this.queryParams.pageSize = val;
                this.getList();
            },
            handleCurrentChange(val) {
                this.queryParams.pageNum = val;
                this.getList();
            }
        }
    }
</script>

<style lang="less" scoped>
    .au-toolbar {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;
    }

    .au-input {
        width: 210px;
    }

    .au-select {
        width: 130px;
    }

    .au-date {
        width: 150px;
    }

    .au-input :deep(.el-input__inner),
    .au-select :deep(.el-input__inner),
    .au-date :deep(.el-input__inner) {
        height: 34px;
        background: rgba(255, 252, 245, .72);
        border: 1px solid #e6ddc9;
        border-radius: 8px;
        color: #2a3a30;
        font-family: inherit;
        font-size: 13px;
        transition: border-color .2s, box-shadow .2s, background .2s;
    }

    .au-input :deep(.el-input__inner:focus),
    .au-select :deep(.el-input__inner:focus),
    .au-date :deep(.el-input__inner:focus) {
        border-color: #b9892c;
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13);
        background: #fffcf5;
    }

    .au-btn {
        height: 34px;
        padding: 0 18px;
        border-radius: 8px;
        font-family: inherit;
        letter-spacing: .06em;
        font-size: 13px;
        transition: transform .18s, box-shadow .18s, background .18s, border-color .18s;
    }

    .au-btn--primary {
        background: #2a3a30;
        border-color: #2a3a30;
        color: #f7f3ea;
    }

    .au-btn--primary:hover,
    .au-btn--primary:focus {
        background: #33493c;
        border-color: #33493c;
        color: #fff;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px -4px rgba(42, 58, 48, .5);
    }

    .au-btn--ghost {
        background: transparent;
        border: 1px solid #e6ddc9;
        color: #5f6b62;
    }

    .au-btn--ghost:hover,
    .au-btn--ghost:focus {
        border-color: #b9892c;
        color: #96701f;
        background: rgba(255, 252, 245, .6);
    }

    .au-tabs {
        display: flex;
        gap: 4px;
        margin-bottom: 14px;
        border-bottom: 1px solid #e6ddc9;
    }

    .au-tab {
        appearance: none;
        border: 0;
        background: transparent;
        position: relative;
        padding: 10px 18px 12px;
        font-family: inherit;
        font-size: 14px;
        letter-spacing: .05em;
        color: #5f6b62;
        cursor: pointer;
        transition: color .25s;
    }

    .au-tab::after {
        content: "";
        position: absolute;
        left: 18px;
        right: 18px;
        bottom: -1px;
        height: 2px;
        background: #b9892c;
        transform: scaleX(0);
        transform-origin: left;
        transition: transform .3s ease;
    }

    .au-tab.is-active {
        color: #2a3a30;
        font-weight: 600;
    }

    .au-tab.is-active::after {
        transform: scaleX(1);
    }
</style>

<style lang="less">
    /* 弹窗经 append-to-body 渲染在 body 下，须用非 scoped 样式 */
    .au-dialog {
        background: #fffcf5;
        border-radius: 16px;
        box-shadow: 0 24px 60px -20px rgba(42, 58, 48, .4);
        overflow: hidden;
    }

    .au-dialog .el-dialog__header {
        padding: 20px 28px 16px;
        border-bottom: 1px solid #e6ddc9;
        background: rgba(255, 252, 245, .6);
    }

    .au-dialog .el-dialog__title {
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 19px;
        font-weight: 700;
        color: #2a3a30;
    }

    .au-dialog .el-dialog__body {
        padding: 22px 28px 6px;
        color: #5f6b62;
    }

    .au-dialog__footer {
        text-align: right;
        padding: 16px 28px 22px;
        border-top: 1px solid #e6ddc9;
        background: rgba(255, 252, 245, .6);
    }

    .au-dialog__footer .el-button {
        height: 34px;
        padding: 0 18px;
        border-radius: 8px;
        font-family: inherit;
        letter-spacing: .06em;
    }

    .au-dialog__footer .el-button + .el-button {
        margin-left: 10px;
    }

    .au-dialog__footer .el-button--primary {
        background: #2a3a30;
        border-color: #2a3a30;
        color: #f7f3ea;
    }

    .au-dialog__footer .el-button--primary:hover {
        background: #33493c;
        border-color: #33493c;
        color: #fff;
    }

    .au-dialog__footer .el-button:not(.el-button--primary) {
        background: transparent;
        border-color: #e6ddc9;
        color: #5f6b62;
    }

    .au-dialog__footer .el-button:not(.el-button--primary):hover {
        border-color: #b9892c;
        color: #96701f;
    }
</style>
