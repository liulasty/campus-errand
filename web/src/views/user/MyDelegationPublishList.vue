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
                    <el-option label="委托已过期" value="EXPIRED" />
                    <el-option label="委托已取消" value="CANCELLED" />

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



        <el-table v-loading="loading" :data="viewOnGoingList" :row-style="{ height: '50px' }">
            <el-table-column label="委托类型" align="center" prop="type" />
            <el-table-column label="委托描述" align="center" prop="description" show-overflow-tooltip />
            <el-table-column label="委托发布时间" align="center" width="160">
                <template slot-scope="scope">{{ scope.row.startTime | dateTime }}</template>
            </el-table-column>
            <el-table-column label="委托截止时间" align="center" width="160">
                <template slot-scope="scope">{{ scope.row.endTime | dateTime }}</template>
            </el-table-column>
            <el-table-column label="委托任务地点" align="center" prop="location" />
            <el-table-column label="委托状态" align="center" prop="status" width="180" />
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                    <el-button size="mini" type="text" icon="el-icon-view"
                        @click="handleView(scope.row)">查看详情</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
            :current-page=queryParams.pageNum :page-sizes="[3, 5, 7, 10]" :page-size=queryParams.pageSize
            layout="total, sizes, prev, pager, next, jumper" :total="total" />

        <!-- 处理接收委托信息审核框 -->
        <el-dialog :visible.sync="open" top="4vh" width="940px" :close-on-click-modal="false"
            custom-class="ce-detail-dialog">
            <div slot="title" class="ce-detail-header">
                <div class="ce-detail-header-left">
                    <span class="ce-detail-title-mark"></span>
                    <span class="ce-detail-title">委托详情</span>
                </div>
                <div class="ce-detail-header-right">
                    <span class="ce-detail-no">#{{ form.task.taskId }}</span>
                    <el-tag :type="statusTagType" effect="dark" size="small">{{ form.task.status }}</el-tag>
                </div>
            </div>

            <!-- 发布者委托统计 -->
            <div class="ce-stat-row">
                <div class="ce-stat-item">
                    <div class="ce-stat-icon is-published"><i class="el-icon-document"></i></div>
                    <div class="ce-stat-meta">
                        <div class="ce-stat-num">{{ form.taskPublishedTotal || 0 }}</div>
                        <div class="ce-stat-label">累计发布</div>
                    </div>
                </div>
                <div class="ce-stat-item">
                    <div class="ce-stat-icon is-accepted"><i class="el-icon-circle-check"></i></div>
                    <div class="ce-stat-meta">
                        <div class="ce-stat-num">{{ form.taskAcceptedTotal || 0 }}</div>
                        <div class="ce-stat-label">已完成</div>
                    </div>
                </div>
                <div class="ce-stat-item">
                    <div class="ce-stat-icon is-overdue"><i class="el-icon-time"></i></div>
                    <div class="ce-stat-meta">
                        <div class="ce-stat-num">{{ form.taskOverdueTotal || 0 }}</div>
                        <div class="ce-stat-label">已过期</div>
                    </div>
                </div>
                <div class="ce-stat-item">
                    <div class="ce-stat-icon is-canceled"><i class="el-icon-close-notification"></i></div>
                    <div class="ce-stat-meta">
                        <div class="ce-stat-num">{{ form.taskCanceledTotal || 0 }}</div>
                        <div class="ce-stat-label">已取消</div>
                    </div>
                </div>
            </div>

            <!-- 委托信息 -->
            <div class="ce-section">
                <div class="ce-section-title">
                    <i class="el-icon-tickets"></i><span>委托信息</span>
                </div>
                <div class="ce-info-grid">
                    <div class="ce-info-item">
                        <span class="ce-info-label">委托类型</span>
                        <span class="ce-info-value">{{ form.task.type }}</span>
                    </div>
                    <div class="ce-info-item">
                        <span class="ce-info-label">任务地点</span>
                        <span class="ce-info-value"><i class="el-icon-location-outline ce-info-ico"></i>{{ form.task.location }}</span>
                    </div>
                    <div class="ce-info-item">
                        <span class="ce-info-label">发布时间</span>
                        <span class="ce-info-value">{{ form.task.startTime | dateTime }}</span>
                    </div>
                    <div class="ce-info-item">
                        <span class="ce-info-label">截止时间</span>
                        <span class="ce-info-value">{{ form.task.endTime | dateTime }}</span>
                    </div>
                    <div class="ce-info-item">
                        <span class="ce-info-label">委托金额</span>
                        <span class="ce-info-value" :class="{ 'is-negotiable': form.task.money == null }">{{ moneyText(form.task.money) }}</span>
                    </div>
                    <div class="ce-info-item">
                        <span class="ce-info-label">任务编号</span>
                        <span class="ce-info-value">{{ form.task.taskId }}</span>
                    </div>
                </div>
                <div class="ce-desc">
                    <span class="ce-info-label">委托内容</span>
                    <span class="ce-desc-text">{{ form.task.description }}</span>
                </div>
            </div>

            <!-- 接单情况（发布中） -->
            <div v-if="form.task.status === TASK_STATUS.ONGOING" class="ce-section">
                <div class="ce-section-title">
                    <i class="el-icon-user"></i><span>接单情况</span>
                    <el-tag size="mini" type="info" class="ce-section-count">{{ form.taskAcceptRecords.length }}</el-tag>
                </div>
                <template v-if="form.taskAcceptRecords.length">
                    <div v-for="record in form.taskAcceptRecords" :key="record.taskAcceptRecords.acceptRecordId"
                        class="ce-applicant">
                        <div class="ce-applicant-avatar" :class="avatarClass(record.userType)">{{ avatarText(record.userName) }}</div>
                        <div class="ce-applicant-body">
                            <div class="ce-applicant-top">
                                <span class="ce-applicant-name">{{ record.userName }}</span>
                                <el-tag size="mini" :type="identityTagType(record.userType)">{{ identity[record.userType] || record.userType }}</el-tag>
                                <span class="ce-applicant-time">{{ record.taskAcceptRecords.acceptTime | dateTime }}</span>
                            </div>
                            <div class="ce-applicant-msg">
                                <i class="el-icon-chat-dot-square"></i>
                                {{ record.taskAcceptRecords.str || '（无留言）' }}
                            </div>
                            <div class="ce-applicant-foot">
                                <span class="ce-applicant-stat"><i class="el-icon-medal"></i>已完成 {{ record.taskAccomplishCount }} 次</span>
                                <span class="ce-applicant-stat"><i class="el-icon-star-on"></i>评分 {{ record.taskAccomplishGrade }}</span>
                            </div>
                        </div>
                        <div class="ce-applicant-action">
                            <el-button v-if="record.taskAcceptRecords.status === ACCEPT_STATUS.PENDING" type="primary" size="small"
                                @click="handleAccept(record.taskAcceptRecords.acceptRecordId)">
                                采取
                            </el-button>
                            <el-tag v-else size="small">{{ record.taskAcceptRecords.status }}</el-tag>
                        </div>
                    </div>
                </template>
                <div v-else class="ce-empty">
                    <i class="el-icon-s-custom"></i><span>该委托目前无人接收</span>
                </div>
            </div>

            <!-- 任务动态 -->
            <div v-if="form.task.status === TASK_STATUS.ACCEPTED || form.task.status === TASK_STATUS.COMPLETED"
                class="ce-section">
                <div class="ce-section-title">
                    <i class="el-icon-data-line"></i><span>任务动态</span>
                </div>
                <div class="ce-timeline-wrap">
                    <el-timeline v-if="taskUpdates.length">
                        <el-timeline-item v-for="(activity, index) in taskUpdates" :key="index"
                            :timestamp="activity.updateTime | dateTime"
                            :color="nodeMeta(activity.updateType) ? nodeMeta(activity.updateType).color : '#c0c4cc'">
                            <template v-if="nodeMeta(activity.updateType)">
                                <div class="node-activity">
                                    <div class="node-activity-header">
                                        <i :class="nodeMeta(activity.updateType).icon"></i>
                                        <el-tag :color="nodeMeta(activity.updateType).color" size="small" effect="dark">{{ nodeMeta(activity.updateType).label }}</el-tag>
                                        <span v-if="activity.updateDescription && activity.updateDescription !== nodeMeta(activity.updateType).label" class="node-remark">{{ activity.updateDescription }}</span>
                                    </div>
                                    <div v-if="activity.location" class="node-location">
                                        <i class="el-icon-location-outline"></i> {{ activity.location }}
                                    </div>
                                    <el-image v-if="activity.imgUrl" :src="activity.imgUrl" class="node-img" fit="cover" :preview-src-list="[activity.imgUrl]"></el-image>
                                </div>
                            </template>
                            <template v-else>
                                {{ activity.updateDescription }}
                                <el-tag size="mini" type="success">进度</el-tag>
                            </template>
                        </el-timeline-item>
                    </el-timeline>
                    <div v-else class="ce-empty">
                        <i class="el-icon-info"></i><span>暂无动态</span>
                    </div>
                </div>
            </div>

            <!-- 完成评价 -->
            <div v-if="form.task.status === TASK_STATUS.ACCEPTED || form.task.status === TASK_STATUS.COMPLETED"
                class="ce-section">
                <div class="ce-section-title">
                    <i class="el-icon-star-on"></i><span>完成评价</span>
                </div>
                <div class="ce-rate-box">
                    <el-rate v-model="taskRateValue" show-text text-color="#f59e0b"></el-rate>
                    <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 6}" placeholder="请输入完成委托评价"
                        v-model="completeTheEntrustedEvaluation"></el-input>
                </div>
            </div>

            <div slot="footer" class="dialog-footer">
                <div v-if="Array.isArray(operation.title)">
                    <el-button v-for="(item, index) in operation.title" :type="operation.type[index]" :key="index"
                        @click="handleButtonClick(operation.click[index])">
                        {{ item }}
                    </el-button>
                </div>
                <div v-else>
                    <el-button :type="operation.type" @click="handleButtonClick(operation.click)">{{ operation.title
                        }}</el-button>
                </div>
            </div>
        </el-dialog>
    </div>
