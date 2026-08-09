# 前端路由驱动菜单重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构前端路由为「嵌套路由 + meta.roles + 角色守卫」，菜单由路由配置生成，实现 ADMIN/USER 菜单完全分离、共享首页 /home、越权 URL 重定向。

**Architecture:** 路由 `web/src/router/index.js` 改为嵌套结构：`/main` 下共享首页 `/home`（`roles:['ADMIN','USER']`）+「工作台」分组（`roles:['USER']`）+「平台管理中心」分组（`roles:['ADMIN']`）。新增纯函数 `web/src/router/menu.js` 从路由配置生成角色菜单。`CommonAside.vue` 用 `buildMenu` 替换硬编码 `menuData`。`beforeEach` 守卫用 `to.matched` 做角色校验。

**Tech Stack:** Vue 2.6 / vue-router 3.6 / Element UI 2.15（vue-cli 5）

参考设计：`docs/superpowers/specs/2026-08-09-route-menu-refactor-design.md`

> **测试说明**：前端无 jest/单测框架，本项目前端验证方式为 `npm run build` 构建校验 + 手动验收清单（末尾）。不引入新测试框架（YAGNI）。

---

### Task 1: 新增 `web/src/router/menu.js`（buildMenu 纯函数）

**Files:**
- Create: `web/src/router/menu.js`

- [ ] **Step 1: 创建 menu.js**

写入以下内容。纯函数，无 import，避免循环依赖：

```js
// web/src/router/menu.js
// 从路由配置生成按角色过滤的侧边菜单（单一数据源），纯函数无依赖

function toNode(record) {
    return {
        path: record.path,
        name: record.name,
        label: record.meta.title,
        icon: record.meta.icon,
        index: record.path
    }
}

export function buildMenu(routes, userType) {
    const mainRoute = routes.find(r => r.path === '/main')
    if (!mainRoute || !mainRoute.children) {
        return []
    }
    const menu = []
    for (const group of mainRoute.children) {
        // 无标题（默认子路由/遗留路由/详情页）不进菜单
        if (!group.meta || !group.meta.title) {
            continue
        }
        // 分组角色过滤（子路由继承分组角色，无需逐层再判）
        if (group.meta.roles && !group.meta.roles.includes(userType)) {
            continue
        }
        const node = toNode(group)
        if (group.children && group.children.length) {
            node.children = group.children
                .filter(c => c.meta && c.meta.title)
                .map(c => {
                    const childNode = toNode(c)
                    if (c.children && c.children.length) {
                        childNode.children = c.children
                            .filter(cc => cc.meta && cc.meta.title)
                            .map(cc => toNode(cc))
                    }
                    return childNode
                })
        }
        menu.push(node)
    }
    return menu
}
```

- [ ] **Step 2: 提交**

```bash
git add web/src/router/menu.js
git commit -m "feat: 路由驱动菜单 buildMenu 生成器（按角色过滤）"
```

### Task 2: 重构 `web/src/router/index.js`

**Files:**
- Modify: `web/src/router/index.js`

- [ ] **Step 1: 替换路由配置为嵌套结构**

将 `routes` 常量（当前第 33-87 行）整体替换为下方内容（保留原 import 不变）：

