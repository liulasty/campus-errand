# 首页驾驶舱重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构首页数据驾驶舱 `web/src/views/dashboard/Home.vue` —— 按业务权重重排布局、统一视觉规范、修复 echarts2 死引用等全部 JS 缺陷，实现响应式自适应；同步给全局 `dateTime` 过滤器加容错。纯前端，不动任何后端接口。

**Architecture:** 单一 Vue SFC 全量重写。布局改为「核心指标区 → 公告+最新委托|热门柱状图 → 四宫格」三层栅格（`xs/sm/md` 断点）。数据加载用 `Promise.allSettled` 并行 + 独立容错；echarts 实例统一进 `chartPool` 池，`beforeDestroy`/`deactivated` 统一 `dispose`，`resize` 监听成对清理。视觉用 `dashboard-` class 前缀 + 设计令牌（圆角/双层阴影/两套指标卡色系）。

**Tech Stack:** Vue 2.6 + Element UI 2.15 + echarts 5 + less（scoped）。无前端测试框架，验证靠 `npm run build` + eslint + 静态 grep；功能验收由 Business Validator 执行（本项目工作流约定）。

---

### Task 1: 全局 `dateTime` 过滤器容错

**Files:**
- Modify: `web/src/main.js:56-61`（`Vue.filter('dateTime', ...)`）

**背景**：现过滤器 `if (!val) return ''` 对 null 返回空串、对非法时间戳（`Number('abc')` → NaN）输出 `NaN-NaN-NaN NaN:NaN`。按 spec 风险点 #3 改为统一返回 `'—'`。

- [ ] **Step 1: 替换过滤器实现**

将 `web/src/main.js` 第 56-61 行：

```js
Vue.filter('dateTime', (val) => {
  if (!val) return ''
  const d = new Date(Number(val))
  const pad = n => (n < 10 ? '0' + n : n)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
})
```

替换为：

```js
Vue.filter('dateTime', (val) => {
  if (val === null || val === undefined || val === '') return '—'
  const num = Number(val)
  if (Number.isNaN(num)) return '—'
  const d = new Date(num)
  if (Number.isNaN(d.getTime())) return '—'
  const pad = n => (n < 10 ? '0' + n : n)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
})
```

- [ ] **Step 2: 构建验证**

在仓库根目录（含 `web/` 与 `pom.xml` 的目录，勿 `cd web` 后跑 mvn）执行：

```bash
cd web && npm run build
```

期望：构建成功，无编译错误。（`cd web` 后记得切回仓库根再跑后续命令。）

---

### Task 2: 全量重写 `Home.vue`

**Files:**
- Rewrite: `web/src/views/dashboard/Home.vue`（完整覆盖 template / script / style）

**背景**：旧文件含 mock 假数据、`generateEchart2` 死引用（新增数据图表空白）、`Brazil/Indonesia` 硬编码占位、`Promise.all` 无容错、echarts 不销毁、无 resize、无空态。以下为最终完整文件，直接整体覆盖。

- [ ] **Step 1: 用下方完整内容覆盖 `web/src/views/dashboard/Home.vue`**

