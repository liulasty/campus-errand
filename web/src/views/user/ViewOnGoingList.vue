<template>
    <div class="view-list-container">
        <el-card shadow="hover" class="search-card">
            <div slot="header" class="clearfix">
                <span><i class="el-icon-search"></i> 筛选查询</span>
            </div>
            <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch"
                label-width="100px">
                <el-form-item label="委托任务内容" prop="description" class="input-reader-name">
                    <el-input v-model="queryParams.description" placeholder="请输入委托内容关键词" clearable
                        @keyup.enter.native="handleQuery" />
                </el-form-item>
                <el-form-item label="委托状态" prop="status" class="input-reader-name">
                    <el-select v-model="queryParams.status" clearable>
                        <el-option label="委托发布中" value="ONGOING" />
                        <el-option label="委托已接收" value="ACCEPTED" />
                        <el-option label="委托已完成" value="COMPLETED" />
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
                        <el-option label="最新" value="0" />
                        <el-option label="最早" value="1" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                    <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>


        <el-card shadow="hover" class="table-card">
            <div slot="header" class="clearfix">
                <span><i class="el-icon-s-order"></i> 委托列表</span>
            </div>
            <el-table v-loading="loading" :data="viewOnGoingList" :row-style="{ height: '50px' }" stripe border>
                <el-table-column label="发布者ID" align="center" prop="ownerId" width="100" />
                <el-table-column label="信用分" align="center" width="110">
                    <template slot-scope="scope">
                        <el-tooltip :content="'信用等级：' + creditLevel(scope.row.ownerCredit)" placement="top">
                            <el-tag :type="creditColor(scope.row.ownerCredit)" size="small">{{ creditScore(scope.row.ownerCredit) }}</el-tag>
                        </el-tooltip>
                    </template>
                </el-table-column>
                <el-table-column label="委托类型" align="center" prop="type" width="120">
                    <template slot-scope="scope">
                        <el-tag size="small">{{ scope.row.type }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="委托描述" align="center" prop="description" show-overflow-tooltip />
                <el-table-column label="发布时间" align="center" prop="startTime" width="160" />
                <el-table-column label="截止时间" align="center" prop="endTime" width="160" />
                <el-table-column label="任务地点" align="center" prop="location" width="120" />
                <el-table-column label="状态" align="center" prop="status" width="120">
                     <template slot-scope="scope">
                        <el-tag v-if="scope.row.status===TASK_STATUS.ONGOING" type="success" size="small">发布中</el-tag>
                        <el-tag v-else-if="scope.row.status===TASK_STATUS.ACCEPTED" type="warning" size="small">已接收</el-tag>
                        <el-tag v-else-if="scope.row.status===TASK_STATUS.COMPLETED" type="success" size="small">已完成</el-tag>
                        <el-tag v-else type="info" size="small">{{ scope.row.status }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
                    <template slot-scope="scope">
                        <el-button size="mini" type="primary" plain icon="el-icon-view"
                            @click="handleView(scope.row)">查看</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container">
                <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
                    :current-page=queryParams.pageNum :page-sizes="[5, 10, 20, 50]" :page-size=queryParams.pageSize
                    layout="total, sizes, prev, pager, next, jumper" :total="total" background />
            </div>
        </el-card>

        <!-- 添加或修改存储委托信息审核记录对话框 -->
        <el-dialog :title="title" :visible.sync="open" width="880px" top="8vh" append-to-body center custom-class="detail-dialog">
            <el-row :gutter="16">
                <el-col :span="12">
                    <el-card class="box-card detail-info-card" shadow="never">
                        <div slot="header">
                            <span>发布者信息</span>
                        </div>
                        <el-form ref="form" :model="form" label-width="90px" label-position="left" class="detail-form">
                            <el-form-item label="姓名">{{form.usersInfo.name}}</el-form-item>
                            <el-form-item label="QQ">{{form.usersInfo.qqNumber}}</el-form-item>
                            <el-form-item label="电话">{{form.usersInfo.phoneNumber}}</el-form-item>
                            <el-form-item label="身份"><el-tag size="small">{{form.usersInfo.userRole}}</el-tag></el-form-item>
                            <el-form-item label="学号">{{form.usersInfo.identityNo}}</el-form-item>
                            <el-form-item label="实名">
                                <el-tag v-if="form.usersInfo.authLevel >= 1" type="success" size="small">L1 已实名</el-tag>
                                <el-tag v-else type="info" size="small">未实名</el-tag>
                            </el-form-item>
                            <el-form-item label="信用分"><el-tag size="small">{{ detailCredit }}</el-tag></el-form-item>
                        </el-form>
                    </el-card>
                </el-col>
                <el-col :span="12">
                    <el-card class="box-card detail-info-card" shadow="never">
                        <div slot="header">
                            <span>任务详情</span>
                        </div>
                        <el-form ref="formTask" :model="form" label-width="90px" label-position="left" class="detail-form">
                            <el-form-item label="任务内容">{{form.task.description}}</el-form-item>
                            <el-form-item label="任务地点">{{form.task.location}}</el-form-item>
                            <el-form-item label="委托类型">{{form.task.type}}</el-form-item>
                            <el-form-item label="截止时间">{{form.task.endTime}}</el-form-item>
                        </el-form>
                    </el-card>
                </el-col>
            </el-row>
            <el-row :gutter="16" style="margin-top:16px">
                <el-col :span="24">
                    <el-card class="box-card detail-info-card" shadow="never">
                        <div slot="header"><span>该委托发布者发布情况</span></div>
                        <div class="publish-stats">
                            <div class="stat-item">
                                <el-statistic :value="form.taskPublishedTotal" title="已发布委托数"></el-statistic>
                            </div>
                            <div class="stat-item">
                                <el-statistic :value="form.taskAcceptedTotal" title="已完成委托数"></el-statistic>
                            </div>
                            <div class="stat-item">
                                <el-statistic :value="form.taskCanceledTotal" title="已取消委托数"></el-statistic>
                            </div>
                            <div class="stat-item">
                                <el-statistic :value="form.taskOverdueTotal" title="已过期委托数"></el-statistic>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
            <div v-show="form.task.status==TASK_STATUS.COMPLETED" class="status-area">
                <el-rate v-model="taskRateValue" disabled show-score text-color="#f59e0b" score-template="{value}">
                </el-rate>
            </div>
            <div v-show="form.task.status==TASK_STATUS.ONGOING" class="status-area">
                <div v-if="!isPublisher(form.task)">
                    <div v-show="form.taskAcceptRecords === null">
                        <el-input type="textarea" v-model="delegationStr" placeholder="请输入你的接收委托留言信息"
                            size="large" rows="5" maxlength="180">
                        </el-input>
                    </div>
                    <div v-show="form.taskAcceptRecords !== null">
                        <el-tag type="info">请在我的委托-我的接收里查看详情</el-tag>
                    </div>
                </div>
                <div v-if="isPublisher(form.task)">
                    <el-tag type="info">请在我的委托-我的发布里查看详情</el-tag>
                </div>
            </div>

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
    </div>
</template>
<script>
    import { getTaskCategories } from '@/api/'
    import { listViewOnGoingList, getTaskAndPublishUserInfoByTaskId, acceptCommission } from '@/api/user.js'
    import { executeConfirmedRequest } from '@/utils/globalConfirmAction.js'
    import { creditScore, creditLevel, creditColor } from '@/utils/creditLevel'
    import { SUCCESS_CODE } from '@/constants/http'
    import { TASK_STATUS } from '@/constants/enums'
    export default {
        data() {
            return {
                TASK_STATUS,
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
                title: "委托详情",
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
                    "委托发布中": {
                        index: 0,
                        title: ["接受委托", "取消"],
                        type: ["success", "info"],
                        click: ["acceptsTheEntrustment", "cancel"]
                    },
                    "已完成": {
                        index: 1,
                        title: ["觉得该委托发布的很赞", "觉得该委托发布的很差"],
                        type: ["success", "warning"],
                        click: ["increaseGood", "increaseBad"]
                    },
                    "已接收": {
                        index: 2,
                        title: [],
                        type: [],
                        click: []
                    }
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
                    task: {}
                },
                // 当前行发布者信用分（旁证展示用）
                detailCredit: null,
                // 发布者已发布信息统计
                publishedValue: 0,
                colors: ['#99A9BF', '#F7BA2A', '#FF9900'],
                // 等同于 { 2: '#99A9BF', 4: { value: '#F7BA2A', excluded: true }, 5: '#FF9900' }
                taskRateValue: 5,


            };
        },
        created() {
            this.handleType();


        },
        mounted() {
            this.handleQuery();
        },
        methods: {
            // Vue2 模板仅能访问组件属性（methods/computed/data），import 函数需挂到 methods 才能在模板中使用
            creditScore: creditScore,
            creditLevel: creditLevel,
            creditColor: creditColor,
            /*判断当前用户是否是发布者 */
            isPublisher(task) {
                if (task.publisherId === this.$store.state.userInfo.userId) {
                    return true;
                } else {
                    return false;
                }
            },
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
                listViewOnGoingList(this.queryParams).then(response => {
                    if (response.data.code === SUCCESS_CODE) {
                        this.viewOnGoingList = response.data.data.records.map((record) => {
                            // 后端字段为 taskType，映射为前端展示用 type
                            record.type = this.taskType[`${record.taskType}`];
                            return record;
                        });
                        this.total = response.data.data.total;
                    } else {
                        this.$message.error(response.data.msg || '查询委托失败')
                    }
                    this.loading = false;
                }).catch(err => {
                    console.error('大厅列表请求失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                    this.loading = false;
                });
            },
            handleView(row) {
                console.log(row);
                this.detailCredit = row.ownerCredit;
                getTaskAndPublishUserInfoByTaskId(row.taskId).then(response => {
                    if (response.data.code === 1) {

                        this.form = response.data.data;
                        console.log(this.form);
                        this.form.usersInfo.userRole = this.identity[this.form.usersInfo.userRole];
                        this.form.task.type = this.taskType[`${this.form.task.taskType}`];
                        // console.log("form", this.form.task.status);
                        this.operation = this.operations[`${this.form.task.status}`];
                        this.open = true;
                    } else {
                        this.$message(
                            {
                                message: response.data.msg,
                                type: 'error'
                            }
                        )
                    }
                }).catch(err => {
                    console.error('查看委托详情失败：', err)
                    this.$message.error('请求异常，请稍后重试')
                });

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
                executeConfirmedRequest(acceptCommission, data, "我确认接受委托", "是否确认接受委托", "接受委托成功,等待委托发布者处理", "接受委托失败", "接受委托失败", "接受委托取消");
                this.getList();
                this.open = false;
            },
            cancel() {
                this.open = false;
            }
        }


    }
</script>
<style lang="less" scoped>
    .view-list-container {
        padding: 10px;
    }

    .search-card {
        margin-bottom: 20px;
        
        .clearfix {
            span {
                font-size: 16px;
                font-weight: 600;
                color: #303133;
                
                i {
                    margin-right: 5px;
                    color: var(--ce-primary);
                }
            }
        }
        
        .el-form-item {
            margin-bottom: 10px;
        }
    }
    
    .table-card {
        .clearfix {
            span {
                font-size: 16px;
                font-weight: 600;
                color: #303133;
                
                i {
                    margin-right: 5px;
                    color: var(--ce-primary);
                }
            }
        }
        
        .pagination-container {
            margin-top: 20px;
            text-align: right;
        }
    }
    
    .detail-info-card {
        height: 100%;
        border-radius: 10px;

        /deep/ .el-card__header {
            padding: 10px 18px;
            font-weight: 600;
            color: #374151;
            background-color: #f7f8fa;
            border-radius: 10px 10px 0 0;
        }

        .detail-form {
            .el-form-item {
                margin-bottom: 4px;
                border-bottom: 1px dashed #f0f2f5;
                padding-bottom: 4px;

                &:last-child {
                    border-bottom: none;
                }
            }

            /deep/ .el-form-item__content {
                color: #4b5563;
                line-height: 22px;
                word-break: break-all;
            }
        }
    }

    .publish-stats {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .stat-item {
            flex: 1 1 0;
            min-width: 120px;
            padding: 14px 8px;
            background: #f7f8fa;
            border-radius: 10px;
            text-align: center;

            /deep/ .el-statistic__content {
                color: var(--ce-primary);
                font-weight: 700;
            }
        }
    }

    .status-area {
        margin-top: 16px;
        padding: 14px 16px;
        border: 1px solid var(--ce-border);
        border-radius: 10px;
        background: #fbfcfd;
    }
    
    /deep/ .detail-dialog {
        border-radius: 8px;
        
        .el-dialog__header {
            border-bottom: 1px solid #ebeef5;
            padding: 20px;
        }
        
        .el-dialog__body {
            padding: 30px 20px;
            background-color: #f9fafc;
        }
    }
</style>