```js
const routes = [
    // 登录页面
    {
        path: '/login',
        name: 'login',
        component: Login
    },
    // 主要页面
    {
        path: '/main',
        component: Main,
        children: [
            {
                path: '',
                components: {
                    default: Home
                }
            },
            // 共享首页（两级角色可见）
            {
                path: '/home',
                name: 'home',
                component: Home,
                meta: { title: '数据驾驶舱', icon: 'data-line', roles: ['ADMIN', 'USER'] }
            },
            // 用户工作台（仅 USER）
            {
                path: '/workbench',
                meta: { title: '工作台', icon: 's-home', roles: ['USER'] },
                children: [
                    {
                        path: '/viewOnGoingList',
                        name: 'viewOnGoingList',
                        component: ViewOnGoingList,
                        meta: { title: '委托大厅', icon: 'search' }
                    },
                    {
                        path: '/myTasks',
                        meta: { title: '我的委托', icon: 's-order' },
                        children: [
                            {
                                path: '/createDelegation',
                                name: 'createDelegation',
                                component: CreateDelegation,
                                meta: { title: '发布委托', icon: 'edit-outline' }
                            },
                            {
                                path: '/myDelegationPublishList',
                                name: 'myDelegationPublishList',
                                component: MyDelegationPublishList,
                                meta: { title: '我发布的订单', icon: 'document-add' }
                            },
                            {
                                path: '/myDelegationAcceptList',
                                name: 'myDelegationAcceptList',
                                component: MyDelegationAcceptList,
                                meta: { title: '我承接的订单', icon: 'document-checked' }
                            }
                        ]
                    },
                    {
                        path: '/messageCenter',
                        name: 'messageCenter',
                        component: MessageCenter,
                        meta: { title: '消息中心', icon: 'bell' }
                    },
                    {
                        path: '/profile',
                        meta: { title: '个人中心', icon: 'user' },
                        children: [
                            {
                                path: '/myInfo',
                                name: 'myInfo',
                                component: MyInfo,
                                meta: { title: '基础信息', icon: 'user-solid' }
                            },
                            {
                                path: '/creditProfile',
                                name: 'creditProfile',
                                component: CreditProfile,
                                meta: { title: '信用档案', icon: 'medal' }
                            }
                        ]
                    },
                    // 详情页：不进菜单，但属于 USER 域（继承工作台 USER 角色，防 ADMIN 绕过）
                    {
                        path: '/myDelegationProgress',
                        name: 'myDelegationProgress',
                        component: MyDelegationProgress
                    }
                ]
            },
            // 平台管理中心（仅 ADMIN）
            {
                path: '/adminPanel',
                meta: { title: '平台管理中心', icon: 's-tools', roles: ['ADMIN'] },
                children: [
                    {
                        path: '/delegationAdmin',
                        meta: { title: '委托管理', icon: 's-order' },
                        children: [
                            {
                                path: '/publishedList',
                                name: 'publishedList',
                                component: PublishedList,
                                meta: { title: '全部委托', icon: 'tickets' }
                            },
                            {
                                path: '/auditList',
                                name: 'auditList',
                                component: AuditList,
                                meta: { title: '委托审核', icon: 's-check' }
                            },
                            {
                                path: '/expireDelegationList',
                                name: 'expireDelegationList',
                                component: ExpireDelegationList,
                                meta: { title: '未完成委托', icon: 'warning-outline' }
                            },
                            {
                                path: '/delegationUpdateRecords',
                                name: 'delegationUpdateRecords',
                                component: DelegationUpdateRecords,
                                meta: { title: '履约记录查询', icon: 's-flag' }
                            }
                        ]
                    },
                    {
                        path: '/userAdmin',
                        meta: { title: '用户管理', icon: 'user' },
                        children: [
                            {
                                path: '/userList',
                                name: 'userList',
                                component: UserList,
                                meta: { title: '用户列表', icon: 'user' }
                            },
                            {
                                path: '/admin/realNameAudit',
                                name: 'realNameAudit',
                                component: RealNameAudit,
                                meta: { title: '实名审核', icon: 'postcard' }
                            }
                        ]
                    },
                    {
                        path: '/bulletinAdmin',
                        meta: { title: '平台公告', icon: 'chat-dot-round' },
                        children: [
                            {
                                path: '/systemBulletinList',
                                name: 'systemBulletinList',
                                component: SystemBulletinList,
                                meta: { title: '公告管理', icon: 'chat-dot-round' }
                            }
                        ]
                    },
                    {
                        path: '/noticeAdmin',
                        meta: { title: '消息管理', icon: 'bell' },
                        children: [
                            {
                                path: '/notificationReadStatus',
                                name: 'notificationReadStatus',
                                component: NotificationReadStatus,
                                meta: { title: '消息管理', icon: 's-comment' }
                            }
                        ]
                    },
                    {
                        path: '/systemConfig',
                        meta: { title: '系统配置', icon: 'setting' },
                        children: [
                            {
                                path: '/delegationType',
                                name: 'delegationType',
                                component: DelegationType,
                                meta: { title: '委托分类配置', icon: 'menu' }
                            },
                            {
                                path: '/sensitiveWord',
                                name: 'sensitiveWord',
                                component: SensitiveWordConfig,
                                meta: { title: '敏感词管控', icon: 'lock' }
                            }
                        ]
                    }
                ]
            },
            // 未接入菜单的遗留路由（不进菜单，保持原可访问性；无角色限制）
            { path: '/draftList', name: 'draftList', component: DraftList },
            { path: '/systemNoticeList', name: 'systemNoticeList', component: SystemNoticeList },
            { path: '/notifications', name: 'notifications', component: Notifications },
            { path: '/page1', name: 'page1', component: pageOne },
            { path: '/page2', name: 'page2', component: pageTwo },
            { path: '/page3', name: 'page3', component: pageThree }
        ]
    },
    // 默认页面
    {
        path: '/',
        name: 'landing',
        component: Landing
    }
]
```