```vue
<template>
    <div class="dashboard-container" v-loading="loading">
        <!-- Row1: 核心指标区 -->
        <el-row :gutter="20">
            <el-col v-for="item in metricCards" :key="item.name" :xs="24" :sm="12" :md="8">
                <div class="dashboard-metric-card">
                    <div class="dashboard-metric-icon" :style="{ background: item.color }">
                        <i :class="`el-icon-${item.icon}`"></i>
                    </div>
                    <div class="dashboard-metric-detail">
                        <p class="dashboard-metric-value">{{ item.value }}</p>
                        <p class="dashboard-metric-name">{{ item.name }}</p>
                    </div>
                </div>
            </el-col>
        </el-row>

        <!-- Row2: 公告+最新委托 | 热门委托统计 -->
        <el-row :gutter="20" class="dashboard-row">
            <el-col :xs="24" :md="10">
                <div class="dashboard-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-bell"></i> 系统公告</span>
                    </div>
                    <div class="dashboard-announcement-carousel">
                        <el-carousel v-if="announcements.length" height="280px" direction="vertical" :autoplay="true" :interval="5000">
                            <el-carousel-item v-for="item in announcements" :key="item.announcementId">
                                <div class="dashboard-announcement-content">
                                    <h3 class="dashboard-announcement-title">
                                        <el-tag v-if="item.isPinned" type="danger" size="mini" effect="dark">置顶</el-tag>
                                        {{ item.title }}
                                    </h3>
                                    <p class="dashboard-announcement-desc">{{ item.content }}</p>
                                    <div class="dashboard-announcement-meta">
                                        <span class="dashboard-time"><i class="el-icon-time"></i> {{ item.publishTime | dateTime }}</span>
                                    </div>
                                </div>
                            </el-carousel-item>
                        </el-carousel>
                        <el-empty v-else description="暂无公告" />
                    </div>
                </div>

                <div class="dashboard-card dashboard-row">
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

            <el-col :xs="24" :md="14">
                <div class="dashboard-card dashboard-chart-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-s-data"></i> 热门委托统计</span>
                    </div>
                    <el-empty v-if="hotBarEmpty" description="暂无热门委托数据" />
                    <div v-else ref="chartHotBar" class="dashboard-chart dashboard-chart-bar"></div>
                </div>
            </el-col>
        </el-row>

        <!-- Row3: 底层四宫格 -->
        <el-row :gutter="20" class="dashboard-row">
            <el-col :xs="24" :md="12">
                <div class="dashboard-card dashboard-chart-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-data-line"></i> 新增委托趋势</span>
                    </div>
                    <div ref="chartTrend" class="dashboard-chart dashboard-chart-line"></div>
                </div>
            </el-col>
            <el-col :xs="24" :md="12">
                <div class="dashboard-card dashboard-chart-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-pie-chart"></i> 委托状态占比</span>
                    </div>
                    <el-empty v-if="pieEmpty" description="暂无状态数据" />
                    <div v-else ref="chartPie" class="dashboard-chart dashboard-chart-pie"></div>
                </div>
            </el-col>
            <el-col :xs="24" :md="12">
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
            <el-col :xs="24" :md="12">
                <div class="dashboard-card">
                    <div class="dashboard-card-header">
                        <span class="dashboard-card-title"><i class="el-icon-operation"></i> 快捷操作</span>
                    </div>
                    <div class="dashboard-quick-actions">
                        <div v-for="action in quickActions" :key="action.path" class="dashboard-quick-action" @click="$router.push(action.path)">
                            <i :class="`el-icon-${action.icon}`" :style="{ background: action.color }"></i>
                            <span>{{ action.label }}</span>
                        </div>
                    </div>
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
        data() {
            return {
                loading: true,
                loadingStats: false,
                userType: '',
                quickActions: [],
                announcements: [],
                latestTasks: [],
                hotCategories: {},
                metricCards: [
                    { name: '今日已接受', value: 0, icon: 'check', color: '#2ec7c9' },
                    { name: '本周已接受', value: 0, icon: 'star-on', color: '#409EFF' },
                    { name: '本月已接受', value: 0, icon: 's-goods', color: '#67C23A' },
                    { name: '今日已发布', value: 0, icon: 'upload2', color: '#ffb980' },
                    { name: '本周已发布', value: 0, icon: 'time', color: '#E6A23C' },
                    { name: '本月已发布', value: 0, icon: 'document', color: '#F56C6C' }
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
                try {
                    const taskUser = JSON.parse(localStorage.getItem('TaskUser') || '{}');
                    userType = taskUser.userType || '';
                } catch (e) {
                    userType = '';
                }
                this.userType = userType;
                this.quickActions = userType === 'ADMIN' ? ADMIN_ACTIONS : USER_ACTIONS;
            },
            async loadData() {
                this.loading = true;
                this.loadingStats = true;
                const results = await Promise.allSettled([getData(2), getDashboardStats()]);
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
                this.latestTasks = data.newestTask || [];
                this.hotCategories = data.hotTaskCategory || {};
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
            margin-top: 20px;
        }

        .dashboard-card {
            background: #fff;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
            padding: 16px 18px;
            margin-bottom: 20px;
            transition: box-shadow 0.3s;

            &:hover {
                box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
            }
        }

        .dashboard-card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 14px;
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

        // 指标卡
        .dashboard-metric-card {
            display: flex;
            align-items: center;
            background: #fff;
            border-radius: 10px;
            padding: 20px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
            margin-bottom: 20px;
            transition: transform 0.3s, box-shadow 0.3s;

            &:hover {
                transform: translateY(-2px);
                box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);

                .dashboard-metric-value {
                    transform: scale(1.05);
                }
            }

            .dashboard-metric-icon {
                width: 60px;
                height: 60px;
                border-radius: 12px;
                display: flex;
                align-items: center;
                justify-content: center;
                color: #fff;
                font-size: 28px;
                flex-shrink: 0;
            }

            .dashboard-metric-detail {
                margin-left: 14px;

                .dashboard-metric-value {
                    font-size: 26px;
                    font-weight: 700;
                    color: #303133;
                    line-height: 32px;
                    margin: 0;
                    transition: transform 0.3s;
                }

                .dashboard-metric-name {
                    font-size: 13px;
                    color: #909399;
                    margin: 2px 0 0;
                }
            }
        }

        // 公告
        .dashboard-announcement-carousel {
            .dashboard-announcement-content {
                padding: 0 4px;

                .dashboard-announcement-title {
                    font-size: 15px;
                    color: #303133;
                    margin: 0 0 10px;
                    display: flex;
                    align-items: center;

                    .el-tag {
                        margin-right: 8px;
                    }
                }

                .dashboard-announcement-desc {
                    font-size: 13px;
                    color: #606266;
                    line-height: 1.6;
                    max-height: 220px;
                    overflow-y: auto;
                    margin: 0 0 12px;
                }

                .dashboard-announcement-meta {
                    color: #909399;
                    font-size: 12px;
                    border-top: 1px solid #ebeef5;
                    padding-top: 10px;
                }
            }
        }

        .dashboard-time {
            font-size: 12px;
            color: #909399;
        }

        // 图表
        .dashboard-chart-bar {
            height: 320px;
            width: 100%;
        }

        .dashboard-chart-line,
        .dashboard-chart-pie {
            height: 260px;
            width: 100%;
        }

        @media (max-width: 991px) {
            .dashboard-chart-bar,
            .dashboard-chart-line,
            .dashboard-chart-pie {
                height: 230px;
            }
        }

        // 快捷操作
        .dashboard-quick-actions {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;

            .dashboard-quick-action {
                flex: 1;
                min-width: 120px;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 18px 12px;
                border: 1px solid #ebeef5;
                border-radius: 8px;
                cursor: pointer;
                color: #303133;
                font-size: 14px;
                transition: all 0.3s;

                i {
                    width: 32px;
                    height: 32px;
                    border-radius: 8px;
                    color: #fff;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 16px;
                    margin-right: 8px;
                    flex-shrink: 0;
                }

                &:hover {
                    border-color: #409EFF;
                    color: #409EFF;
                    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
                    transform: translateY(-2px);
                }
            }
        }
    }
</style>
```

