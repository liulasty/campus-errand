# 首页数据驾驶舱重构设计（Home.vue）

日期：2026-08-09
范围：前端单页重构 —— `web/src/views/dashboard/Home.vue`
不动后端接口。

## 背景与目标

首页（`/home`，ADMIN + USER 共享）当前存在布局、样式、JS 三层问题：

- 布局：固定 `span 8+16` 左右分栏无响应式；统计卡硬编码 32% 宽度难对齐；左侧两卡高度固定 300px 溢出不可控；模块垂直堆叠无权重。
- 样式：Element-UI 原生卡片堆砌，无统一圆角/阴影/间距/配色规范；公告滚动区硬编码高度；统计卡无 hover 动效；`/deep/` 零散耦合。
- JS：`generateEchart2` 缺失导致「新增数据」图表永远空白；两接口并行渲染时序不可控；echarts 实例不销毁造成内存泄漏；mock 假数据；无 resize 自适应；无加载骨架与空态；`dateTime` 过滤无容错。

目标：按业务权重重排模块，修复全部 JS 缺陷，建立统一视觉规范，实现响应式自适应。

## 布局结构（优先级式看板）

采用上中下 + 栅格自适应，权重从上到下递减：

- **Row 1 核心指标区**：6 个统计仪表卡，`:gutter="20"`，每卡 `:xs="24" :sm="12" :md="8"`（大屏一行 3、平板 2、手机 1）。
- **Row 2 中层双栏**：
  - 左栏（`:xs="24" :md="10"`）：系统公告轮播 + 最新委托清单。
  - 右栏（`:xs="24" :md="14"`）：热门委托柱状图。
- **Row 3 底层四宫格**（各 `:xs="24" :md="12"`）：
  1. 新增委托趋势折线图（修复 echarts2）
  2. 委托状态占比饼图
  3. 接单达人 TOP5 排行
  4. 快捷操作入口（按角色区分）

## 视觉规范（现代简约浅色）

- 页面背景沿用 `#f0f2f5`；卡片纯白，圆角 10px。
- 阴影分层：常态 `0 2px 8px rgba(0,0,0,0.06)`；hover `0 6px 20px rgba(0,0,0,0.12)` + 上浮（`transform: translateY(-2px)`）。
- 配色沿用 Element 语义色：主色 `#409EFF`、成功 `#67C23A`、预警 `#E6A23C`、危险 `#F56C6C`，保证全站一致。
- 指标卡两色系区分：接收组（前 3 卡）vs 发布组（后 3 卡），具体色值见模块 1；用渐变色图标块 + 左侧色条。
- 间距规范：卡片间距 20px（gutter/margin）；图标文字间距 14px；页面内边距 20px。
- 文字规范：卡片标题 16px/600/`#303133`；次级/描述 14px/`#606266`；辅助 12px/`#909399`。
- 卡片头部统一：色点/图标 + 标题 + 右上角「更多」文本按钮（`el-button type="text"`）。
- 本页所有自定义 class 统一 `dashboard-` 前缀（如 `dashboard-card`、`dashboard-metric`），彻底杜绝命名冲突；移除散落 `/deep/`，样式收敛到 scoped 内。

## 模块细节

1. **指标卡**：渐变图标块（60px）+ 数值（24px/bold）+ 名称（12px/灰），hover 上浮 + 阴影加深。按卡序分两套色系（前 3 接收 / 后 3 发布）：
   - 接收组：今日已接受 `#2ec7c9`、本周已接受 `#409EFF`、本月已接受 `#67C23A`
   - 发布组：今日已发布 `#ffb980`、本周已发布 `#E6A23C`、本月已发布 `#F56C6C`
2. **公告轮播**：`el-carousel` 不支持完全自适应高度，采用折中方案——容器固定高 280px，单条公告内容区 `max-height: 220px` 内部滚动；保留置顶标签；描述区 `overflow-y: auto`。
3. **最新委托**：删除 mock 默认值；类型用彩色 `el-tag`；时间用 `dateTime` 过滤并容错；空数据显示 `el-empty`。
4. **热门柱状图**：y 轴类别动态生成，删除 `['Brazil','Indonesia',...]` 硬编码占位；空数据提示。
5. **新增趋势折线图（修复 echarts2）**：数据源用现有 `transactionStats`，x 轴 `["今日","本周","本月"]`，两条 series；零后端改动。字段映射：
   - series-已接受：`[今日已接受, 本周已接受, 本月已接受]`
   - series-已发布：`[今日已发布, 本周已发布, 本月已发布]`
