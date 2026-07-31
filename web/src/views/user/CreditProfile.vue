<template>
    <div class="credit-page">
        <el-card shadow="hover">
            <div slot="header">
                <span><i class="el-icon-medal"></i> 我的信用档案</span>
            </div>
            <div v-loading="loading">
                <el-row :gutter="20" v-if="profile" class="credit-hero-row">
                    <el-col :span="24">
                        <el-card shadow="never" class="credit-hero">
                            <div class="hero-score">{{ displayScore }}</div>
                            <div class="hero-label">信用分 · {{ displayLevel }}</div>
                        </el-card>
                    </el-col>
                </el-row>

                <el-row :gutter="20" v-if="profile">
                    <el-col :span="6">
                        <el-card shadow="never" class="stat-card">
                            <div class="stat-value">{{ profile.acceptTotal }}</div>
                            <div class="stat-label">接单总数</div>
                        </el-card>
                    </el-col>
                    <el-col :span="6">
                        <el-card shadow="never" class="stat-card">
                            <div class="stat-value">{{ profile.completedTotal }}</div>
                            <div class="stat-label">已完成接单</div>
                        </el-card>
                    </el-col>
                    <el-col :span="6">
                        <el-card shadow="never" class="stat-card">
                            <div class="stat-value">{{ profile.ratingAvg == null ? '-' : profile.ratingAvg.toFixed(1) }}</div>
                            <div class="stat-label">平均评分</div>
                        </el-card>
                    </el-col>
                    <el-col :span="6">
                        <el-card shadow="never" class="stat-card">
                            <div class="stat-value">{{ profile.goodRate }}%</div>
                            <div class="stat-label">好评率（评分≥4）</div>
                        </el-card>
                    </el-col>
                </el-row>

                <el-divider content-position="left">历史评价（{{ profile ? profile.reviewCount : 0 }} 条）</el-divider>
                <el-empty v-if="!profile || profile.reviewList.length === 0" description="暂无评价记录"></el-empty>
                <div v-for="item in profile.reviewList" :key="item.reviewId" class="review-item">
                    <div class="review-head">
                        <span class="reviewer">{{ item.reviewerName || '用户' + item.reviewerId }}</span>
                        <el-rate :value="Number(item.rating)" disabled show-score score-template="{value} 星" style="display:inline-block; margin-left:10px;"></el-rate>
                        <el-tag size="mini" type="info" style="float: right;">任务 #{{ item.taskId }}</el-tag>
                    </div>
                    <div class="review-task" v-if="item.taskDescription">{{ item.taskDescription }}</div>
                    <div class="review-comment">{{ item.comment }}</div>
                </div>
            </div>
        </el-card>
    </div>
</template>
<script>
    import { getCreditProfile } from '@/api/'
    export default {
        name: 'CreditProfile',
        data() {
            return {
                profile: null,
                loading: false
            }
        },
        computed: {
            displayScore() {
                return this.profile && this.profile.creditScore != null ? this.profile.creditScore : 60
            },
            displayLevel() {
                const s = this.displayScore
                if (s < 60) return '待提升'
                if (s < 80) return '良好'
                return '优秀'
            }
        },
        created() {
            this.getData()
        },
        methods: {
            getData() {
                this.loading = true
                getCreditProfile().then(res => {
                    if (res.data.code === 1) {
                        this.profile = res.data.data
                    } else {
                        this.$message.error(res.data.msg || '获取信用档案失败')
                    }
                    this.loading = false
                })
            }
        }
    }
</script>
<style scoped>
    .credit-page {
        padding: 10px;
    }

    .stat-card {
        text-align: center;
        padding: 10px 0;
    }

    .stat-value {
        font-size: 30px;
        font-weight: 700;
        color: #409EFF;
    }

    .stat-label {
        color: #909399;
        font-size: 13px;
        margin-top: 6px;
    }

    .credit-hero-row {
        margin-bottom: 10px;
    }

    .credit-hero {
        text-align: center;
        background: linear-gradient(135deg, #409EFF, #66b1ff);
        color: #fff;
        border: none;
    }

    .hero-score {
        font-size: 44px;
        font-weight: 700;
    }

    .hero-label {
        color: rgba(255, 255, 255, 0.9);
        font-size: 14px;
        margin-top: 4px;
    }

    .review-item {
        border: 1px solid #ebeef5;
        border-radius: 6px;
        padding: 12px;
        margin-bottom: 10px;
    }

    .review-head {
        display: flex;
        align-items: center;
    }

    .reviewer {
        font-weight: 600;
        color: #303133;
    }

    .review-task {
        color: #909399;
        font-size: 13px;
        margin-top: 6px;
    }

    .review-comment {
        color: #303133;
        margin-top: 4px;
    }
</style>