- [ ] **Step 2: 替换路由守卫为带角色校验的版本**

将 `beforeEach`（当前第 110-138 行）整体替换为下方内容：

```js
// 读取当前登录用户角色（刷新后 vuex 为空，以 localStorage 为准）
function getUserType() {
    try {
        const taskUser = JSON.parse(localStorage.getItem('TaskUser') || '{}')
        return taskUser.userType || ''
    } catch (e) {
        return ''
    }
}

// 添加全局前置导航守卫：token + 角色
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('jwtToken');
    const LOGIN_PATH = '/login';
    const LANDING_PATH = '/';
    const publicPaths = [LOGIN_PATH, LANDING_PATH];

    if (token) {
        // 已登录用户访问登录页 → 回首页
        if (to.path === LOGIN_PATH) {
            next('/home');
            return;
        }
        // 角色校验：目标路由链中存在角色要求且当前角色不满足 → 回首页
        const userType = getUserType();
        const hasRoleRestriction = to.matched.some(r => r.meta && r.meta.roles);
        const isAllowed = to.matched.some(r => r.meta && r.meta.roles && r.meta.roles.includes(userType));
        if (hasRoleRestriction && !isAllowed) {
            next('/home');
        } else {
            next();
        }
    } else {
        // 未登录
        if (publicPaths.includes(to.path)) {
            next();
        } else {
            next(LOGIN_PATH);
        }
    }
});
```

- [ ] **Step 3: 提交**

```bash
git add web/src/router/index.js
git commit -m "feat: 路由改嵌套结构+meta.roles，守卫增加角色校验防越权"
```

### Task 3: 重构 `web/src/components/CommonAside.vue`

**Files:**
- Modify: `web/src/components/CommonAside.vue`

- [ ] **Step 1: 替换模板为支持顶层独立项 + 分组 + 子分组的版本**

将 `<template>` 内 `el-menu` 的 `<el-submenu v-for="group in menuData">...</el-submenu>` 结构（当前第 17-39 行）整体替换为：

```html
        <template v-for="item in menuData">
            <el-menu-item v-if="!item.children" :key="item.index" :index="item.index" @click="clickMenu(item)">
                <i :class="`el-icon-${item.icon}`"></i>
                <span slot="title">{{ item.label }}</span>
            </el-menu-item>
            <el-submenu v-else :key="item.index" :index="item.index">
                <template slot="title">
                    <i :class="`el-icon-${item.icon}`"></i>
                    <span slot="title">{{ item.label }}</span>
                </template>
                <template v-for="child in item.children">
                    <el-menu-item v-if="!child.children" :key="child.index" :index="child.index" @click="clickMenu(child)">
                        <i :class="`el-icon-${child.icon}`"></i>
                        <span slot="title">{{ child.label }}</span>
                    </el-menu-item>
                    <el-submenu v-else :key="child.index" :index="child.index">
                        <template slot="title">
                            <i :class="`el-icon-${child.icon}`"></i>
                            <span slot="title">{{ child.label }}</span>
                        </template>
                        <el-menu-item v-for="grand in child.children" :key="grand.index" :index="grand.index"
                            @click="clickMenu(grand)">
                            <i :class="`el-icon-${grand.icon}`"></i>
                            <span slot="title">{{ grand.label }}</span>
                        </el-menu-item>
                    </el-submenu>
                </template>
            </el-submenu>
        </template>
```

- [ ] **Step 2: 整体替换 `<script>` 块**

将整个 `<script>...</script>` 块（当前从 `<script>` 到 `</script>`，含原 `export default` 全部内容）替换为下方完整内容：

