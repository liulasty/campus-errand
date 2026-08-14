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
                <el-table-column label="发布时间" align="center" width="160">
                    <template slot-scope="scope">{{ scope.row.startTime | dateTime }}</template>
                </el-table-column>
                <el-table-column label="截止时间" align="center" width="160">
                    <template slot-scope="scope">{{ scope.row.endTime | dateTime }}</template>
                </el-table-column>
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

        <!-- 委托详情弹窗 -->
        <el-dialog :visible.sync="open" width="760px" top="6vh" append-to-body custom-class="delegation-detail"
            :close-on-click-modal="false">
            <div slot="title" class="dd-dialog-title">
                <span>委托详情</span>
                <el-tag :type="statusTagType" effect="dark" size="mini">{{ statusText }}</el-tag>
            </div>

            <!-- 任务概览 -->
            <div class="dd-overview">
                <div class="dd-overview-main">
                    <div class="dd-overview-title">{{ form.task.description }}</div>
                    <div class="dd-overview-meta">
                        <el-tag v-if="form.task.type" size="mini" effect="plain">{{ form.task.type }}</el-tag>
                        <el-tag v-if="form.task.location" size="mini" type="info" effect="plain">
                            <i class="el-icon-location-outline"></i> {{ form.task.location }}
                        </el-tag>
                    </div>
                </div>
            </div>

            <el-row :gutter="14">
                <el-col :span="12">
                    <div class="dd-section">
                        <div class="dd-section-title"><i class="el-icon-user-solid"></i> 发布者信息</div>
                        <div class="dd-section-body">
                            <div class="dd-field">
                                <span class="dd-label">姓名</span>
                                <span class="dd-value">{{ form.usersInfo.name }}</span>
                            </div>
                            <div class="dd-field">
                                <span class="dd-label">身份</span>
                                <span class="dd-value"><el-tag size="mini" type="info">{{ form.usersInfo.userRole }}</el-tag></span>
                            </div>
                            <div class="dd-field">
                                <span class="dd-label">学号</span>
                                <span class="dd-value mono">{{ form.usersInfo.identityNo }}</span>
                            </div>
                            <div class="dd-field">
                                <span class="dd-label">电话</span>
                                <span class="dd-value">{{ form.usersInfo.phoneNumber || '—' }}</span>
                            </div>
                            <div class="dd-field">
                                <span class="dd-label">QQ</span>
                                <span class="dd-value">{{ form.usersInfo.qqNumber || '—' }}</span>
                            </div>
                            <div class="dd-field">
                                <span class="dd-label">实名</span>
                                <span class="dd-value">
                                    <el-tag v-if="form.usersInfo.authLevel >= 1" type="success" size="mini">L1 已实名</el-tag>
                                    <el-tag v-else type="info" size="mini">未实名</el-tag>
                                </span>
                            </div>
                            <div class="dd-field">
                                <span class="dd-label">信用分</span>
                                <span class="dd-value"><el-tag size="mini" type="warning">{{ detailCredit }}</el-tag></span>
                            </div>
                        </div>
                    </div>
                </el-col>
                <el-col :span="12">
                    <div class="dd-section">
                        <div class="dd-section-title"><i class="el-icon-document-copy"></i> 任务信息</div>
                        <div class="dd-section-body">
                            <div class="dd-field">
                                <span class="dd-label">委托类型</span>
                                <span class="dd-value">{{ form.task.type }}</span>
                            </div>
                            <div class="dd-field">
                                <span class="dd-label">任务地点</span>
                                <span class="dd-value">{{ form.task.location }}</span>
                            </div>
                            <div class="dd-field">
                                <span class="dd-label">发布时间</span>
                                <span class="dd-value">{{ form.task.startTime | dateTime }}</span>
                            </div>
                            <div class="dd-field">
                                <span class="dd-label">截止时间</span>
                                <span class="dd-value">{{ form.task.endTime | dateTime }}</span>
                            </div>
                        </div>
                    </div>
                </el-col>
            </el-row>

            <!-- 发布统计 -->
            <div class="dd-section">
                <div class="dd-section-title"><i class="el-icon-data-line"></i> 发布者发布情况</div>
                <div class="dd-stats">
                    <div class="dd-stat">
                        <div class="dd-stat-num">{{ form.taskPublishedTotal || 0 }}</div>
                        <div class="dd-stat-label">已发布</div>
                    </div>
                    <div class="dd-stat">
                        <div class="dd-stat-num">{{ form.taskAcceptedTotal || 0 }}</div>
                        <div class="dd-stat-label">已完成</div>
                    </div>
                    <div class="dd-stat">
                        <div class="dd-stat-num">{{ form.taskCanceledTotal || 0 }}</div>
                        <div class="dd-stat-label">已取消</div>
                    </div>
                    <div class="dd-stat">
                        <div class="dd-stat-num">{{ form.taskOverdueTotal || 0 }}</div>
                        <div class="dd-stat-label">已过期</div>
                    </div>
                </div>
            </div>

            <!-- 操作区 -->
            <div v-if="form.task.status===TASK_STATUS.ONGOING" class="dd-action">
                <template v-if="!isPublisher(form.task)">
                    <el-input v-show="form.taskAcceptRecords === null" type="textarea" v-model="delegationStr"
                        placeholder="请输入你的接收委托留言信息" :rows="4" maxlength="180"></el-input>
                    <el-alert v-show="form.taskAcceptRecords !== null"
                        title="你已提交过接收申请，请在我的委托-我的接收里查看详情" type="info" :closable="false" show-icon></el-alert>
                </template>
                <el-alert v-else title="你是本委托的发布者，请在我的委托-我的发布里查看详情" type="info" :closable="false"
                    show-icon></el-alert>
            </div>
            <div v-if="form.task.status===TASK_STATUS.COMPLETED" class="dd-action dd-rate">
                <span class="dd-rate-label">委托评价</span>
                <el-rate v-model="taskRateValue" disabled show-score text-color="#f59e0b" score-template="{value}"></el-rate>
            </div>

            <div slot="footer" class="dd-footer">
                <template v-if="Array.isArray(operation.title) && operation.title.length">
                    <el-button v-for="(item, index) in operation.title" :key="index"
                        :type="operation.type[index] || 'primary'" @click="handleButtonClick(operation.click[index])">
                        {{ item }}
                    </el-button>
                </template>
                <el-button v-else-if="operation.title && !Array.isArray(operation.title)"
                    :type="operation.type || 'primary'" @click="handleButtonClick(operation.click)">
                    {{ operation.title }}
                </el-button>
                <el-button v-if="!(Array.isArray(operation.title) ? operation.title.length : operation.title)"
                    type="info" plain @click="cancel">关闭</el-button>
            </div>
        </el-dialog>
    </div>
