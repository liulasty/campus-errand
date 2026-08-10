<template>
    <div>
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch"
            label-width="100px">
            <el-form-item label="委托任务内容" prop="description" class="input-reader-name">
                <el-input v-model="queryParams.description" placeholder="请输入委托内容关键词" clearable
                    @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item label="委托状态" prop="status" class="input-reader-name">
                <el-select v-model="queryParams.status" clearable>
                    <el-option label="委托发布中" value="ONGOING" />
                    <el-option label="委托已完成" value="ACCEPTED" />
                </el-select>
            </el-form-item>
            <el-form-item label="委托类型" prop="taskType" class="input-reader-name">
                <el-select v-model="queryParams.taskType" clearable>
                    <el-option v-for="dict in taskTypeOption" :key="dict.value" :label="dict.label"
                        :value="dict.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="委托任务地点" prop="Location" class="input-reader-name">
                <el-select v-model="queryParams.location" clearable>
                    <el-option v-for="dict in locationType" :key="dict.value" :label="dict.label" :value="dict.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="发布时间" prop="queryRules" class="input-reader-name">
                <el-select v-model="queryParams.queryRules">
                    <el-option label="最新" value="1" />
                    <el-option label="最早" value="0" />
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>



        <el-table v-loading="loading" :data="viewOnGoingList" :row-style="{ height: '50px' }">
            <el-table-column label="委托接收记录编号" align="center" prop="id" />
            <el-table-column label="委托类型" align="center" prop="type" />
            <el-table-column label="委托描述" align="center" prop="description" show-overflow-tooltip />
            <el-table-column label="委托留言时间" align="center" width="160">
                <template slot-scope="scope">{{ scope.row.acceptTime | dateTime }}</template>
            </el-table-column>
            <el-table-column label="委托留言" align="center" prop="str" />
            <el-table-column label="委托任务地点" align="center" prop="location" />
            <el-table-column label="委托状态" align="center" prop="taskStatus" width="180" />
            <el-table-column label="委托处理状态" align="center" prop="status" />
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                    <el-button size="mini" type="text" icon="el-icon-view"
                        @click="handleView(scope.row)">查看详情</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
            :current-page=queryParams.pageNum :page-sizes="[5, 7, 10, 15]" :page-size=queryParams.pageSize
            layout="total, sizes, prev, pager, next, jumper" :total="total" />

        <!-- 添加或修改存储委托信息审核记录对话框 -->
        <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
            <el-form ref="form" :model="form" label-width="110px">
                <el-form-item label="发布者信息" prop="usersInfo">
                    <el-form-item label="发布者名字" prop="name">
                        {{form.usersInfo.name}}
                    </el-form-item>
                    <el-form-item label="发布者QQ" prop="qqNumber">
                        {{form.usersInfo.qqNumber}}
                    </el-form-item>
                    <el-form-item label="发布者电话" prop="phoneNumber">
                        {{form.usersInfo.phoneNumber}}
                    </el-form-item>
                    <el-form-item label="发布者身份" prop="userRole">
                        {{form.usersInfo.userRole}}
                    </el-form-item>
                    <el-form-item label="发布者学号" prop="identityNo">
                        {{form.usersInfo.identityNo}}
                    </el-form-item>
                    <el-form-item label="发布者实名" prop="authLevel">
                        <el-tag v-if="form.usersInfo.authLevel >= 1" type="success" size="mini">L1 已实名</el-tag>
                        <el-tag v-else type="info" size="mini">未实名</el-tag>
                    </el-form-item>
                </el-form-item>
                <el-form-item label="委托内容" prop="task">
                    <el-form-item label="委托任务内容" prop="name">
                        {{form.task.description}}
                    </el-form-item>
                    <el-form-item label="委托任务地点" prop="qqNumber">
                        {{form.task.location}}
                    </el-form-item>
                    <el-form-item label="委托类型" prop="phoneNumber">
                        {{form.task.type}}
                    </el-form-item>
                    <el-form-item label="委托截止时间" prop="userRole">
                        {{ form.task.endTime | dateTime }}
                    </el-form-item>
                    <el-form-item label="委托金额" prop="money">
                        {{form.task.money}} 元
                    </el-form-item>
                </el-form-item>
                <el-form-item label="留言" prop="delegationStr">
                    {{ form.acceptMessage}}
                </el-form-item>
            </el-form>
            <el-card v-show="form.task.status === TASK_STATUS.ACCEPTED" class="box-card node-card" style="margin-top: 10px;">
                <div slot="header" class="clearfix">
                    <span>履约打卡</span>
                    <el-button style="float: right; padding: 3px 0" type="text" icon="el-icon-view"
                        @click="viewProgress">进度详情</el-button>
                </div>
                <div class="node-buttons">
                    <el-button
                        v-for="node in TASK_NODE_TYPES"
                        :key="node.dbValue"
                        :type="isNodeSubmitted(node.dbValue) ? 'info' : 'primary'"
                        :icon="node.icon"
                        :disabled="isNodeSubmitted(node.dbValue)"
                        @click="openNodeDialog(node)">
                        {{ node.label }}
                    </el-button>
                </div>
            </el-card>
            <div slot="footer" class="dialog-footer">
                <div v-if="Array.isArray(operation.title)">
                    <!-- 多个按钮的情况 -->
                    <el-button v-for="(item, index) in operation.title" :type="operation.type[index]" :key="index"
                        @click="handleButtonClick(operation.click[index])">
                        {{ item }}
                    </el-button>
                </div>
                <div v-else>
                    <!-- 单个按钮的情况 -->
                    <el-button :type="operation.type" @click="handleButtonClick(operation.click)">{{ operation.title
                        }}</el-button>
                </div>
            </div>
        </el-dialog>

        <!-- 履约节点打卡对话框 -->
        <el-dialog :title="nodeForm.nodeLabel + '打卡'" :visible.sync="nodeDialogVisible" width="520px" append-to-body>
            <el-form :model="nodeForm" label-width="90px">
                <el-form-item label="打卡图片">
                    <el-upload action="#" :show-file-list="false" :http-request="handleUpload" accept="image/*">
                        <el-button size="small" type="primary" icon="el-icon-upload2">上传打卡图片</el-button>
                    </el-upload>
                    <div v-if="nodeForm.imgUrl" class="node-img-preview">
                        <img :src="nodeForm.imgUrl" alt="打卡图片" />
                        <el-button size="mini" type="text" @click="nodeForm.imgUrl = ''">移除图片</el-button>
                    </div>
                </el-form-item>
                <el-form-item label="打卡定位">
                    <el-input v-model="nodeForm.location" placeholder="请输入定位坐标或地点">
                        <el-button slot="append" @click="fillMockLocation">模拟定位</el-button>
                    </el-input>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input type="textarea" v-model="nodeForm.remark" placeholder="请输入简短备注（可选）" :rows="3"></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button @click="nodeDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="nodeSubmitting" @click="submitNode">提交打卡</el-button>
            </div>
        </el-dialog>
    </div>
