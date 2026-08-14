<template>
    <list-shell title="委托类型管理" eyebrow="CATEGORY DIRECTORY" subtitle="委托分类的启用与维护"
        :count="total" count-label="个类型" :loading="loading" :total="total"
        :page.sync="queryParams.pageNum" :page-size.sync="queryParams.pageSize"
        :page-sizes="[3, 5, 7, 10]" @current-change="handleCurrentChange"
        @size-change="handleSizeChange">
        <template #header-extra>
            <el-button type="primary" icon="el-icon-plus" class="dt-btn dt-btn--add"
                @click="addDialogForm = true">添加委托类型</el-button>
        </template>

        <template #toolbar>
            <div class="dt-toolbar">
                <el-input v-model="queryParams.categoryName" placeholder="委托类型" clearable class="dt-input"
                    @keyup.enter.native="handleQuery" />
                <el-select v-model="queryParams.isEnable" placeholder="启用状态" clearable class="dt-select">
                    <el-option label="启用" value="1" />
                    <el-option label="禁用" value="0" />
                </el-select>
                <el-input v-model="queryParams.description" placeholder="类型描述关键词" clearable class="dt-input"
                    @keyup.enter.native="handleQuery" />
                <el-button type="primary" icon="el-icon-search" class="dt-btn dt-btn--primary"
                    @click="handleQuery">检索</el-button>
                <el-button icon="el-icon-refresh" class="dt-btn dt-btn--ghost" @click="resetQuery">重置</el-button>
            </div>
        </template>

        <data-list :data="list" :loading="loading" mode="table" :config="listConfig" row-key="categoryId"
            @action="onAction" />

        <el-dialog title="添加委托类型" :visible.sync="addDialogForm" width="480px" custom-class="dt-dialog"
            append-to-body :close-on-click-modal="false">
            <el-form :model="addForm" :rules="rules" ref="addForm" label-width="96px" size="small">
                <el-form-item label="委托类型" prop="categoryName">
                    <el-input v-model="addForm.categoryName" placeholder="请输入委托类型" clearable />
                </el-form-item>
                <el-form-item label="类型描述" prop="categoryDescription">
                    <el-input v-model="addForm.categoryDescription" placeholder="请输入委托类型描述" clearable />
                </el-form-item>
            </el-form>
            <div slot="footer" class="dt-dialog__footer">
                <el-button @click="addDialogForm = false">取 消</el-button>
                <el-button type="primary" @click="addDelegationType">添 加</el-button>
            </div>
        </el-dialog>
        <el-dialog title="编辑委托类型" :visible.sync="updateDialogForm" width="480px" custom-class="dt-dialog"
            append-to-body :close-on-click-modal="false">
            <el-form :model="updateForm" :rules="rules" ref="updateForm" label-width="96px" size="small">
                <el-form-item label="委托类型" prop="categoryName">
                    <el-input v-model="updateForm.categoryName" placeholder="请输入委托类型" clearable />
                </el-form-item>
                <el-form-item label="类型描述" prop="categoryDescription">
                    <el-input v-model="updateForm.categoryDescription" placeholder="请输入委托类型描述" clearable />
                </el-form-item>
            </el-form>
            <div slot="footer" class="dt-dialog__footer">
                <el-button @click="updateDialogForm = false">取 消</el-button>
                <el-button type="primary" @click="updateDelegationType">确 定</el-button>
            </div>
        </el-dialog>
    </list-shell>
</template>