- [ ] **Step 2: 构建验证**

```bash
cd web && npm run build
```

期望：构建成功。重点确认无 `generateEchart` / `Brazil` / `scope.row` 模板报错。

- [ ] **Step 3: 提交**

```bash
git add web/src/views/dashboard/Home.vue
git commit -m "refactor: 首页驾驶舱重排布局、修复echarts死引用与内存泄漏、统一视觉规范"
```

---

### Task 3: eslint 校验两个改动文件

**Files:**
- Check: `web/src/views/dashboard/Home.vue`、`web/src/main.js`

- [ ] **Step 1: 对两个文件跑 eslint（不开 auto-fix）**

```bash
cd web && npx eslint --ext .js,.vue src/main.js src/views/dashboard/Home.vue
```

期望：无报错。若 Home.vue 因 `console.warn` 触发 `no-console` 类告警（eslint:recommended 不含该规则，预期无），如有则按 eslint 建议修正（仅限这两个文件；不要改动无关文件）。

- [ ] **Step 2: 提交（如有 lint 修正）**

```bash
git add web/src/views/dashboard/Home.vue web/src/main.js
git commit -m "style: 修正首页重构文件的 lint 问题"
```

---

### Task 4: 静态验证（死引用 / 假数据 / class 前缀）

**Files:**
- Inspect: `web/src/views/dashboard/Home.vue`

