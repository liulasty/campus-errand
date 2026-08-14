<template>
    <div class="notice-center">
        <el-tabs v-model="activeType" @tab-click="handleClick">
            <el-tab-pane v-for="notice in noticeType" :key="notice.value" :label="notice.label"
                :name="notice.value">
                <div v-loading="loading" class="notice-list">
                    <div v-for="item in tableData" :key="item.id" class="notice-card"
                        :class="{ 'is-unread': item.isRead === false }" @click="handleView(item.id)">
                        <div class="notice-card-bar"></div>
                        <div class="notice-card-body">
                            <div class="notice-card-top">
                                <span class="notice-card-title">
                                    <i v-if="item.isRead === false" class="notice-unread-dot"></i>
                                    {{ item.title }}
                                </span>
                                <span class="notice-card-time">{{ item.date | dateTime }}</span>
                            </div>
                            <div class="notice-card-desc">{{ item.description }}</div>
                        </div>
                    </div>
                    <el-empty v-if="!tableData.length && !loading" description="暂无通知"></el-empty>
                </div>
                <el-pagination v-if="total > pageSize" class="notice-pagination"
                    @size-change="handleSizeChange" @current-change="handleCurrentChange"
                    :current-page="pageNum" :page-size="pageSize" :page-sizes="[5, 10, 20]"
                    layout="total, sizes, prev, pager, next" :total="total">
                </el-pagination>
            </el-tab-pane>
        </el-tabs>

        <el-dialog width="480px" title="通知详情" :visible.sync="innerVisible" append-to-body
            @close="onDetailClose">
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


    export default {
        props: {
            userId: {
                type: Number,
                default: 0
            },
        },
        data() {
            return {
                noticeType: [
                    { value: 'OWN', label: '个人信息通知' },
                    { value: 'USER', label: '委托信息通知' },
                    { value: 'MARKETING', label: '营销信息通知' },
                    { value: 'SYSTEM', label: '系统信息通知' }
                ],
                activeType: 'OWN',
                tableData: [],
                total: 0,
                pageNum: 1,
                pageSize: 5,
                loading: false,
                notice: {
                    id: 1,
                    title: '',
                    description: '',
                    date: ''
                },
                innerVisible: false
            }
        },
        methods: {
            handleClick() {
                this.pageNum = 1;
                this.loadByType();
            },
            loadByType() {
                this.loading = true;
                getPersonalNoticeList(this.activeType, {
                    pageNum: this.pageNum,
                    pageSize: this.pageSize
                }).then((data) => {
                    if (data.data.code == 1) {
                        const res = data.data.data;
                        this.tableData = (res && res.records) || [];
                        this.total = (res && res.total) || 0;
                    } else {
                        this.$message({
                            message: data.data.msg,
                            type: 'error'
                        });
                    }
                    this.loading = false;
                }).catch(() => {
                    this.loading = false;
                });
            },
            handleSizeChange(size) {
                this.pageSize = size;
                this.pageNum = 1;
                this.loadByType();
            },
            handleCurrentChange(page) {
                this.pageNum = page;
                this.loadByType();
            },
            handleView(id) {
                getNoticeById(id).then((data) => {
                    if (data.data.code == 1) {
                        this.notice = data.data.data;
                        this.innerVisible = true;
                    } else {
                        this.$message({
                            message: data.data.msg,
                            type: 'error'
                        });
                    }
                });
            },
            onDetailClose() {
                // 查看详情后刷新当前列表，同步已读状态
                this.loadByType();
            }
        },
        mounted() {
            this.loadByType();
        }
    }
</script>
<style lang="less" scoped>
    .notice-center {
        padding: 4px;

        .notice-list {
            min-height: 220px;

            .notice-card {
                position: relative;
                display: flex;
                align-items: stretch;
                margin-bottom: 10px;
                padding: 12px 14px;
                border: 1px solid #ebeef5;
                border-radius: 8px;
                background: #fafafa;
                cursor: pointer;
                transition: box-shadow 0.2s, border-color 0.2s, background 0.2s;

                &:hover {
                    box-shadow: 0 4px 14px rgba(0, 21, 41, 0.1);
                    border-color: #c0c4cc;
                    background: #fff;
                }

                &.is-unread {
                    background: #fff;
                    border-color: #c6e2ff;

                    .notice-card-bar {
                        background: #409eff;
                    }
                }

                .notice-card-bar {
                    width: 3px;
                    border-radius: 2px;
                    background: transparent;
                    margin-right: 12px;
                    flex-shrink: 0;
                }

                .notice-card-body {
                    flex: 1;
                    min-width: 0;
                }

                .notice-card-top {
                    display: flex;
                    justify-content: space-between;
                    align-items: baseline;
                    gap: 8px;
                }

                .notice-card-title {
                    font-size: 14px;
                    font-weight: 600;
                    color: #303133;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;

                    .notice-unread-dot {
                        display: inline-block;
                        width: 7px;
                        height: 7px;
                        margin-right: 6px;
                        border-radius: 50%;
                        background: #f56c6c;
                        vertical-align: middle;
                    }
                }

                .notice-card-time {
                    flex-shrink: 0;
                    font-size: 12px;
                    color: #909399;
                }

                .notice-card-desc {
                    margin-top: 4px;
                    font-size: 12px;
                    color: #909399;
                    line-height: 1.5;
                    display: -webkit-box;
                    -webkit-box-orient: vertical;
                    -webkit-line-clamp: 2;
                    overflow: hidden;
                }
            }
        }

        .notice-pagination {
            margin-top: 8px;
            display: flex;
            justify-content: flex-end;
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
