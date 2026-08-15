<template>
    <list-shell title="委托归档" eyebrow="ARCHIVE" subtitle="已过期、已取消、未完成与已完成委托的归档视图"
        :count="total" count-label="条记录" :loading="loading" :total="total"
        :page.sync="queryParams.pageNum" :page-size.sync="queryParams.pageSize"
        :page-sizes="[5, 7, 10, 15]" @current-change="handleCurrentChange"
        @size-change="handleSizeChange">
        <template #toolbar>
            <div class="ex-toolbar">
                <el-input v-model="queryParams.Description" placeholder="委托内容关键词" clearable class="ex-input"
                    @keyup.enter.native="handleQuery" />
                <el-select v-model="queryParams.taskType" placeholder="委托类型" clearable class="ex-select">
                    <el-option v-for="dict in taskTypeOption" :key="dict.value" :label="dict.label"
                        :value="dict.value" />
                </el-select>
                <el-select v-model="queryParams.Location" placeholder="委托地点" clearable class="ex-select">
                    <el-option v-for="dict in locationType" :key="dict.value" :label="dict.label"
                        :value="dict.value" />
                </el-select>
                <el-date-picker v-model="queryParams.CreatedAt" type="date" value-format="yyyy-MM-dd"
                    placeholder="委托创建时间" clearable class="ex-date" />
                <el-button type="primary" icon="el-icon-search" class="ex-btn ex-btn--primary"
                    @click="handleQuery">检索</el-button>
                <el-button icon="el-icon-refresh" class="ex-btn ex-btn--ghost" @click="resetQuery">重置</el-button>
            </div>
        </template>

        <data-list :data="delegateRecordsList" :loading="loading" mode="card" :config="listConfig" row-key="taskId"
            @action="onAction" />

        <!-- 委托详情弹窗 -->
        <el-dialog :title="title" :visible.sync="open" width="560px" custom-class="ex-dialog"
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
                <el-form-item label="委托状态">
                    {{ form.status }}
                </el-form-item>
            </el-form>
            <div slot="footer" class="ex-dialog__footer">
                <el-button @click="open = false">关 闭</el-button>
                <template v-if="Array.isArray(operation.title)">
                    <el-button v-for="(item, index) in operation.title" :key="index" :type="operation.type[index]"
                        @click="handleButtonClick(operation.click[index])">{{ item }}</el-button>
                </template>
                <el-button v-else-if="operation.title" :type="operation.type"
                    @click="handleButtonClick(operation.click)">{{ operation.title }}</el-button>
            </div>
        </el-dialog>
    </list-shell>
</template>