```js
<script>
    import { buildMenu } from '../router/menu'

    export default {
        watch: {
            '$route'(to, from) {
                this.updateMenuState(to.path);
            }
        },
        data() {
            return {
                isUniqueOpened: true,
                activeIndex: '/home',
                openeds: [],
                menuData: []
            };
        },
        methods: {
            handleOpen(key, keyPath) {
                console.log(key, keyPath);
            },
            handleClose(key, keyPath) {
                console.log(key, keyPath);
            },
            clickMenu(item) {
                console.log("点击菜单", item)
                if (this.$route.path !== item.path) {
                    this.$router.push(item.path)
                }
                this.$store.commit('selectMenu', item)
            },
            refreshPage() {
                this.$router.go(0);
            },
            setUserInfo() {
                const userInfo = localStorage.getItem('TaskUser')
                if (userInfo) {
                    this.$store.commit('loginUser', JSON.parse(userInfo))
                }
            },
            extractPathsAndIndices() {
                const result = [];
                const walk = (items) => {
                    items.forEach(item => {
                        if (item.children) {
                            walk(item.children);
                        } else {
                            result.push({ path: item.path, index: item.index });
                        }
                    });
                };
                walk(this.menuData);
                return result;
            },
            initAside() {
                const currentPath = this.$route.path;
                const walk = (items) => {
                    items.forEach(item => {
                        if (item.children) {
                            walk(item.children);
                        } else if (item.path === currentPath) {
                            this.$store.commit('selectMenu', item);
                            this.activeIndex = item.index;
                        }
                    });
                };
                walk(this.menuData);
            },
            updateMenuState(path) {
                const activeItem = this.extractPathsAndIndices().find(item => item.path === path);
                if (activeItem) {
                    this.activeIndex = activeItem.index;
                } else {
                    this.activeIndex = '/home';
                }
            }
        },
        mounted() {
            this.setUserInfo()
            this.menuData = buildMenu(this.$router.options.routes, this.$store.state.userInfo.userType)
            this.initAside()
        },
        computed: {
            isCollapse() {
                return this.$store.state.tab.isCollapse
            }
        }
    }
</script>
```

> 注意：原 `data()` 里硬编码的 `menuData` 数组与 `userPermissions(type)` 方法随之删除；`computed.isCollapse` 已包含在新版本中。

- [ ] **Step 3: 提交**

```bash
git add web/src/components/CommonAside.vue
git commit -m "feat: 侧边菜单改由路由配置生成，支持角色分离与顶层独立项"
```

### Task 4: 构建校验 + 手动验收清单

**Files:**（只读验证，不改代码）

- [ ] **Step 1: 前端构建校验**

工作目录切到 `web/` 后执行：

```bash
cd web && npm run build
```

预期：`Compiled successfully`，无报错。（dist/ 已 gitignore，不提交）

- [ ] **Step 2: 手动验收清单（由用户在前端 dev server 复核）**

| 场景 | 操作 | 预期 |
|------|------|------|
| USER 菜单 | zhangsan/test123456 登录 | 菜单 = 数据驾驶舱 + 工作台（委托大厅/我的委托/消息中心/个人中心），无平台管理中心 |
| USER 越权 | 手动访问 `/publishedList` | 被重定向回 `/home` |
| ADMIN 菜单 | majiaqi/admin123456 登录 | 菜单 = 数据驾驶舱 + 平台管理中心（委托/用户/公告/消息/系统配置），无工作台 |
| ADMIN 越权 | 手动访问 `/createDelegation` | 被重定向回 `/home`，不再触发后端「用户不存在」 |
| 功能回归 | USER 走「我承接的订单 → 查看进度」 | `/myDelegationProgress` 可正常打开（不受菜单重构影响） |
| 菜单交互 | 高亮/面包屑/标签页/二级展开/折叠 | 均正常 |
| 未登录 | 未带 token 访问 `/home` | 跳转登录页 |

- [ ] **Step 3: 问题回退**

若构建失败或验收异常，回到对应 Task 修正并重新提交；验证不改代码，无额外提交。

---

## 说明

- 纯前端改动（3 个文件），不涉及后端接口契约，无需 DSL 用例。
- 叶子路由绝对路径全部保持不变，`$router.push('/xxx')` 引用（如 `MyDelegationAcceptList.vue:484`）不受影响。
- 后端 `createTask` 的 `role == "USER"` 守卫保留（ADMIN 不允许发布委托），与本重构解耦。