6. **状态占比饼图**：沿用 `stats.statusCounts`，过滤 value>0；空数据显示空态。
7. **TOP5 排行**：前三名奖牌色高亮（`el-table-column` 插槽动态绑定文字颜色）；空数据 `el-empty`；`v-loading` 由 `loadingStats` 控制。
   - index 0 → 金 `#E6A23C`；index 1 → 银 `#909399`；index 2 → 铜 `#cf8c4e`；index ≥3 → 默认文字色。
8. **快捷入口**：3 张入口卡（图标 + 文案 + hover），按角色区分；`mounted` 最先从 `localStorage.TaskUser` 读取 `userType` 决定入口分支：
   - USER：发布委托 `/createDelegation`、委托大厅 `/viewOnGoingList`、我的接单 `/myDelegationAcceptList`
   - ADMIN：委托审核 `/auditList`、全部委托 `/publishedList`、用户管理 `/userList`

## JS 重构（全部修复）

- 删除 mock `tableData`/`countData` 假数据，全部改为响应式空值。
- 两个接口（`getData(2)`、`getDashboardStats()`）用 `Promise.allSettled` 并行加载并各自独立 `catch`，数据就绪后统一渲染；任一接口失败不阻塞另一接口对应模块渲染，避免单点失败整页空白。
- 所有 `initChart` 初始化必须放在 `$nextTick` 回调内，并校验 `$refs` 存在，防止 DOM 未就绪时 `echarts.init(undefined)` 报错。
- echarts 统一封装 `initChart(ref)`：实例缓存到 `this._charts`；`beforeDestroy` 统一 `dispose()`，修复多实例内存泄漏与容器复用报错。
- 若页面处于 keep-alive 缓存下 `beforeDestroy` 不触发，补充 `deactivated` 钩子同样销毁全部 echarts 实例。
- `window.resize` 监听 → 各实例 `chart.resize()`；`beforeDestroy` / `deactivated` 成对移除监听。
- 页面整体 `v-loading` 骨架；各模块空数据用 `el-empty` / 提示文案；`hotTaskCategory` 为空 `{}` 时柱状图捕获空数组展示空态。
- `dateTime` 过滤容错：null / undefined / 空字符串 / 非法时间戳统一返回 `'—'`。
- 移除已废弃的大段注释代码（旧 getData 双图逻辑）。

## 数据契约（沿用，不新增接口）

- `getData(2)` → `{ systemAnnouncements, newestTask, hotTaskCategory, transactionStats, tasksWithUser }`
  - `newestTask` → 最新委托表格；`transactionStats` → 指标卡 + 趋势折线图；`hotTaskCategory` → 热门柱状图；`systemAnnouncements` → 公告轮播。
- `getDashboardStats()` → `{ todayNewTask, todayAccepted, statusCounts, categoryCounts, acceptRanking }`
  - `statusCounts` → 饼图；`acceptRanking` → TOP5 排行。

## 响应式断点

- md ≥992：指标卡 3 列；sm 768–992：2 列；xs <768：1 列全宽。
- 中层/底层栅格窄屏自动堆叠，无横向挤压。
- 图表容器高度：md 大屏柱状图 320px、饼图与折线图 260px；sm/xs 移动端全部图表容器降至 230px。

## 验收要点

- 大屏（≥1200）、平板（768–992）、手机（<768）三档布局正确流式堆叠。
- echarts2 折线图正常渲染（发布/接受 × 今日/本周/本月）。
- 快速切换路由多次进出首页，无图表报错（实例已 dispose，含 keep-alive 下 deactivated 销毁）。
- 浏览器缩放窗口，图表自适应 resize。
- 接口返回空时各模块显示空态，无假数据。
- `getDashboardStats()` 失败时 getData 侧模块（公告/指标卡/最新委托）仍正常渲染（allSettled 独立容错）。
- TOP5 前三名奖牌色正确高亮（金/银/铜）。
- 后台渲染确认后由 Business Validator 跑测（本工作流不预跑）。

## 范围外

- 不改动任何后端接口 / Mapper / Controller。
- 不新增「近 7/30 日每日新增」真实时间序列接口（如需后续单独排期）。
- 全局主题（侧边栏、Header、全局字号）不在本次范围，仅重构首页。