<script>
    import ListShell from '@/components/list/ListShell'
    import DataList from '@/components/list/DataList'
    import {
        getDelegationTypeList, getDelegationTypeById, updateDelegationTypeAdmin, addDelegationTypeAdmin,
        deleteDelegationType, enableDelegationType
    } from "@/api/";
    import { executeConfirmedRequest } from '@/utils/globalConfirmAction'
    export default {
        name: "DelegationType",
        components: { ListShell, DataList },
        data() {
            return {
                queryParams: {
                    categoryName: undefined,
                    isEnable: undefined,
                    description: undefined,
                    pageNum: 1,
                    pageSize: 5
                },
                total: 0,
                loading: false,
                list: [],
                addDialogForm: false,
                addForm: {
                    categoryName: "",
                    categoryDescription: ""
                },
                updateDialogForm: false,
                updateForm: {},
                rules: {
                    categoryName: [
                        { required: true, message: "请输入委托类型", trigger: "blur" }
                    ],
                    categoryDescription: [
                        { required: true, message: "请输入委托类型描述", trigger: "blur" }
                    ]
                },
                listConfig: [
                    { label: "编号", field: "categoryId", width: 90 },
                    { label: "委托类型", field: "categoryName", width: 190 },
                    { label: "详情", field: "categoryDescription", minWidth: 240 },
                    {
                        label: "状态", field: "isEnabled", type: "badge", width: 110,
                        badgeMap: {
                            true: { text: "已启用", tone: "success" },
                            false: { text: "已禁用", tone: "info" }
                        }
                    },
                    {
                        label: "操作", type: "operate", width: 250,
                        actions: [
                            { key: "edit", label: "编辑", tone: "primary" },
                            { key: "delete", label: "删除", tone: "danger" },
                            { key: "toggle", label: row => (row.isEnabled === true ? "禁用" : "启用"), tone: "warning" }
                        ]
                    }
                ]
            }
        },
        created() {
            this.getList();
        },
        methods: {
            handleQuery() {
                this.queryParams.pageNum = 1;
                this.getList();
            },
            resetQuery() {
                this.queryParams = {
                    categoryName: undefined,
                    isEnable: undefined,
                    description: undefined,
                    pageNum: 1,
                    pageSize: this.queryParams.pageSize
                };
                this.getList();
            },
            handleSizeChange(val) {
                this.queryParams.pageSize = val;
                this.getList();
            },
            handleCurrentChange(val) {
                this.queryParams.pageNum = val;
                this.getList();
            },
            getList() {
                this.loading = true;
                getDelegationTypeList(this.queryParams).then(response => {
                    if (response.data.code == 1) {
                        this.list = response.data.data.records;
                        this.total = response.data.data.total;
                    } else {
                        this.$message({
                            message: response.data.msg,
                            type: "error"
                        });
                    }
                    this.loading = false;
                }).catch(err => {
                    console.error('获取委托分类失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                    this.loading = false;
                });
            },
            onAction({ key, row }) {
                if (key === 'edit') {
                    this.handleUpdate(row.categoryId);
                } else if (key === 'delete') {
                    this.handleDelete(row.categoryId);
                } else if (key === 'toggle') {
                    this.handleEnable(row.categoryId, row.isEnabled);
                }
            },
            handleUpdate(id) {
                this.updateDialogForm = true;
                getDelegationTypeById(id).then(response => {
                    if (response.data.code == 1) {
                        this.updateForm = response.data.data;
                    } else {
                        this.$message({
                            message: response.data.msg,
                            type: "error"
                        });
                    }
                }).catch(err => {
                    console.error('获取委托分类详情失败：', err);
                    this.$message.error('请求异常，请稍后重试');
                });
            },
            updateDelegationType() {
                this.$refs.updateForm.validate(valid => {
                    if (valid) {
                        updateDelegationTypeAdmin(this.updateForm).then(response => {
                            if (response.data.code == 1) {
                                this.$message.success(response.data.msg);
                                this.updateDialogForm = false;
                                this.getList();
                            } else {
                                this.$message.error(response.data.msg);
                            }
                        }).catch(err => {
                            console.error('更新委托分类失败：', err);
                            this.$message.error('请求异常，请稍后重试');
                        });
                    } else {
                        return false;
                    }
                });
            },
            async handleDelete(id) {
                const ok = await executeConfirmedRequest(
                    deleteDelegationType,
                    id,
                    '确认删除该委托类型？',
                    '确认信息',
                    '操作成功',
                    '操作警告',
                    '操作失败，请稍后重试',
                    '操作已取消'
                );
                if (ok) {
                    this.getList();
                }
            },
            async handleEnable(id, status) {
                const action = status === true ? '禁用' : '启用';
                const ok = await executeConfirmedRequest(
                    enableDelegationType,
                    id,
                    `确认${action}该委托类型？`,
                    '确认信息',
                    '操作成功',
                    '操作警告',
                    '操作失败，请稍后重试',
                    '操作已取消'
                );
                if (ok) {
                    this.getList();
                }
            },
            async addDelegationType() {
                const ok = await executeConfirmedRequest(
                    addDelegationTypeAdmin,
                    this.addForm,
                    '确认添加该委托类型？',
                    '确认信息',
                    '操作成功',
                    '操作警告',
                    '操作失败，请稍后重试',
                    '操作已取消'
                );
                if (ok) {
                    this.addDialogForm = false;
                    this.getList();
                }
            }
        }
    }
</script>

<style lang="less" scoped>
    .dt-toolbar {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;
    }

    .dt-input {
        width: 200px;
    }

    .dt-select {
        width: 130px;
    }

    .dt-input :deep(.el-input__inner),
    .dt-select :deep(.el-input__inner) {
        height: 34px;
        background: rgba(255, 252, 245, .72);
        border: 1px solid #e6ddc9;
        border-radius: 8px;
        color: #2a3a30;
        font-family: inherit;
        font-size: 13px;
        transition: border-color .2s, box-shadow .2s, background .2s;
    }

    .dt-input :deep(.el-input__inner:focus),
    .dt-select :deep(.el-input__inner:focus) {
        border-color: #b9892c;
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13);
        background: #fffcf5;
    }

    .dt-btn {
        height: 34px;
        padding: 0 18px;
        border-radius: 8px;
        font-family: inherit;
        letter-spacing: .06em;
        font-size: 13px;
        transition: transform .18s, box-shadow .18s, background .18s, border-color .18s;
    }

    .dt-btn--primary {
        background: #2a3a30;
        border-color: #2a3a30;
        color: #f7f3ea;
    }

    .dt-btn--primary:hover,
    .dt-btn--primary:focus {
        background: #33493c;
        border-color: #33493c;
        color: #fff;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px -4px rgba(42, 58, 48, .5);
    }

    .dt-btn--ghost {
        background: transparent;
        border: 1px solid #e6ddc9;
        color: #5f6b62;
    }

    .dt-btn--ghost:hover,
    .dt-btn--ghost:focus {
        border-color: #b9892c;
        color: #96701f;
        background: rgba(255, 252, 245, .6);
    }

    .dt-btn--add {
        background: #b9892c;
        border-color: #b9892c;
        color: #fdf9ec;
    }

    .dt-btn--add:hover,
    .dt-btn--add:focus {
        background: #96701f;
        border-color: #96701f;
        color: #fff;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px -4px rgba(150, 112, 31, .5);
    }