</template>
<script>
    import { getTaskCategories, listDelegateUpdateRecords } from '@/api/'
    import { getNodeMeta } from '@/utils/taskNode.js'
    import {
        publishDelegationList, queryTheEntrustmentDetailsByEntrustmentNumber, confirmTheRecipient,
        cancelPublishUser, updateDelegationCompleted
    } from '@/api/user.js'
    import { executeConfirmedRequest } from '@/utils/globalConfirmAction.js'
    import { TASK_STATUS, ACCEPT_STATUS } from '@/constants/enums'
    export default {
        data() {

            return {
                TASK_STATUS,
                ACCEPT_STATUS,
                taskUpdates: [],
                //委托描述
                descriptions: "",
                count: 0,
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
                    pageSize: 5,
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
                        title: ["取消发布"],
                        type: ["warning"],
                        click: ["cancelPublish"]
                    },
                    "已接收": {
                        index: 1,
                        title: ["确认完成", "该委托未完成"],
                        type: ["success", "warning"],
                        click: ["confirmTheRecipientDelegation", "cancelTheRecipientDelegation"]
                    },
                    "已过期": {
                        index: 2,
                        title: ["回退为草稿", "删除该委托"],
                        type: ["info", "warning"],
                        click: ["fallbackDraftByPublisher", "deleteDelegation"]
                    },
                    [TASK_STATUS.CANCELLED]: {
                        index: 3,
                        title: ["回退为草稿", "删除该委托"],
                        type: ["info", "warning"],
                        click: ["fallbackDraftByPublisher", "deleteDelegation"]
                    },
                    "已完成": {
                        index: 4,
                        title: ["删除该委托"],
                        type: ["warning"],
                        click: ["deleteDelegation"]
                    },
                    "未完成": {
                        index: 5,
                        title: ["删除该委托"],
                        type: ["warning"],
                        click: ["deleteDelegation"]
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
                    taskAcceptRecords: []
                },

                taskRateValue: 1,
                completeTheEntrustedEvaluation: "",



            };
        },
        created() {
            this.handleType();


        },
        mounted() {
            this.handleQuery();
        },
        computed: {
            statusTagType() {
                const map = {
                    [TASK_STATUS.ONGOING]: 'success',
                    [TASK_STATUS.ACCEPTED]: 'warning',
                    [TASK_STATUS.COMPLETED]: 'success',
                    [TASK_STATUS.EXPIRED]: 'info',
                    [TASK_STATUS.CANCELLED]: 'danger',
                    [TASK_STATUS.UNFINISHED]: 'danger',
                    [TASK_STATUS.DRAFT]: 'info'
                }
                return map[this.form.task.status] || 'info'
            }
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
                publishDelegationList(this.queryParams).then(response => {
                    if (response.data.code == 1) {
                        this.viewOnGoingList = response.data.data.records.map((record) => {

                            record.type = this.taskType[`${record.taskType}`]; // 确保类型安全

                            return record;
                        });


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
                this.form = {
                    usersInfo: {
                        name: "",
                    },
                    task: {},
                    taskAcceptRecords: []
                }
                queryTheEntrustmentDetailsByEntrustmentNumber(row.taskId).then(response => {
                    if (response.data.code === 1) {

                        this.form = response.data.data;

                        this.form.usersInfo.userRole = this.identity[this.form.usersInfo.userRole];
                        this.form.task.type = this.taskType[`${this.form.task.taskType}`];
                        this.form.taskAcceptRecords.forEach(element => {
                            element.entrustedCompletionStatus = "已完成委托次数: " + element.taskAccomplishCount + ", 委托完成评分: " + element.taskAccomplishGrade;
                        });
                        this.operation = this.operations[`${this.form.task.status}`]
                        console.log("已发布的委托信息", this.form);
                        
                        if (this.form.task.status === TASK_STATUS.ACCEPTED || this.form.task.status === TASK_STATUS.COMPLETED) {
                            this.getTaskUpdates(this.form.task.taskId);
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

            },
            getTaskUpdates(taskId) {
                this.taskUpdates = [];
                listDelegateUpdateRecords({
                    taskId: taskId,
                    pageNum: 1,
                    pageSize: 100
                }).then(response => {
                    if (response.data.code === 1) {
                        this.taskUpdates = response.data.data.records;
                    }
                });
            },
            nodeMeta(updateType) {
                return getNodeMeta(updateType);
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
            moneyText(money) {
                if (money === null || money === undefined) return '面议'
                if (Number(money) === 0) return '免费'
                return '¥ ' + Number(money).toFixed(2)
            },
            avatarText(name) {
                return name ? String(name).charAt(0) : '?'
            },
            avatarClass(userType) {
                if (userType === 'student') return 'is-student'
                if (userType === 'teacher') return 'is-teacher'
                return 'is-admin'
            },
            identityTagType(userType) {
                if (userType === 'student') return 'success'
                if (userType === 'teacher') return 'warning'
                return 'info'
            },
            handleAccept(id) {
                executeConfirmedRequest(confirmTheRecipient, id, "是否将此委托托付给该接收者", "确认接受委托者", "确认成功,等待接收者完成委托任务", "确认失败");
                this.getList();
                this.open = false;
            },
            increaseGood() {
                executeConfirmedRequest(increaseGood, this.form.task.taskId, "是否确认增加积分", "确认增加积分", "确认成功,积分已增加", "确认失败");
            },
            decreaseGood() {
                executeConfirmedRequest(decreaseGood, this.form.task.taskId, "是否确认减少积分", "确认减少积分", "确认成功,积分已减少", "确认失败");
            },
            cancelPublish() {
                executeConfirmedRequest(cancelPublish, this.form.task.taskId, "是否确认取消发布", "确认取消发布", "确认成功,取消发布成功", "确认失败");
            },
            fallbackDraftByPublisher() {
                executeConfirmedRequest(fallbackDraftByPublisher, this.form.task.taskId, "是否确认撤回", "确认撤回", "确认成功,撤回成功", "确认失败");
            },
            deleteDelegation() {
                executeConfirmedRequest(deleteDelegation, this.form.task.taskId, "是否确认删除", "确认删除", "确认成功,删除成功", "确认失败");
            },
            cancelPublish() {
                executeConfirmedRequest(cancelPublishUser, this.form.task.taskId, "是否确认取消发布", "确认取消发布", "确认成功,取消发布成功", "确认失败");
            },
            confirmTheRecipientDelegation() {
                console.log("确认该委托已完成", this.form.task.taskId, this.completeTheEntrustedEvaluation, this.taskRateValue);
                if (this.completeTheEntrustedEvaluation && this.taskRateValue) {
                    const data = {
                        taskId: this.form.task.taskId,
                        taskAccomplishGrade: this.taskRateValue,
                        taskAccomplishReview: this.completeTheEntrustedEvaluation
                    }
                    executeConfirmedRequest(updateDelegationCompleted, data, "是否确认该委托已完成", "确认该委托已完成", "确认完成，已提交完成信息", "确认失败");
                } else {
                    this.$message({
                        message: "请填写委托完成评价和评分",
                        type: 'error'
                    })
                }
            }

        }

    }

</script>
<style lang="css" scoped>
    .el-input {
        width: 180px;
    }

    .el-select {
        width: 130px;
    }

    .my-label {
        background: #E1F3D8;
    }

    .my-content {
        background: #FDE2E2;
    }

    .node-activity {
        display: flex;
        flex-direction: column;
        gap: 4px;
    }

    .node-activity-header {
        display: flex;
        align-items: center;
        gap: 6px;
    }

    .node-activity-header i {
        font-size: 16px;
        color: #606266;
    }

    .node-remark {
        color: #303133;
        font-size: 13px;
    }

    .node-location {
        font-size: 12px;
        color: #909399;
    }

    .node-img {
        width: 90px;
        height: 90px;
        border-radius: 6px;
        border: 1px solid #ebeef5;
    }

</style>