- [ ] **Step 1: 确认无死引用与硬编码占位**

```bash
cd web && grep -nE "generateEchart|Brazil|Indonesia|硬编码|mock" src/views/dashboard/Home.vue
```

期望：无任何输出（exit 1 = 未匹配，属正常）。

- [ ] **Step 2: 确认旧 mock 数据已清除**

```bash
cd web && grep -nE "活动名称|参赛资格|2016-05-03" src/views/dashboard/Home.vue
```

期望：无输出。

- [ ] **Step 3: 确认自定义 class 均为 `dashboard-` 前缀**

```bash
cd web && grep -oE 'class="[^"]*dashboard-[^"]*"' src/views/dashboard/Home.vue
```

期望：所有输出均含 `dashboard-`。

- [ ] **Step 4: 提交设计文档与计划（若尚未提交）**

```bash
git add docs/superpowers/specs/2026-08-09-home-dashboard-redesign-design.md docs/superpowers/plans/2026-08-09-home-dashboard-redesign.md
git commit -m "docs: 首页驾驶舱重构设计文档与实现计划"
```

---

## Self-Review

**Spec 覆盖对照**：
- 布局三层栅格（Row1 指标 / Row2 公告+最新委托|柱状图 / Row3 四宫格）→ Task 2 模板。
- 视觉规范（圆角 10px、双层阴影、hover 上浮、两套指标卡色系、`dashboard-` 前缀、间距 20/14px、字号层级）→ Task 2 style。
- 公告轮播折中方案（容器 280px + 内容区 max-height 220px 滚动）→ Task 2。
- echarts2 修复（折线图，transactionStats × [今日,本周,本月]）→ `renderTrendLineChart`。
- TOP5 奖牌色（金 #E6A23C / 银 #909399 / 铜 #cf8c4e / 默认）→ `medalColor`。
- 快捷入口按角色（USER 3 项 / ADMIN 3 项，mounted 读 localStorage.TaskUser）→ `initUserType` + `USER_ACTIONS/ADMIN_ACTIONS`。
- Promise.allSettled + 独立容错 → `loadData`。
- initChart 置于 $nextTick + ref 校验 → `applyQuickData/applyStatsData` + `getChart`。
- echarts 实例池 + beforeDestroy/deactivated dispose + resize 成对清理 → `created`/`beforeDestroy`/`deactivated`/`handleResize`/`disposeCharts`。
- v-loading 骨架 + 各模块 el-empty（公告/最新委托/柱状图/饼图/Top5）→ 模板。
- hotTaskCategory 空对象捕获 → `hotBarEmpty` computed。
- dateTime 容错 → Task 1（main.js）。

**无占位符**：每个改动均含完整代码与命令。

**类型一致性**：`chartPool`、`getChart`/`disposeCharts`/`handleResize` 命名全程一致；`renderHotBarChart`/`renderTrendLineChart`/`renderStatusPieChart` 与模板 ref（`chartHotBar`/`chartTrend`/`chartPie`）一一对应；`applyQuickData`/`applyStatsData` 入参均为 `{code,msg,data}`（axios 响应 `res.data` 层）。

**遗留说明**：任务完成后的功能/视觉验收由 Business Validator 执行（本项目工作流约定，开发方不预跑验收用例）。`deactivated` 仅在 keep-alive 场景生效，当前 Main.vue 未启用 keep-alive，属防御性代码。
