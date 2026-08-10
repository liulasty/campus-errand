<template>
    <div class="notice-center">
        <el-tabs @tab-click="handleClick">
            <el-tab-pane v-for="notice in noticeType" :key="notice.value" :label="notice.label">
                <el-card class="notice-table-card" shadow="never">
                    <el-table :data="tableData" style="width: 100%" :empty-text="'暂无通知'">
                        <el-table-column prop="date" label="日期" width="170">
                        </el-table-column>
                        <el-table-column prop="title" label="主题" show-overflow-tooltip>
                        </el-table-column>
                        <el-table-column prop="isRead" label="状态" width="90" align="center">
                            <template slot-scope="scope">
                                <el-tag v-if="scope.row.isRead == 0" type="success" size="small">未读</el-tag>
                                <el-tag v-else type="info" size="small">已读</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="90" align="center">
                            <template slot-scope="scope">
                                <el-button type="primary" size="small" plain @click="handleView(scope.row.id)">查看</el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </el-card>
            </el-tab-pane>
        </el-tabs>

        <el-dialog width="480px" title="通知详情" :visible.sync="innerVisible" append-to-body>
            <div class="notice-detail">
                <h2 class="notice-title">{{ notice.title }}</h2>
                <div class="notice-meta">发送时间：{{ notice.date | dateTime }}</div>
                <div class="notice-content">{{ notice.description }}</div>
            </div>
        </el-dialog>
    </div>

</template>
<script>

    import {
        getPersonalNoticeList, getNoticeById,
    } from "@/api/user.js"
    import { NOTIFICATION_TYPE } from '@/constants/enums'


    export default {
        props: {
            userId: {
                type: Number,
                default: 0
            },

        },
        data() {
            return {
                NOTIFICATION_TYPE,
                dialogVisibleEdit: false,
                dialogVisibleReason: false,
                dialogVisiblePublish: false,
                noticeType: [
                    {
                        value: NOTIFICATION_TYPE.OWN,
                        label: NOTIFICATION_TYPE.OWN,
                    },
                    {
                        value: NOTIFICATION_TYPE.TASK,
                        label: NOTIFICATION_TYPE.TASK,
                    },
                    {
                        value: NOTIFICATION_TYPE.MARKETING,
                        label: NOTIFICATION_TYPE.MARKETING,
                    },
                    {
                        value: NOTIFICATION_TYPE.SYSTEM,
                        label: NOTIFICATION_TYPE.SYSTEM,
                    },
                ],
                DelegationFrom: {
                    content: '',
                    location: ''
                },
                publishFrom: {

                },
                DraftFrom: {
                    taskId: 0,
                    location: "教学楼",
                    description: "教学事故研究会",
                    type: 1,
                    createdAt: "2024-04-13 09:41:25"
                },
                taskType: [],

                showTypeColumn: false,
                tableData: [],
                options: [
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
                notice: {
                    id: 1,
                    title: "个人信息通知",
                    description: "个人信息通知",
                    date: "2023-04-01",
                    show: false
                },
                innerVisible: false
            }
        },
        methods: {
            handleClick(tab, event) {
                // console.log(tab, event);
                this.tableData = [];
                switch (tab.label) {
                    case NOTIFICATION_TYPE.OWN:
                        this.getPersonalInformationNotification();
                        break;
                    case NOTIFICATION_TYPE.TASK:
                        this.getTaskInformationNotification();
                        break;
                    case NOTIFICATION_TYPE.MARKETING:
                        this.getMarketInformationNotification();
                        break;
                    case NOTIFICATION_TYPE.SYSTEM:
                        this.getSystemInformationNotification();
                        break;
                }
            },
            getPersonalInformationNotification() {

                getPersonalNoticeList("OWN").then((data) => {
                    console.log(data);
                    if (data.data.code == 1) {
                        this.tableData = data.data.data;
                    } else {
                        this.$message({
                            message: data.data.msg,
                            type: 'error'
                        });
                    }
                })
            },
            getTaskInformationNotification() {

                getPersonalNoticeList("USER").then((data) => {
                    console.log(data);
                    if (data.data.code == 1) {
                        this.tableData = data.data.data;
                    } else {
                        this.$message({
                            message: data.data.msg,
                            type: 'error'
                        });
                    }
                })
            },
            getMarketInformationNotification() {

                getPersonalNoticeList("MARKETING").then((data) => {
                    console.log(data);
                    if (data.data.code == 1) {
                        this.tableData = data.data.data;
                    } else {
                        this.$message({
                            message: data.data.msg,
                            type: 'error'
                        });
                    }
                })
            },
            getSystemInformationNotification() {

                getPersonalNoticeList("SYSTEM").then((data) => {
                    console.log(data);
                    if (data.data.code == 1) {
                        this.tableData = data.data.data;
                    } else {
                        this.$message({
                            message: data.data.msg,
                            type: 'error'
                        });
                    }
                })
            },
            handleView(id) {
                getNoticeById(id).then((data) => {
                    console.log(data);
                    if (data.data.code == 1) {
                        this.notice = data.data.data;
                        this.innerVisible = true;
                    } else {
                        this.$message({
                            message: data.data.msg,
                            type: 'error'
                        });
                    }
                })
            },




            cancel(form) {
                this.resetForm(form);
            },

        },
        mounted() {

            this.getPersonalInformationNotification(this.userId);

        }
    }
</script>
<style lang="less" scoped>
    .notice-center {
        padding: 4px;

        .notice-table-card {
            border-radius: 10px;

            /deep/ .el-table__body-wrapper {
                max-height: 380px;
                overflow-y: auto;
            }
        }

        .notice-detail {
            padding: 4px 8px;

            .notice-title {
                margin: 0 0 10px;
                font-size: 20px;
                font-weight: 600;
                color: var(--ce-text);
            }

            .notice-meta {
                font-size: 13px;
                color: var(--ce-text-2);
                padding-bottom: 14px;
                border-bottom: 1px solid var(--ce-border);
                margin-bottom: 16px;
            }

            .notice-content {
                font-size: 14px;
                line-height: 1.8;
                color: var(--ce-text);
                white-space: pre-wrap;
                word-break: break-word;
            }
        }
    }
</style>
