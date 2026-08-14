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
                <div class="dashboard-card dashboard-latest">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-tickets"></i> 最新委托</span>
                        <el-button type="text" class="dashboard-more" @click="$router.push(taskMorePath)">更多</el-button>
                    </div>
                    <div v-if="latestTasks.length" class="dl-latest">
                        <div v-for="(task, index) in latestTasks" :key="task.taskId" class="dl-latest__item"
                            :style="{ animationDelay: (index * 55) + 'ms' }"
                            @click="$router.push(taskMorePath)">
                            <span class="dl-latest__badge">{{ typeName(task.taskType) }}</span>
                            <div class="dl-latest__body">
                                <p class="dl-latest__desc">{{ task.description }}</p>
                                <div class="dl-latest__meta">
                                    <span class="dl-latest__meta-item"><i class="el-icon-location-outline"></i>{{ task.location || '—' }}</span>
                                    <span class="dl-latest__meta-item"><i class="el-icon-time"></i>{{ task.startTime | dateTime }}</span>
                                    <span class="dl-latest__status">{{ statusName(task.status) }}</span>
                                </div>
                            </div>
                            <i class="el-icon-arrow-right dl-latest__arrow"></i>
                        </div>
                    </div>
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
    import { getData, getDashboardStats, getTaskCategories } from '@/api';
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
                taskTypeMap: {},
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
            this.loadTaskCategories();
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
            // 加载委托分类：newestTask.taskType 是数字 id，映射为名称
            loadTaskCategories() {
                getTaskCategories().then(res => {
                    if (res.data.code === 1 && res.data.data && res.data.data.length) {
                        const map = {};
                        res.data.data.forEach(c => { map[c.id] = c.name; });
                        this.taskTypeMap = map;
                    }
                }).catch(err => {
                    console.error('加载委托分类失败：', err);
                });
            },
            typeName(taskType) {
                return this.taskTypeMap[taskType] || ('类型 ' + taskType);
            },
            statusName(status) {
                if (!status) return '';
                const map = {
                    '委托发布中': '发布中',
                    '已接收': '已接收',
                    '已完成': '已完成',
                    '草稿': '草稿',
                    '审核中': '审核中',
                    '审核未通过': '未通过',
                    '等待发布': '待发布',
                    '已过期': '已过期',
                    '已取消': '已取消'
                };
                return map[status] || status;
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
                        itemStyle: {
                            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                                { offset: 0, color: '#5f8a6f' },
                                { offset: 1, color: '#93b09a' }
                            ]),
                            borderRadius: [0, 5, 5, 0]
                        }
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
                            itemStyle: { color: '#5f8a6f' },
                            areaStyle: { color: 'rgba(95, 138, 111, .14)' }
                        },
                        {
                            name: '已发布',
                            type: 'line',
                            smooth: true,
                            data: published,
                            itemStyle: { color: '#b9892c' },
                            areaStyle: { color: 'rgba(185, 137, 44, .12)' }
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
                    color: ['#5f8a6f', '#b9892c', '#3e6b50', '#c07b32', '#d8ccb2', '#9aa198'],
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
        --paper: #f5f1e8;
        --card: #fffdf8;
        --ink: #2a3a30;
        --ink-soft: #5f6b62;
        --muted: #9aa198;
        --line: #e6ddc9;
        --line-strong: #d8ccb2;
        --brass: #b9892c;
        --sage: #5f8a6f;

        min-height: calc(100vh - 160px);
        padding: 8px 6px 28px;
        border-radius: 14px;
        background:
            radial-gradient(900px 420px at 100% -10%, rgba(185, 137, 44, .08), transparent 55%),
            radial-gradient(800px 400px at -5% 10%, rgba(95, 138, 111, .08), transparent 55%),
            var(--paper);

        .dashboard-row {
            margin-top: 14px;
        }

        .dashboard-card {
            background: var(--card);
            border: 1px solid #eee7d5;
            border-radius: 14px;
            box-shadow: 0 1px 2px rgba(42, 58, 48, .04), 0 12px 28px -18px rgba(42, 58, 48, .22);
            padding: 14px 16px;
            margin-bottom: 12px;
            transition: box-shadow .3s, border-color .3s, transform .3s;

            &:hover {
                border-color: var(--line-strong);
                box-shadow: 0 2px 4px rgba(42, 58, 48, .05), 0 18px 36px -18px rgba(42, 58, 48, .28);
                transform: translateY(-1px);
            }
        }

        .dashboard-card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 12px;
        }

        .dashboard-card-title {
            font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
            font-size: 16px;
            font-weight: 700;
            letter-spacing: .02em;
            color: var(--ink);
            display: flex;
            align-items: center;

            i {
                margin-right: 7px;
                color: var(--brass);
            }
        }

        // 卡片内的「更多」文本按钮
        .dashboard-card :deep(.el-button--text) {
            color: var(--muted);
            font-size: 12px;
            letter-spacing: .04em;

            &:hover {
                color: var(--brass-deep);
            }
        }

        // 最新委托：档案风卡片列表
        .dashboard-latest {
            background:
                radial-gradient(420px 200px at 92% -10%, rgba(185, 137, 44, .12), transparent 60%),
                linear-gradient(160deg, #fbf7ee 0%, #f1ead9 100%);
            border: 1px solid #e6ddc9;
            border-radius: 12px;
            box-shadow: 0 6px 18px -12px rgba(42, 58, 48, .3);

            .dashboard-card-title {
                font-family: Georgia, "Noto Serif SC", "Source Han Serif SC", "Songti SC", SimSun, serif;
                letter-spacing: .03em;
                color: #2a3a30;

                i {
                    color: #b9892c;
                }
            }

            .dashboard-more {
                color: #96701f;
                font-family: inherit;
                letter-spacing: .05em;

                &:hover {
                    color: #6d5213;
                }
            }
        }

        .dl-latest {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .dl-latest__item {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 10px 12px;
            background: rgba(255, 252, 245, .82);
            border: 1px solid #ece3d1;
            border-left: 3px solid #d8ccb2;
            border-radius: 9px;
            cursor: pointer;
            transition: transform .2s, box-shadow .2s, border-color .2s;
            animation: dlLatestIn .42s ease both;

            &:hover {
                transform: translateX(3px);
                border-left-color: #b9892c;
                box-shadow: 0 6px 16px -10px rgba(42, 58, 48, .28);
            }
        }

        .dl-latest__badge {
            flex-shrink: 0;
            font-size: 11px;
            letter-spacing: .06em;
            padding: 3px 10px;
            border-radius: 999px;
            background: #dce8e0;
            color: #3e6b50;
            white-space: nowrap;
        }

        .dl-latest__body {
            flex: 1;
            min-width: 0;
        }

        .dl-latest__desc {
            margin: 0;
            font-size: 13px;
            font-weight: 600;
            line-height: 1.5;
            color: #2a3a30;
            display: -webkit-box;
            -webkit-line-clamp: 1;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }

        .dl-latest__meta {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 4px 12px;
            margin-top: 5px;
            font-size: 11px;
            color: #9aa198;
        }

        .dl-latest__meta-item {
            display: inline-flex;
            align-items: center;

            i {
                margin-right: 3px;
                color: #c0b79f;
            }
        }

        .dl-latest__status {
            color: #5f8a6f;
            letter-spacing: .05em;
        }

        .dl-latest__arrow {
            color: #d8ccb2;
            flex-shrink: 0;
            transition: color .2s, transform .2s;
        }

        .dl-latest__item:hover .dl-latest__arrow {
            color: #b9892c;
            transform: translateX(2px);
        }

        @keyframes dlLatestIn {
            from {
                opacity: 0;
                transform: translateY(10px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        // Hero：指标 + 公告 + 快捷入口（深绿渐变横幅）
        .dashboard-hero {
            position: relative;
            overflow: hidden;
            display: flex;
            align-items: center;
            background: linear-gradient(135deg, #2a3a30 0%, #405e4b 100%);
            border-radius: 16px;
            box-shadow: 0 14px 32px -16px rgba(42, 58, 48, .5);
            padding: 22px 26px;
            margin-bottom: 14px;
            color: #fff;

            &::before {
                content: "";
                position: absolute;
                inset: 0;
                pointer-events: none;
                background:
                    radial-gradient(340px 180px at 88% -30%, rgba(255, 255, 255, .14), transparent 60%),
                    radial-gradient(260px 180px at -4% 120%, rgba(185, 137, 44, .32), transparent 60%);
            }

            .dashboard-hero-seg {
                position: relative;
                display: flex;
                align-items: center;
            }

            .dashboard-hero-metrics {
                flex: 1.7;
                flex-wrap: wrap;
                gap: 6px 8px;

                .dashboard-hero-metric {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    padding: 0 10px;

                    .dashboard-hero-metric-value {
                        font-family: Georgia, "Times New Roman", serif;
                        font-size: 24px;
                        font-weight: 700;
                        color: #fff;
                        line-height: 26px;
                        margin: 0;
                        font-variant-numeric: tabular-nums;
                    }

                    .dashboard-hero-metric-name {
                        font-size: 11px;
                        color: rgba(255, 255, 255, .62);
                        letter-spacing: .08em;
                        margin: 3px 0 0;
                        white-space: nowrap;
                    }
                }
            }

            .dashboard-hero-divider {
                width: 1px;
                height: 42px;
                background: rgba(255, 255, 255, .18);
                margin: 0 18px;
                flex-shrink: 0;
            }

            .dashboard-hero-announcement {
                flex: 1;
                min-width: 0;

                .dashboard-hero-announcement-icon {
                    margin-right: 8px;
                    color: #f0c879;
                    flex-shrink: 0;
                    font-size: 16px;
                }

                .dashboard-hero-marquee {
                    overflow: hidden;
                    white-space: nowrap;
                    flex: 1;
                    min-width: 0;

                    .dashboard-hero-marquee-text {
                        display: inline-block;
                        font-size: 13px;
                        letter-spacing: .03em;
                        color: rgba(255, 255, 255, .85);

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
                gap: 10px;

                .dashboard-hero-action {
                    display: flex;
                    align-items: center;
                    padding: 8px 12px;
                    background: rgba(255, 255, 255, .1);
                    border: 1px solid rgba(255, 255, 255, .18);
                    border-radius: 10px;
                    cursor: pointer;
                    font-size: 13px;
                    letter-spacing: .03em;
                    color: #fff;
                    -webkit-backdrop-filter: blur(4px);
                    backdrop-filter: blur(4px);
                    transition: background .2s, border-color .2s, transform .2s;

                    i {
                        width: 24px;
                        height: 24px;
                        border-radius: 7px;
                        color: #fff;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 12px;
                        margin-right: 6px;
                        flex-shrink: 0;
                    }

                    &:hover {
                        background: rgba(255, 255, 255, .18);
                        border-color: rgba(255, 255, 255, .4);
                        transform: translateY(-1px);
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
            color: #9aa198;
        }

        // 卡片内表格现代化
        .dashboard-card :deep(.el-table) {
            background: transparent;
            color: #2a3a30;
            font-size: 13px;
        }

        .dashboard-card :deep(.el-table::before) {
            display: none;
        }

        .dashboard-card :deep(th.el-table__cell) {
            background: transparent;
            color: #9aa198;
            font-weight: 600;
            font-size: 12px;
            letter-spacing: .05em;
            border-bottom: 1px solid #e9e2d0;
        }

        .dashboard-card :deep(td.el-table__cell) {
            border-bottom: 1px solid #f0ead9;
        }

        .dashboard-card :deep(.el-table__row:hover > td.el-table__cell) {
            background: rgba(220, 232, 224, .25);
        }

        // 卡片内标签 → 档案风徽标
        .dashboard-card :deep(.el-tag) {
            background: #efe9d7;
            border-color: #e6ddc9;
            color: #7a6a3a;
            border-radius: 999px;
            border: none;
            font-size: 11px;
            letter-spacing: .05em;
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
