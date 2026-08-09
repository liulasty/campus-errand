<template>
    <div class="dashboard-container" v-loading="loading">
        <!-- Hero: 核心指标 + 公告单行滚动 + 快捷入口（按身份） -->
        <div class="dashboard-hero">
            <div class="dashboard-hero-seg dashboard-hero-metrics">
                <div v-for="item in metricCards" :key="item.name" class="dashboard-hero-metric">
                    <p class="dashboard-hero-metric-value">{{ item.value }}</p>
                    <p class="dashboard-hero-metric-name">{{ item.name }}</p>
                </div>
            </div>
            <div class="dashboard-hero-divider"></div>
            <div class="dashboard-hero-seg dashboard-hero-announcement">
                <i class="el-icon-bell dashboard-hero-announcement-icon"></i>
                <div class="dashboard-hero-marquee">
                    <span class="dashboard-hero-marquee-text" :class="{ 'is-scrolling': announcements.length }">{{ announcementMarquee }}</span>
                </div>
            </div>
            <div class="dashboard-hero-divider"></div>
            <div class="dashboard-hero-seg dashboard-hero-actions">
                <div v-for="action in quickActions" :key="action.path" class="dashboard-hero-action" @click="$router.push(action.path)">
                    <i :class="`el-icon-${action.icon}`" :style="{ background: action.color }"></i>
                    <span>{{ action.label }}</span>
                </div>
            </div>
        </div>

        <!-- Row1: 最新委托 | 热门柱状图 -->
        <el-row :gutter="20" class="dashboard-row">
            <el-col :xs="24" :md="12">
                <div class="dashboard-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-tickets"></i> 最新委托</span>
                        <el-button type="text" @click="$router.push(taskMorePath)">更多</el-button>
                    </div>
                    <el-table v-if="latestTasks.length" :data="latestTasks" size="small" :show-header="true">
                        <el-table-column prop="type" label="类型" width="90">
                            <template slot-scope="scope">
                                <el-tag size="mini" type="info">{{ scope.row.type }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="description" label="内容" show-overflow-tooltip />
                        <el-table-column prop="startTime" label="发布时间" width="150">
                            <template slot-scope="scope">
                                <span class="dashboard-time">{{ scope.row.startTime | dateTime }}</span>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-else description="暂无最新委托" />
                </div>
            </el-col>
            <el-col :xs="24" :md="12">
                <div class="dashboard-card dashboard-chart-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-s-data"></i> 热门委托统计</span>
                    </div>
                    <el-empty v-if="hotBarEmpty" description="暂无热门委托数据" />
                    <div v-else ref="chartHotBar" class="dashboard-chart dashboard-chart-bar"></div>
                </div>
            </el-col>
        </el-row>

        <!-- Row2: 新增趋势 | 状态占比 | 接单排行 -->
        <el-row :gutter="20" class="dashboard-row">
            <el-col :xs="24" :md="8">
                <div class="dashboard-card dashboard-chart-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-data-line"></i> 新增委托趋势</span>
                    </div>
                    <div ref="chartTrend" class="dashboard-chart dashboard-chart-line"></div>
                </div>
            </el-col>
            <el-col :xs="24" :md="8">
                <div class="dashboard-card dashboard-chart-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-pie-chart"></i> 委托状态占比</span>
                    </div>
                    <el-empty v-if="pieEmpty" description="暂无状态数据" />
                    <div v-else ref="chartPie" class="dashboard-chart dashboard-chart-pie"></div>
                </div>
            </el-col>
            <el-col :xs="24" :md="8">
                <div class="dashboard-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-trophy"></i> 接单达人排行（Top 5）</span>
                    </div>
                    <el-table v-if="ranking.length" :data="ranking" size="small" v-loading="loadingStats">
                        <el-table-column type="index" label="#" width="60" align="center" />
                        <el-table-column prop="name" label="用户" />
                        <el-table-column prop="value" label="接单数" align="center">
                            <template slot-scope="scope">
                                <span :style="{ color: medalColor(scope.$index), fontWeight: scope.$index < 3 ? 600 : 400 }">{{ scope.row.value }}</span>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-else description="暂无排行数据" />
                </div>
            </el-col>
        </el-row>

        <!-- Row3: 与我相关 | 接单记录 -->
        <el-row :gutter="20" class="dashboard-row">
            <el-col :xs="24" :md="12">
                <div class="dashboard-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-user"></i> 与我相关的委托</span>
                        <el-button type="text" @click="$router.push(taskMorePath)">更多</el-button>
                    </div>
                    <el-table v-if="relatedTasks.length" :data="relatedTasks" size="small">
                        <el-table-column prop="description" label="内容" show-overflow-tooltip />
                        <el-table-column prop="status" label="状态" width="110">
                            <template slot-scope="scope">
                                <el-tag size="mini" type="info">{{ scope.row.status }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="startTime" label="发布时间" width="150">
                            <template slot-scope="scope">
                                <span class="dashboard-time">{{ scope.row.startTime | dateTime }}</span>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-else description="暂无相关委托" />
                </div>
            </el-col>
            <el-col :xs="24" :md="12">
                <div class="dashboard-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-document"></i> 我的接单记录</span>
                    </div>
                    <el-table v-if="acceptRecords.length" :data="acceptRecords" size="small">
                        <el-table-column prop="taskId" label="任务ID" width="80" />
                        <el-table-column prop="status" label="接单状态" width="100">
                            <template slot-scope="scope">
                                <el-tag size="mini" type="info">{{ scope.row.status }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="acceptTime" label="接单时间" width="150">
                            <template slot-scope="scope">
                                <span class="dashboard-time">{{ scope.row.acceptTime | dateTime }}</span>
                            </template>
                        </el-table-column>
                        <el-table-column prop="str" label="留言" show-overflow-tooltip />
                    </el-table>
                    <el-empty v-else description="暂无接单记录" />
                </div>
            </el-col>
        </el-row>
    </div>
</template>

<script>
    import * as echarts from 'echarts';
    import { getData, getDashboardStats } from '@/api';
    import { SUCCESS_CODE } from '@/constants/http';

    const USER_ACTIONS = [
        { icon: 'edit-outline', label: '发布委托', path: '/createDelegation', color: '#409EFF' },
        { icon: 'search', label: '委托大厅', path: '/viewOnGoingList', color: '#67C23A' },
        { icon: 'document-checked', label: '我的接单', path: '/myDelegationAcceptList', color: '#E6A23C' }
    ];
    const ADMIN_ACTIONS = [
        { icon: 's-check', label: '委托审核', path: '/auditList', color: '#409EFF' },
        { icon: 'tickets', label: '全部委托', path: '/publishedList', color: '#67C23A' },
        { icon: 'user', label: '用户管理', path: '/userList', color: '#E6A23C' }
    ];

    export default {
        name: 'DashboardHome',
        data() {
            return {
                loading: true,
                loadingStats: false,
                userType: '',
                quickActions: [],
                announcements: [],
                latestTasks: [],
                hotCategories: {},
                stats: null,
                relatedTasks: [],
                acceptRecords: [],
                metricCards: [
                    { name: '今日已接受', value: 0 },
                    { name: '本周已接受', value: 0 },
                    { name: '本月已接受', value: 0 },
                    { name: '今日已发布', value: 0 },
                    { name: '本周已发布', value: 0 },
                    { name: '本月已发布', value: 0 }
                ],
                ranking: []
            };
        },
        computed: {
            hotBarEmpty() {
                return Object.keys(this.hotCategories).length === 0;
            },
            pieEmpty() {
                const counts = (this.stats && this.stats.statusCounts) || [];
                return counts.filter(item => Number(item.value) > 0).length === 0;
            },
            taskMorePath() {
                return this.userType === 'ADMIN' ? '/publishedList' : '/viewOnGoingList';
            },
            announcementMarquee() {
                if (!this.announcements || !this.announcements.length) return '暂无公告';
                const latest = this.announcements[0];
                return (latest.title ? latest.title + '：' : '') + (latest.content || '');
            }
        },
        created() {
            // 非响应式实例字段：echarts 实例池，避免 Vue 代理 echarts 对象
            this.chartPool = {};
        },
        mounted() {
            this.initUserType();
            this.loadData();
            window.addEventListener('resize', this.handleResize);
        },
        beforeDestroy() {
            this.disposeCharts();
            window.removeEventListener('resize', this.handleResize);
        },
        deactivated() {
            // keep-alive 下 beforeDestroy 不触发，失活时销毁图表实例防泄漏
            this.disposeCharts();
        },
        methods: {
            initUserType() {
                let userType = '';
                let userId = '';
                try {
                    const taskUser = JSON.parse(localStorage.getItem('TaskUser') || '{}');
                    userType = taskUser.userType || '';
                    userId = taskUser.userId || '';
                } catch (e) {
                    userType = '';
                }
                this.userType = userType;
                this.currentUserId = userId;
                this.quickActions = userType === 'ADMIN' ? ADMIN_ACTIONS : USER_ACTIONS;
            },
            async loadData() {
                this.loading = true;
                this.loadingStats = true;
                const results = await Promise.allSettled([getData(this.currentUserId), getDashboardStats()]);
                const [quick, stats] = results;
                if (quick.status === 'fulfilled' && quick.value && quick.value.data) {
                    this.applyQuickData(quick.value.data);
                } else {
                    console.warn('首页快速信息加载失败', quick.status === 'rejected' ? quick.reason : '');
                }
                if (stats.status === 'fulfilled' && stats.value && stats.value.data) {
                    this.applyStatsData(stats.value.data);
                } else {
                    console.warn('首页驾驶舱统计加载失败', stats.status === 'rejected' ? stats.reason : '');
                }
                this.loading = false;
                this.loadingStats = false;
            },
            applyQuickData(payload) {
                const { code, data } = payload;
                if (code !== SUCCESS_CODE || !data) return;
                this.announcements = data.systemAnnouncements || [];
                // 驾驶舱 widget 只展示最新 5 条，全量列表由「更多」跳转全列表页
                this.latestTasks = (data.newestTask || []).slice(0, 5);
                this.hotCategories = data.hotTaskCategory || {};
                // 个人相关卡片：与本人关联的委托 / 本人接单记录（前 5 条）
                this.relatedTasks = (data.tasksWithUser || []).slice(0, 5);
                this.acceptRecords = (data.taskAcceptRecordsWithUser || []).slice(0, 5);
                const transactionStats = data.transactionStats || {};
                this.metricCards.forEach(card => {
                    if (transactionStats[card.name] !== undefined) {
                        card.value = transactionStats[card.name];
                    }
                });
                // 数据就绪后 DOM 可能尚未更新，须在 $nextTick 中初始化图表并校验 ref
                this.$nextTick(() => {
                    this.renderHotBarChart(this.hotCategories);
                    this.renderTrendLineChart(transactionStats);
                });
            },
            applyStatsData(payload) {
                const { code, data } = payload;
                if (code !== SUCCESS_CODE || !data) return;
                this.stats = data;
                this.ranking = data.acceptRanking || [];
                this.$nextTick(() => {
                    this.renderStatusPieChart(data.statusCounts || []);
                });
            },
            getChart(refName) {
                const el = this.$refs[refName];
                if (!el) return null;
                if (!this.chartPool[refName]) {
                    this.chartPool[refName] = echarts.init(el);
                }
                return this.chartPool[refName];
            },
            disposeCharts() {
                Object.keys(this.chartPool).forEach(key => this.chartPool[key].dispose());
                this.chartPool = {};
            },
            handleResize() {
                Object.keys(this.chartPool).forEach(key => this.chartPool[key].resize());
            },
            renderHotBarChart(categoryMap) {
                const chart = this.getChart('chartHotBar');
                if (!chart) return;
                const names = Object.keys(categoryMap || {});
                const values = names.map(key => (categoryMap[key] && categoryMap[key].typeCount) || 0);
                chart.setOption({
                    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
                    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
                    xAxis: { type: 'value', boundaryGap: [0, 0.01] },
                    yAxis: { type: 'category', data: names },
                    series: [{
                        name: '委托统计',
                        type: 'bar',
                        data: values,
                        barMaxWidth: 18,
                        itemStyle: { color: '#409EFF', borderRadius: [0, 4, 4, 0] }
                    }]
                });
            },
            renderTrendLineChart(transactionStats) {
                const chart = this.getChart('chartTrend');
                if (!chart) return;
                const xAxis = ['今日', '本周', '本月'];
                const accepted = xAxis.map(k => transactionStats[k + '已接受'] || 0);
                const published = xAxis.map(k => transactionStats[k + '已发布'] || 0);
                chart.setOption({
                    tooltip: { trigger: 'axis' },
                    legend: { data: ['已接受', '已发布'], bottom: 0 },
                    grid: { top: '12%', left: '3%', right: '4%', bottom: '14%', containLabel: true },
                    xAxis: { type: 'category', boundaryGap: false, data: xAxis },
                    yAxis: { type: 'value' },
                    series: [
                        {
                            name: '已接受',
                            type: 'line',
                            smooth: true,
                            data: accepted,
                            itemStyle: { color: '#2ec7c9' },
                            areaStyle: { opacity: 0.12 }
                        },
                        {
                            name: '已发布',
                            type: 'line',
                            smooth: true,
                            data: published,
                            itemStyle: { color: '#409EFF' },
                            areaStyle: { opacity: 0.12 }
                        }
                    ]
                });
            },
            renderStatusPieChart(statusCounts) {
                const chart = this.getChart('chartPie');
                if (!chart) return;
                const pieData = (statusCounts || [])
                    .filter(item => Number(item.value) > 0)
                    .map(item => ({ name: item.name, value: Number(item.value) }));
                if (!pieData.length) {
                    chart.clear();
                    return;
                }
                chart.setOption({
                    tooltip: { trigger: 'item' },
                    legend: { bottom: 0 },
                    series: [{
                        name: '委托状态',
                        type: 'pie',
                        radius: ['40%', '70%'],
                        center: ['50%', '45%'],
                        data: pieData,
                        label: { show: false }
                    }]
                });
            },
            medalColor(index) {
                if (index === 0) return '#E6A23C';
                if (index === 1) return '#909399';
                if (index === 2) return '#cf8c4e';
                return '#303133';
            }
        }
    };
</script>

<style lang="less" scoped>
    .dashboard-container {
        min-height: 100%;

        .dashboard-row {
            margin-top: 12px;
        }

        .dashboard-card {
            background: #fff;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
            padding: 12px 14px;
            margin-bottom: 12px;
            transition: box-shadow 0.3s;

            &:hover {
                box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
            }
        }

        .dashboard-card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 10px;
        }

        .dashboard-card-title {
            font-size: 16px;
            font-weight: 600;
            color: #303133;
            display: flex;
            align-items: center;

            i {
                margin-right: 6px;
                color: #409EFF;
            }
        }

        // Hero：指标 + 公告 + 快捷入口
        .dashboard-hero {
            display: flex;
            align-items: center;
            background: #fff;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
            padding: 14px 18px;
            margin-bottom: 12px;

            .dashboard-hero-seg {
                display: flex;
                align-items: center;
            }

            .dashboard-hero-metrics {
                flex: 1.7;
                flex-wrap: wrap;
                gap: 4px 6px;

                .dashboard-hero-metric {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    padding: 0 8px;

                    .dashboard-hero-metric-value {
                        font-size: 18px;
                        font-weight: 700;
                        color: #303133;
                        line-height: 22px;
                        margin: 0;
                    }

                    .dashboard-hero-metric-name {
                        font-size: 11px;
                        color: #909399;
                        margin: 2px 0 0;
                        white-space: nowrap;
                    }
                }
            }

            .dashboard-hero-divider {
                width: 1px;
                height: 38px;
                background: #ebeef5;
                margin: 0 16px;
                flex-shrink: 0;
            }

            .dashboard-hero-announcement {
                flex: 1;
                min-width: 0;

                .dashboard-hero-announcement-icon {
                    margin-right: 6px;
                    color: #409EFF;
                    flex-shrink: 0;
                }

                .dashboard-hero-marquee {
                    overflow: hidden;
                    white-space: nowrap;
                    flex: 1;
                    min-width: 0;

                    .dashboard-hero-marquee-text {
                        display: inline-block;
                        font-size: 13px;
                        color: #606266;

                        &.is-scrolling {
                            padding-left: 100%;
                            animation: dashboard-marquee 20s linear infinite;
                        }
                    }

                    &:hover .dashboard-hero-marquee-text.is-scrolling {
                        animation-play-state: paused;
                    }
                }
            }

            .dashboard-hero-actions {
                flex-shrink: 0;
                gap: 8px;

                .dashboard-hero-action {
                    display: flex;
                    align-items: center;
                    padding: 6px 10px;
                    border: 1px solid #ebeef5;
                    border-radius: 6px;
                    cursor: pointer;
                    font-size: 13px;
                    color: #303133;
                    transition: all 0.2s;

                    i {
                        width: 22px;
                        height: 22px;
                        border-radius: 6px;
                        color: #fff;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 12px;
                        margin-right: 5px;
                        flex-shrink: 0;
                    }

                    &:hover {
                        border-color: #409EFF;
                        color: #409EFF;
                    }
                }
            }
        }

        @keyframes dashboard-marquee {
            0% {
                transform: translateX(0);
            }
            100% {
                transform: translateX(-100%);
            }
        }

        @media (max-width: 991px) {
            .dashboard-hero {
                flex-wrap: wrap;
                gap: 10px;

                .dashboard-hero-divider {
                    display: none;
                }

                .dashboard-hero-actions {
                    width: 100%;
                    justify-content: center;
                }
            }
        }

        .dashboard-time {
            font-size: 12px;
            color: #909399;
        }

        // 图表
        .dashboard-chart-bar {
            height: 240px;
            width: 100%;
        }

        .dashboard-chart-line,
        .dashboard-chart-pie {
            height: 210px;
            width: 100%;
        }

        @media (max-width: 991px) {
            .dashboard-chart-bar,
            .dashboard-chart-line,
            .dashboard-chart-pie {
                height: 180px;
            }
        }

    }
</style>