</template>
<script>
    import { getTaskCategories, addTaskNodeUpdate, uploadImg, listDelegateUpdateRecords } from '@/api/'
    import { TASK_NODE_TYPES, MOCK_LOCATION, getNodeMeta } from '@/utils/taskNode.js'
    import { acceptDelegationList, queryTheEntrustmentDetailsByEntrustmentNumber, acceptCommission, cancelAcceptorByAcceptor, getTaskAcceptById } from '@/api/user.js'
    import { executeConfirmedRequest } from '@/utils/globalConfirmAction.js'
    import { ACCEPT_STATUS, TASK_STATUS } from '@/constants/enums'
    export default {
        data() {
            return {
                ACCEPT_STATUS,
                TASK_STATUS,
                nodeDialogVisible: false,
                nodeSubmitting: false,
                nodeForm: {
                    nodeType: '',
                    nodeLabel: '',
                    imgUrl: '',
                    location: '',
                    remark: ''
                },
                // 当前任务的履约节点记录与已提交节点集合
                taskUpdates: [],
                submittedNodes: new Set(),
                TASK_NODE_TYPES: TASK_NODE_TYPES,
                // 遮罩层
                loading: true,
                //委托留言
                delegationStr: "",

                // 显示搜索条件
                showSearch: true,
                // 总条数
                total: 0,
                // 存储委托记录表格数据
                viewOnGoingList: [],
                // 弹出层标题
                title: "",
                // 是否显示弹出层
                open: false,
                // 查询参数
                queryParams: {
                    pageNum: 1,
                    pageSize: 10,
                },
                // 地点类型数组
                locationType: [
                    {
                        value: '教学楼',
                        label: '教学楼'
                    },
                    {
                        value: '图书馆',
                        label: '图书馆'
                    },
                    {
                        value: '食堂',
                        label: '食堂'
                    },
                    {
                        value: '运动场',
                        label: '运动场'
                    },
                    {
                        value: '实验室',
                        label: '实验室'
                    },
                    {
                        value: '其他',
                        label: '其他'
                    },
                ],
                //委托操作类型
                taskTypeOption: [
                    { label: "委托", value: 1 },
                    { label: "取消委托", value: 2 }
                ],
                //委托类型
                taskType: {
                },
                operations: {
                    [ACCEPT_STATUS.PENDING]: {
                        index: 0,
                        title: ["取消待接收"],
                        type: ["info"],
                        click: ["cancelAcceptor"]
                    },
                    [ACCEPT_STATUS.CANCEL]: {
                        index: 1,
                        title: [],
                        type: [],
                        click: []
                    },
                    [ACCEPT_STATUS.EXPIRED]: {
                        index: 2,
                        title: [],
                        type: [],
                        click: []
                    },
                    [ACCEPT_STATUS.UNCHECKED]: {
                        index: 3,
                        title: [],
                        type: [],
                        click: []
                    },
                    [ACCEPT_STATUS.CHECKED]: {
                        index: 4,
                        title: ["觉得很赞", "觉得很差"],
                        type: ["success", "warning"],
                        click: ["increaseGood", "increaseBad"]
                    },

                },
                operation: {},
                //身份信息
                identity: {
                    'student': '学生',
                    "teacher": "老师",
                    "admin": "管理员"
                },


                // 表单参数
                form: {
                    usersInfo: {
                        name: "",
                    },
                    task: {},
                    acceptMessage: "",
                },
            };
        },
        created() {
            this.handleType();


        },
        mounted() {
            this.handleQuery();
        },
        methods: {
            /** 获取委托类型操作 */
            handleType() {
                //获取类型
                getTaskCategories().then((data) => {
                    this.taskTypeOption = [];
                    if (data.data.code === 1) {

                        if (data.data.data.length > 0) {

                            const taskCategories = data.data.data;

                            for (let i = 0; i < taskCategories.length; i++) {
                                //生成键值对
                                this.taskType[`${taskCategories[i].id}`] = `${taskCategories[i].name}`
                                this.taskTypeOption.push({ label: taskCategories[i].name, value: taskCategories[i].id })
                            }
                            // console.log("类型信息", this.tabPanes);
                            // console.log(this.taskType);
                            // console.log("类型数组", this.taskTypeOption);
                        }
                    }
                })
            },
            /** 搜索按钮操作 */
            handleQuery() {
                this.queryParams.pageNum = 1;

                // console.log("搜索参数：", this.queryParams);
                this.getList();
            },
            /** 重置按钮操作 */
            resetQuery() {
                this.resetForm("queryForm");
                this.handleQuery();
            },
            getList() {
                this.loading = true;
                acceptDelegationList(this.queryParams).then(response => {
                    if (response.data.code == 1) {
                        this.viewOnGoingList = response.data.data.records.map((record) => {

                            record.type = this.taskType[`${record.taskType}`]; // 确保类型安全

                            return record;
                        });
                        console.log("列表数据", this.viewOnGoingList);

                        this.total = response.data.data.total;
                        this.loading = false;

                    } else {
                        this.$message({
                            message: response.data.msg,
                            type: 'error'
                        });
                        this.loading = false;
                    }

                });
            },
            handleView(row) {
                console.log(row);
                this.delegationStr = ""
                getTaskAcceptById(row.taskId).then(response => {
                    if (response.data.code === 1) {
                        console.log("接收记录详情", response.data.data);
                        this.form.acceptMessage = response.data.data.str;
                        queryTheEntrustmentDetailsByEntrustmentNumber(row.taskId).then(response => {
                            if (response.data.code === 1) {
                                this.form.task = response.data.data.task;
                                this.form.usersInfo = response.data.data.usersInfo;
                                this.form.usersInfo.userRole = this.identity[this.form.usersInfo.userRole];
                                this.form.task.type = this.taskType[`${this.form.task.taskType}`];
                                this.operation = this.operations[`${row.status}`];
                                this.form.id = row.id;
                                console.log("委托接收记录详情", this.form);
                                if (this.form.task.status === TASK_STATUS.ACCEPTED) {
                                    this.loadTaskUpdates(this.form.task.taskId);
                                }
                                this.open = true;
                            } else {
                                this.$message(
                                    {
                                        message: response.data.msg,
                                        type: 'error'
                                    }
                                )
                            }
                        });
                    } else {
                        this.$message(
                            {
                                message: response.data.msg,
                                type: 'error'
                            }
                        )
                        return;
                    }
                })


            },
            handleSizeChange(val) {
                console.log(`每页 ${val} 条`);
                this.queryParams.pageSize = val
                this.getList();
            },
            handleCurrentChange(val) {
                console.log(`当前页: ${val}`);
                this.queryParams.pageNum = val
                this.getList();
            },
            resetForm(formRef) {
                if (this.$refs[formRef]) {
                    this.$refs[formRef].resetFields();
                    console.log("表单已重置！");
                } else {
                    console.error("未找到指定的表单引用！");
                }
            },
            /** 引导按钮操作 */
            handleButtonClick(actionName) {
                console.log("点击按钮", actionName);
                // 在这里实现点击按钮时调用的逻辑，例如：
                this[actionName]()
                // this[actionName]() 或者 this.$emit(actionName)
                // 具体实现取决于您的项目需求和上下文
            },
            /** 取消接收 */
            cancelAcceptor() {
                const acceptRecordId = this.form.id;
                console.log("委托接收记录id", acceptRecordId)
                executeConfirmedRequest(cancelAcceptorByAcceptor, acceptRecordId, "确认取消接受委托", "确认取消接受委托", "取消委托成功", "取消接受委托失败", "取消接受委托失败", "取消接受委托取消")
            },
            /** 提交留言 */
            acceptsTheEntrustment() {
                const data = {
                    task: this.form.task.taskId,
                    str: this.delegationStr,
                    user: this.$store.state.userInfo.userId
                }
                console.log("提交留言", this.delegationStr);
                if (this.delegationStr === "") {
                    this.$message(
                        {
                            message: "请留下你所提供的信息",
                            type: 'error'
                        }
                    )
                    return;
                }
                executeConfirmedRequest(acceptCommission, data, "确认接受委托", "确认接受委托", "接受委托成功,等待委托发布者处理", "接受委托失败", "接受委托失败", "接受委托取消");
                this.getList();
                this.open = false;
            },
            loadTaskUpdates(taskId) {
                this.taskUpdates = [];
                listDelegateUpdateRecords({
                    taskId: taskId,
                    pageNum: 1,
                    pageSize: 100
                }).then(response => {
                    if (response.data.code === 1) {
                        this.taskUpdates = response.data.data.records;
                        const nodes = new Set();
                        this.taskUpdates.forEach(record => {
                            const meta = getNodeMeta(record.updateType);
                            if (meta) {
                                nodes.add(meta.dbValue);
                            }
                        });
                        this.submittedNodes = nodes;
                    }
                });
            },
            isNodeSubmitted(dbValue) {
                return this.submittedNodes.has(dbValue);
            },
            viewProgress() {
                this.$router.push('/myDelegationProgress?taskId=' + this.form.task.taskId)
            },
            openNodeDialog(node) {
                this.nodeForm = {
                    nodeType: node.dbValue,
                    nodeLabel: node.label,
                    imgUrl: '',
                    location: '',
                    remark: ''
                };
                this.nodeDialogVisible = true;
            },
            handleUpload(option) {
                const file = option.file;
                if (file.size > 5 * 1024 * 1024) {
                    this.$message.error('图片大小不能超过 5MB');
                    return;
                }
                if (!/\.(jpg|jpeg|png|gif)$/i.test(file.name)) {
                    this.$message.error('仅支持 JPG、PNG、GIF 格式图片');
                    return;
                }
                uploadImg(file).then(result => {
                    if (result.data.code === 1) {
                        this.nodeForm.imgUrl = 'http://' + result.data.data;
                        this.$message.success('图片上传成功');
                    } else {
                        this.$message.error(result.data.msg || '图片上传失败');
                    }
                }).catch(err => {
                    console.error('图片上传失败：', err)
                    this.$message.error('图片上传异常，请稍后重试')
                });
            },
            fillMockLocation() {
                this.nodeForm.location = MOCK_LOCATION;
            },
            async submitNode() {
                if (!this.nodeForm.nodeType) {
                    return;
                }
                const data = {
                    taskId: this.form.task.taskId,
                    nodeType: this.nodeForm.nodeType,
                    imgUrl: this.nodeForm.imgUrl,
                    location: this.nodeForm.location,
                    remark: this.nodeForm.remark
                };
                this.nodeSubmitting = true;
                try {
                    const response = await addTaskNodeUpdate(data);
                    if (response.data.code === 1) {
                        this.$message.success('打卡成功');
                        this.nodeDialogVisible = false;
                        this.loadTaskUpdates(this.form.task.taskId);
                    } else {
                        this.$message.error(response.data.msg || '打卡失败');
                    }
                } catch (e) {
                    this.$message.error('打卡失败');
                } finally {
                    this.nodeSubmitting = false;
                }
            }
        }


    }
</script>
<style scoped>
    .el-input {
        width: 180px;
    }

    .el-select {
        width: 130px;
    }

    .node-buttons {
        display: flex;
        gap: 12px;
    }

    .node-img-preview {
        margin-top: 10px;
    }

    .node-img-preview img {
        max-width: 180px;
        max-height: 180px;
        border-radius: 6px;
        border: 1px solid #ebeef5;
        display: block;
        margin-bottom: 4px;
    }
</style>