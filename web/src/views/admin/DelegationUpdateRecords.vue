<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch"
            label-width="98px">
            <el-form-item label="委托更新描述" prop="DelegateComment">
                <el-input v-model="queryParams.DelegateComment" placeholder="委托更新描述" clearable
                    @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item label="更新结果状态" prop="ReviewStatus">
                <el-select v-model="queryParams.ReviewStatus" placeholder="请选择更新结果状态" clearable>
                    <el-option v-for="(value, key) in type" :key="value" :label="value" :value="key" />
                </el-select>
            </el-form-item>
            <el-form-item label="更新完成时间" prop="ReviewTime">
                <el-date-picker clearable v-model="queryParams.ReviewTime" type="date" value-format="yyyy-MM-dd"
                    placeholder="请选择更新完成时间">
                </el-date-picker>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>



        <el-table v-loading="loading" :data="delegateUpdatesRecordsList" @selection-change="handleSelectionChange">
            <el-table-column label="更新记录编号" align="center" prop="updateId" />
            <el-table-column label="委托任务ID" align="center" prop="taskId" />
            <el-table-column label="操作人员" align="center">
                <template slot-scope="scope">{{ scope.row.userName || scope.row.userId }}</template>
            </el-table-column>
            <el-table-column label="更新类型" align="center" width="130" :filters="autoAdvanceFilters" :filter-method="filterUpdateType">
                <template slot-scope="scope">
                    <el-tag v-if="scope.row.updateType === TASK_UPDATE_TYPE.AUTO_ADVANCE" type="danger" size="small" effect="dark">系统自动推进</el-tag>
                    <el-tag v-else-if="nodeMeta(scope.row.updateType)" :color="nodeMeta(scope.row.updateType).color" size="small" effect="dark">
                        {{ nodeMeta(scope.row.updateType).label }}
                    </el-tag>
                    <el-tag v-else size="small" type="info">{{ scope.row.updateType }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="节点进度" align="center" width="80">
                <template slot-scope="scope">
                    <span v-if="scope.row.nodeIndex">{{ scope.row.nodeIndex }}/3</span>
                    <span v-else>-</span>
                </template>
            </el-table-column>
            <el-table-column label="更新内容" align="center" prop="updateDescription" show-overflow-tooltip />
            <el-table-column label="打卡图片" align="center" width="100">
                <template slot-scope="scope">
                    <el-image v-if="scope.row.imgUrl" :src="scope.row.imgUrl" fit="cover" :preview-src-list="[scope.row.imgUrl]" style="width: 50px; height: 50px; border-radius: 4px;"></el-image>
                    <span v-else>-</span>
                </template>
            </el-table-column>
            <el-table-column label="打卡定位" align="center" prop="location" show-overflow-tooltip />
            <el-table-column label="更新时间" align="center" width="150">
                <template slot-scope="scope">
                    <span>{{ scope.row.updateTime | dateTime }}</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                    <el-button size="mini" type="text" icon="el-icon-edit" @click="handleView(scope.row)">查看</el-button>
                    <el-button size="mini" type="text" icon="el-icon-delete"
                        @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
            :current-page=queryParams.pageNum :page-sizes="[5, 7, 10, 15]" :page-size=queryParams.pageSize
            layout="total, sizes, prev, pager, next, jumper" :total="total">
        </el-pagination>

        <!-- 添加或修改存储委托信息审核记录对话框 -->
        <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
            <el-form ref="form" :model="form" label-width="120px">
                <el-form-item label="委托任务ID" prop="DelegateID">
                    {{form.taskId}}
                </el-form-item>
                <el-form-item label="管理员ID" prop="UserID">
                    {{form.userId}}
                </el-form-item>
                <el-form-item label="审核意见或说明" prop="ReviewComment">
                    {{form.updateDescription}}
                </el-form-item>
                <el-form-item label="审核完成时间" prop="ReviewTime">
                    {{ form.updateTime | dateTime }}
                </el-form-item>
            </el-form>
        </el-dialog>
    </div>
</template>

<script>
    import { listDelegateUpdateRecords, delDelegateUpdateRecords, getDelegateUpdateType, getViewDelegateRecord } from "@/api/";
    import { getNodeMeta } from '@/utils/taskNode.js';

    import initLayui from "@/layui/layuiInit";

    import { executeConfirmedRequest } from '@/utils/globalConfirmAction'
    import { TASK_UPDATE_TYPE } from '@/constants/enums'

    export default {
        name: "Delegateauditrecords",
        data() {
            return {
                TASK_UPDATE_TYPE,
                // 遮罩层
                loading: true,
                // 选中数组
                ids: [],
                // 非单个禁用
                single: true,
                // 非多个禁用
                multiple: true,
                // 显示搜索条件
                showSearch: true,
                // 总条数
                total: 0,
                // 存储委托信息审核记录表格数据
                delegateUpdatesRecordsList: [],
                // 弹出层标题
                title: "",
                // 是否显示弹出层
                open: false,
                autoAdvanceFilters: [
                    { text: '仅系统自动推进', value: 'auto' }
                ],
                // 查询参数
                queryParams: {
                    pageNum: 1,
                    pageSize: 10,
                    ReviewStatus: null,
                    ReviewComment: null,
                    ReviewTime: null
                },
                // 类型数组
                type: [
                    { label: "已批准", value: 1 },
                    { label: "已拒绝", value: 2 },
                    { label: "待定", value: 3 }
                ],
                // 表单参数
                form: {},
                // 表单校验
                rules: {
                    DelegateID: [
                        { required: true, message: "委托任务ID不能为空", trigger: "blur" }
                    ],
                    UserID: [
                        { required: true, message: "管理员ID不能为空", trigger: "blur" }
                    ],
                    ReviewStatus: [
                        { required: true, message: "审核结果状态不能为空", trigger: "change" }
                    ],
                }
            };
        },
        created() {
            this.getTaskUpdateType()
            this.getList();
        },
        methods: {
            filterUpdateType(value, row) {
                return row.updateType === TASK_UPDATE_TYPE.AUTO_ADVANCE
            },
            getTaskUpdateType() {
                getDelegateUpdateType().then(request => {
                    console.log("响应结果", request);
                    if (request.data.code == 1) {
                        this.type = request.data.data
                    }
                }).catch(err => {
                    console.error('获取更新类型失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                })
            },
            nodeMeta(updateType) {
                return getNodeMeta(updateType);
            },
            /** 查询存储委托信息审核记录列表 */
            getList() {
                this.loading = true;

                listDelegateUpdateRecords(this.queryParams).then(response => {
                    if (response.data.code != 1) {
                        this.$message({
                            message: response.data.msg || "获取数据失败",
                            type: "error"
                        });
                    } else {
                        this.delegateUpdatesRecordsList = response.data.data.records;
                        console.log("响应结果", this.delegateUpdatesRecordsList);
                        this.total = response.data.data.total;
                    }
                    this.loading = false;
                }).catch(err => {
                    console.error('获取委托更新记录失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                    this.loading = false;
                });
            },
            // 取消按钮
            cancel() {
                this.open = false;
                this.reset();
            },
            // 表单重置
            reset() {
                this.form = {
                    RecordID: null,
                    DelegateID: null,
                    UserID: null,
                    ReviewStatus: null,

                    ReviewTime: null
                };
                this.resetForm("form");
            },
            /** 搜索按钮操作 */
            handleQuery() {
                this.queryParams.pageNum = 1;
                console.log("搜索参数：", this.queryParams);
                this.getList();
            },
            /** 重置按钮操作 */
            resetQuery() {
                this.resetForm("queryForm");
                this.handleQuery();
            },
            // 多选框选中数据
            handleSelectionChange(selection) {
                this.ids = selection.map(item => item.RecordID)
                this.single = selection.length !== 1
                this.multiple = !selection.length
            },
            /** 新增按钮操作 */
            handleAdd() {
                this.reset();
                this.open = true;
                this.title = "添加存储委托信息审核记录";
            },
            /** 查看按钮操作 */
            handleView(row) {
                // this.reset();
                const RecordID = row.updateId
                getViewDelegateRecord(RecordID).then(response => {
                    if (response.data.code === 1 && response.data.data) {
                        this.form = response.data.data;
                        this.open = true;
                        this.title = "查看委托信息审核记录";
                    } else {
                        this.$message.error(response.data.msg || '获取记录详情失败');
                    }
                }).catch(err => {
                    console.error('获取记录详情失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                });
            },
            /** 删除按钮操作 */
            async handleDelete(row) {
                const RecordIDs = row.updateId;

                const ok = await executeConfirmedRequest(delDelegateUpdateRecords, RecordIDs, '是否确认删除存储委托信息记录编号为"' + RecordIDs + '"的数据项？', '删除委托更新记录');
                if (ok) {
                    this.getList();
                }
            },

            handleSizeChange(val) {
                console.log(`每页 ${val} 条`);
                this.queryParams.pageSize = val
                this.getList()
            },
            handleCurrentChange(val) {
                console.log(`当前页: ${val}`);
                this.queryParams.pageNum = val
                this.getList()
            },
        }
    };
</script>