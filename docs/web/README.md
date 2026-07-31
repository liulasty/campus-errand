# campus_entrustment（校园委托平台 · 前端）

基于 Vue 2 的校园委托服务平台前端，配套 Spring Boot 后端（仓库根目录）。

## 快速开始

```bash
npm install
npm run serve
# 开发页面: http://localhost:8080/campus_entrustment/
```

生产构建与代码检查：

```bash
npm run build
npm run lint
```

## 关键业务说明

### 注册 / 登录

- 注册后**直接登录**，无需邮箱验证（后端已移除激活邮件流程）。
- 登录鉴权使用 JWT，请求头 `jwt` 携带令牌。

### 委托履约进度（三节点打卡）

- 接收者在任务执行中按固定节点打卡：**已联系 → 已取件 → 已送达**。
- 每个节点支持上传**打卡图片**、填写**定位**与简短备注。
- 已提交的节点按钮置灰，不可重复提交。
- 发布方与管理员在「任务动态 / 进度记录」中区分展示结构化打卡与历史自由文本进度。

### 自动评价

- 任务完成后 **24 小时**内若无人工评价，系统自动生成中性好评（3 星，标注「系统自动评价」）。
- 阈值可通过后端配置 `app.auto-review.delay-hours` 调整（演示时可调小快速触发）。

## 技术栈

Vue 2.6 · Element UI 2.15 · Vuex 3 · Vue Router 3 · Axios · ECharts