</style>

<style lang="less">
    /* 弹窗经 append-to-body 渲染在 body 下，须用非 scoped 样式 */
    .dt-dialog {
        background: #fffcf5;
        border-radius: 16px;
        box-shadow: 0 24px 60px -20px rgba(42, 58, 48, .4);
        overflow: hidden;
    }

    .dt-dialog .el-dialog__header {
        padding: 20px 28px 16px;
        border-bottom: 1px solid #e6ddc9;
        background: rgba(255, 252, 245, .6);
    }

    .dt-dialog .el-dialog__title {
        font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
        font-size: 19px;
        font-weight: 700;
        color: #2a3a30;
    }

    .dt-dialog .el-dialog__headerbtn .el-dialog__close {
        color: #9aa198;
    }

    .dt-dialog .el-dialog__body {
        padding: 22px 28px 6px;
    }

    .dt-dialog .el-input__inner {
        background: rgba(255, 252, 245, .85);
        border: 1px solid #e6ddc9;
        border-radius: 8px;
        color: #2a3a30;
        font-family: inherit;
        font-size: 13px;
    }

    .dt-dialog .el-input__inner:focus {
        border-color: #b9892c;
        box-shadow: 0 0 0 3px rgba(185, 137, 44, .13);
    }

    .dt-dialog__footer {
        text-align: right;
        padding: 16px 28px 22px;
        border-top: 1px solid #e6ddc9;
        background: rgba(255, 252, 245, .6);
    }

    .dt-dialog__footer .el-button {
        height: 34px;
        padding: 0 18px;
        border-radius: 8px;
        font-family: inherit;
        letter-spacing: .06em;
    }

    .dt-dialog__footer .el-button + .el-button {
        margin-left: 10px;
    }

    .dt-dialog__footer .el-button--primary {
        background: #2a3a30;
        border-color: #2a3a30;
        color: #f7f3ea;
    }

    .dt-dialog__footer .el-button--primary:hover,
    .dt-dialog__footer .el-button--primary:focus {
        background: #33493c;
        border-color: #33493c;
        color: #fff;
    }

    .dt-dialog__footer .el-button:not(.el-button--primary) {
        background: transparent;
        border-color: #e6ddc9;
        color: #5f6b62;
    }

    .dt-dialog__footer .el-button:not(.el-button--primary):hover {
        border-color: #b9892c;
        color: #96701f;
        background: rgba(255, 252, 245, .6);
    }
</style>