<script>
    import ListShell from '@/components/list/ListShell'
    import DataList from '@/components/list/DataList'
    import {
        listDelegateRecords, delDelegate, getDelegateByTaskID, getTaskCategories
    } from "@/api/";
    import { executeConfirmedRequest } from '@/utils/globalConfirmAction'
    import { TASK_STATUS } from '@/constants/enums'

    export default {
        name: "ExpireDelegationList",
        components: { ListShell, DataList },
        data() {
            return {
                TASK_STATUS,
                loading: true,
                total: 0,
                delegateRecordsList: [],
                title: "",
                open: false,
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
                    [TASK_STATUS.EXPIRED]: {
                        title: ["删除记录"],
                        type: ["danger"],
                        click: ["deleteRecordAdmin"]
                    },
                    [TASK_STATUS.CANCELLED]: {
                        title: ["删除记录"],
                        type: ["danger"],
                        click: ["deleteRecordAdmin"]
                    },
                    [TASK_STATUS.UNFINISHED]: {
                        title: ["删除记录"],
                        type: ["danger"],
                        click: ["deleteRecordAdmin"]
                    },
                    [TASK_STATUS.COMPLETED]: {
                        title: ["删除记录"],
                        type: ["danger"],
                        click: ["deleteRecordAdmin"]
                    }
                },
                operation: {},
                form: {},
                listConfig: [
                    {
                        label: '状态', field: 'status', type: 'badge',
                        badgeMap: {
                            [TASK_STATUS.EXPIRED]: { text: '已过期', tone: 'warning' },
                            [TASK_STATUS.CANCELLED]: { text: '已取消', tone: 'info' },
                            [TASK_STATUS.UNFINISHED]: { text: '未完成', tone: 'danger' },
                            [TASK_STATUS.COMPLETED]: { text: '已完成', tone: 'success' }
                        }
                    },
                    { label: '类型', field: 'type', type: 'badge', badgeMap: {} },
                    { label: '委托内容', field: 'description', title: true, noField: 'taskId' },
                    { label: '发布者', field: 'ownerName', emptyText: '—' },
                    { label: '信用分', field: 'ownerCredit' },
                    { label: '创建时间', field: 'createdAt', type: 'date' },
                    {
                        label: '操作', type: 'operate',
                        actions: [{ key: 'view', label: '查看', tone: 'primary' }]
                    }
                ]
            };
        },
        created() {
            this.handleType();
            this.getList();
        },
        methods: {
            getList() {
                this.loading = true;
                this.queryParams.TypePhase = "LIFECYCLE_TERMINATION";
                listDelegateRecords(this.queryParams).then((response) => {
                    if (response.data.code === 1) {
                        this.delegateRecordsList = response.data.data.records.map((record) => {
                            record.type = this.taskType[`${record.taskType}`];
                            return record;
                        });
                        this.total = response.data.data.total;
                    } else {
                        this.$message.error(response.data.msg || '查询归档委托失败');
                    }
                    this.loading = false;
                }).catch(err => {
                    console.error('查询归档委托失败：', err);
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
                }).catch(err => {
                    console.error('获取委托类型失败：', err);
                    this.$message.error('请求异常，请稍后重试');
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
    .ex-toolbar {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;
    }

    .ex-input {
        width: 210px;
    }

    .ex-select {
        width: 130px;
    }

    .ex-date {
        width: 150px;
    }

    .ex-input :deep(.el-input__inner),
    .ex-select :deep(.el-input__inner),
    .ex-date :deep(.el-input__inner) {
        height: 34px;
        background: rgba(255, 252, 245, .72);
        border: 1px solid #e6ddc9;
        border-radius: 8px;
        color: #2a3a30;
        font-family: inherit;
        font-size: 13px;
        transition: border-color .2s, box-shadow .2s, background .2s;
    }

    .ex-input :deep(.el-input__inner:focus),
    .ex-select :deep(.el-input__inner:focus),
    .ex-date :deep(.el-input__inner:focus) {
        border-color: #b9892c;
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13);
        background: #fffcf5;
    }

    .ex-btn {
        height: 34px;
        padding: 0 18px;
        border-radius: 8px;
        font-family: inherit;
        letter-spacing: .06em;
        font-size: 13px;
        transition: transform .18s, box-shadow .18s, background .18s, border-color .18s;
    }

    .ex-btn--primary {
        background: #2a3a30;
        border-color: #2a3a30;
        color: #f7f3ea;
    }

    .ex-btn--primary:hover,
    .ex-btn--primary:focus {
        background: #33493c;
        border-color: #33493c;
        color: #fff;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px -4px rgba(42, 58, 48, .5);
    }

    .ex-btn--ghost {
        background: transparent;
        border: 1px solid #e6ddc9;
        color: #5f6b62;
    }

    .ex-btn--ghost:hover,
    .ex-btn--ghost:focus {
        border-color: #b9892c;
        color: #96701f;
        background: rgba(255, 252, 245, .6);
    }
</style>

<style lang="less">
    /* 弹窗经 append-to-body 渲染在 body 下，须用非 scoped 样式 */
    .ex-dialog {
        background: #fffcf5;
        border-radius: 16px;
        box-shadow: 0 24px 60px -20px rgba(42, 58, 48, .4);
        overflow: hidden;
    }

    .ex-dialog .el-dialog__header {
        padding: 20px 28px 16px;
        border-bottom: 1px solid #e6ddc9;
        background: rgba(255, 252, 245, .6);
    }

    .ex-dialog .el-dialog__title {
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 19px;
        font-weight: 700;
        color: #2a3a30;
    }

    .ex-dialog .el-dialog__body {
        padding: 22px 28px 6px;
        color: #5f6b62;
    }

    .ex-dialog__footer {
        text-align: right;
        padding: 16px 28px 22px;
        border-top: 1px solid #e6ddc9;
        background: rgba(255, 252, 245, .6);
    }

    .ex-dialog__footer .el-button {
        height: 34px;
        padding: 0 18px;
        border-radius: 8px;
        font-family: inherit;
        letter-spacing: .06em;
    }

    .ex-dialog__footer .el-button + .el-button {
        margin-left: 10px;
    }

    .ex-dialog__footer .el-button--primary {
        background: #2a3a30;
        border-color: #2a3a30;
        color: #f7f3ea;
    }

    .ex-dialog__footer .el-button--primary:hover {
        background: #33493c;
        border-color: #33493c;
        color: #fff;
    }

    .ex-dialog__footer .el-button:not(.el-button--primary) {
        background: transparent;
        border-color: #e6ddc9;
        color: #5f6b62;
    }

    .ex-dialog__footer .el-button:not(.el-button--primary):hover {
        border-color: #b9892c;
        color: #96701f;
    }
</style>
