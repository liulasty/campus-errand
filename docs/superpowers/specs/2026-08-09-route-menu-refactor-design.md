# 前端路由驱动菜单重构设计

日期：2026-08-09
状态：已批准（共享首页 + 菜单分离 + 路由层守卫）

## 背景与问题

ADMIN 账号（majiaqi）登录后，前端菜单同时展示「工作台」与「平台管理中心」两组。ADMIN 进入「工作台 → 发布委托」时，前端把 `ownerId` 设为当前登录用户自己的 userId，后端 `TaskServiceImpl.createTask` 的守卫 `role == "USER"` 将其拦截并抛出误导性的「用户不存在」。ADMIN 不应看到用户操作界面。

现状：
- `web/src/router/index.js`：扁平路由，全部挂在 `/main` 下，无角色 meta；`beforeEach` 守卫只校验 token，不校验角色。
- `web/src/components/CommonAside.vue`：硬编码 `menuData`（工作台 + 平台管理中心两组），`userPermissions(type)` 只对非 ADMIN 过滤掉「平台管理中心」，导致 ADMIN 两组都可见。
- 登录响应 `userType = role`（后端 `UsersController:124`），存于 localStorage `TaskUser` 与 vuex。

## 目标

1. ADMIN 与 USER 菜单完全分离：USER 只见工作台，ADMIN 只见平台管理中心。
2. 共享首页 `/home`（数据驾驶舱），两级角色登录后都落在 `/home`。
3. 路由层角色守卫：手动输入越权 URL（如 ADMIN 访问 `/createDelegation`）被重定向回 `/home`。
4. 菜单由路由配置生成（单一数据源），不再硬编码。

## 重构后菜单结构

```
数据驾驶舱  /home                 ← 共享首页，[ADMIN, USER]，顶部独立菜单项
[USER] 工作台
  ├ 委托大厅 /viewOnGoingList
  ├ 我的委托
  │  ├ 发布委托 /createDelegation
  │  ├ 我发布的订单 /myDelegationPublishList
  │  └ 我承接的订单 /myDelegationAcceptList
  ├ 消息中心 /messageCenter
  └ 个人中心
     ├ 基础信息 /myInfo
     └ 信用档案 /creditProfile
[ADMIN] 平台管理中心
  ├ 委托管理
  │  ├ 全部委托 /publishedList
  │  ├ 委托审核 /auditList
  │  ├ 未完成委托 /expireDelegationList
  │  └ 履约记录查询 /delegationUpdateRecords
  ├ 用户管理
  │  ├ 用户列表 /userList
  │  └ 实名审核 /admin/realNameAudit
  ├ 平台公告
  │  └ 公告管理 /systemBulletinList
  ├ 消息管理
  │  └ 消息管理 /notificationReadStatus
  └ 系统配置
     ├ 委托分类配置 /delegationType
     └ 敏感词管控 /sensitiveWord
```

## 改动点

### 1. `web/src/router/index.js`

- 路由改为嵌套结构：`/main` 下保留 `/home`（共享首页，`meta.roles: ['ADMIN','USER']`），新增「工作台」「平台管理中心」两个分组路由（无组件，仅结构，含 `meta:{title,icon,roles}`）。
- 叶子路由挂到对应分组下，路径（绝对路径 `/xxx`）保持不变，每个叶子加 `meta:{title,icon}`（`roles` 继承自分组）。
- `beforeEach` 守卫增加角色校验：
  - 从 `localStorage.getItem('TaskUser')` 解析 `userType`（刷新后 vuex 可能为空）。
  - `to.matched` 中存在 `meta.roles` 且不含当前角色 → `next('/home')`。

### 2. 新增 `web/src/router/menu.js`

- `buildMenu(userType)`：递归遍历路由配置，按角色过滤，输出 el-menu 结构（顶层独立项 / 分组 / 子分组 / 叶子）。

### 3. `web/src/components/CommonAside.vue`

- 删除硬编码 `menuData`，`data()` 中改为由 `buildMenu(userType)` 生成。
- 模板支持顶层独立菜单项（数据驾驶舱）——当前模板只有分组（el-submenu），需增加顶层无 children 项的渲染分支。
- 保留 `activeIndex` / `initAside` / `updateMenuState` / 折叠逻辑（基于生成的 menuData 递归取路径）。

## 关键实现细节

1. **角色来源**：路由守卫与菜单均以 `localStorage.TaskUser.userType` 为准；页面刷新后 vuex store 为空，不能仅依赖 store。CommonAside `mounted` 已通过 `setUserInfo()` 从 localStorage 恢复 vuex。
2. **meta 继承**：分组路由设 `meta.roles`，叶子路由继承；叶子各自设 `title`/`icon`。
3. **嵌套渲染**：分组路由无 `component`，vue-router 3 回退到最近有组件的祖先（`/main`）的 `<router-view>` 渲染叶子；分组记录仅参与匹配与 meta 继承，不产生 UI 组件。
4. **守卫角色链**：`to.matched` 含整条匹配链（`/main` → 分组 → 叶子），用 `to.matched.some(r => r.meta.roles && r.meta.roles.includes(userType))` 判定。
5. **登录跳转**：维持 `Login.vue` 的 `router.replace('/home')`。
6. **叶子路径不变**：所有绝对路径保持不变，`$router.push('/xxx')` 与面包屑/标签页引用不受影响。
7. **测试页**：`/page1`、`/page2`、`/page3` 保留，无 `meta.roles`（任何已登录用户可访问，不进菜单）。

## 不做的事

- 不改后端：`createTask` 的 `role == "USER"` 守卫保留（ADMIN 不允许发布委托），仅改进告日志已提交。
- 不改「用户不存在」文案为「仅普通用户可创建委托」——本次范围不含该文案优化（如需要可另行提）。
- 不改 CommonHeader / CommonTag（与角色无关）。
- 不做 403 错误页：越权统一重定向 `/home`（MVP 够用，YAGNI）。

## 验收关注点

- USER（zhangsan/test123456）登录：菜单 = 数据驾驶舱 + 工作台；手动输 `/publishedList` 被重定向 `/home`。
- ADMIN（majiaqi/admin123456）登录：菜单 = 数据驾驶舱 + 平台管理中心；手动输 `/createDelegation` 被重定向 `/home`，不再触发后端「用户不存在」。
- 菜单高亮、面包屑、标签页、二级菜单展开、折叠均正常。
- 前端 `npm run build` 构建通过。

## 影响面

纯前端改动（3 个文件：路由、菜单工具、侧边栏组件），不涉及后端接口契约，不影响 Business Validator 现有用例的接口行为。