</template>
<script>
    import { getTaskCategories, addReview } from '@/api/'
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
        computed: {
            // 委托状态展示（后端返回中文 webValue，与 TASK_STATUS 映射一致）
            statusText() {
                if (this.form.task.status === TASK_STATUS.ONGOING) return '发布中'
                if (this.form.task.status === TASK_STATUS.ACCEPTED) return '已接收'
                if (this.form.task.status === TASK_STATUS.COMPLETED) return '已完成'
                return this.form.task.status || ''
            },
            statusTagType() {
                if (this.form.task.status === TASK_STATUS.ACCEPTED) return 'warning'
                return 'success'
            }
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
                }).catch(err => {
                    console.error('获取委托分类失败：', err)
                    this.$message.error('请求异常，请稍后重试')
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
            async acceptsTheEntrustment() {
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
                const ok = await executeConfirmedRequest(acceptCommission, data, "我确认接受委托", "是否确认接受委托", "接受委托成功,等待委托发布者处理", "接受委托失败", "接受委托失败", "接受委托取消");
                if (ok) {
                    this.getList();
                    this.open = false;
                }
            },
            cancel() {
                this.open = false;
            },
            /** 已完成委托「赞」 */
            increaseGood() {
                this.submitReview(5, '觉得该委托发布的很赞')
            },
            /** 已完成委托「差」 */
            increaseBad() {
                this.submitReview(1, '觉得该委托发布的很差')
            },
            submitReview(rate, comment) {
                if (!this.form.task || !this.form.task.taskId) return
                addReview({ taskId: this.form.task.taskId, rate, comment }).then(res => {
                    if (res.data.code === 1) {
                        this.$message.success('评价成功')
                    } else {
                        this.$message.error(res.data.msg || '评价失败')
                    }
                }).catch(err => {
                    console.error('评价失败：', err)
                    this.$message.error('评价失败，请稍后重试')
                })
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
    
    /* ---------- 委托详情弹窗 ---------- */
    .dd-overview {
        padding: 18px 20px;
        border-radius: 12px;
        background: linear-gradient(135deg, var(--ce-primary-light) 0%, #ffffff 78%);
        border: 1px solid #bfe0d8;
        margin-bottom: 14px;

        .dd-overview-main {
            min-width: 0;
        }

        .dd-overview-title {
            font-size: 16px;
            font-weight: 700;
            color: var(--ce-text);
            line-height: 1.5;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }

        .dd-overview-meta {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-top: 10px;

            i {
                margin-right: 3px;
            }
        }
    }

    .dd-section {
        background: #fff;
        border: 1px solid var(--ce-border);
        border-radius: 12px;
        overflow: hidden;
        margin-bottom: 14px;

        .dd-section-title {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 10px 16px;
            background: #f7f8fa;
            font-weight: 600;
            font-size: 13px;
            color: #374151;
            border-bottom: 1px solid var(--ce-border);

            i {
                color: var(--ce-primary);
            }
        }

        .dd-section-body {
            padding: 4px 16px;
        }

        .dd-field {
            display: flex;
            padding: 9px 0;
            border-bottom: 1px dashed #f0f2f5;

            &:last-child {
                border-bottom: none;
            }

            .dd-label {
                flex-shrink: 0;
                width: 68px;
                color: var(--ce-text-2);
                font-size: 13px;
            }

            .dd-value {
                flex: 1;
                min-width: 0;
                color: #374151;
                font-size: 13px;
                line-height: 20px;
                word-break: break-all;

                &.mono {
                    font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
                }
            }
        }
    }

    .dd-stats {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 10px;
        padding: 14px 16px;

        .dd-stat {
            text-align: center;
            padding: 12px 6px;
            background: #f7f8fa;
            border-radius: 10px;

            .dd-stat-num {
                font-size: 22px;
                font-weight: 700;
                color: var(--ce-primary);
                font-variant-numeric: tabular-nums;
                line-height: 1;
            }

            .dd-stat-label {
                margin-top: 6px;
                font-size: 12px;
                color: var(--ce-text-2);
            }
        }
    }

    .dd-action {
        padding: 14px 16px;
        border: 1px solid var(--ce-border);
        border-radius: 12px;
        background: #fbfcfd;
        margin-bottom: 4px;

        &.dd-rate {
            display: flex;
            align-items: center;
            gap: 14px;

            .dd-rate-label {
                font-size: 13px;
                color: var(--ce-text-2);
            }
        }
    }

    .dd-footer {
        text-align: right;

        .el-button + .el-button {
            margin-left: 10px;
        }
    }
</style>

<style lang="less">
    /* 委托详情弹窗壳（append-to-body 渲染在 body 下，须用非 scoped 样式） */
    .delegation-detail {
        border-radius: 16px;
        overflow: hidden;

        .el-dialog__header {
            padding: 16px 20px;
            border-bottom: 1px solid var(--ce-border);
        }

        .el-dialog__headerbtn {
            top: 14px;

            .el-dialog__close {
                color: var(--ce-text-2);
                font-size: 18px;

                &:hover {
                    color: var(--ce-primary);
                }
            }
        }

        .el-dialog__body {
            padding: 16px 20px;
            background: #f9fafc;
        }

        .dd-dialog-title {
            display: flex;
            align-items: center;
            gap: 10px;
            font-weight: 600;
        }
    }
</style>