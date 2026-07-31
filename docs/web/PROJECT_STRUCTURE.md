# 项目结构说明文档

## 1. 项目概况

**项目名称**: campus_entrustment（校园委托平台）
**项目描述**: 基于 Vue 2.x 开发的校园委托服务平台，提供用户发布、接受委托及后台管理功能。

## 2. 技术栈

- **核心框架**: Vue.js 2.6.14
- **UI 组件库**: Element UI 2.15.14, Layui 2.9.7
- **状态管理**: Vuex 3.6.2
- **路由管理**: Vue Router 3.6.5
- **HTTP 请求**: Axios 1.8.3
- **模拟数据**: Mock.js 1.1.0
- **图表库**: ECharts 5.1.2
- **其他工具**: `moment`（时间处理）、`js-cookie`（Cookie 操作）

## 3. 目录结构说明

```
campus_entrustment/web/
├── public/                     # 静态资源目录
│   ├── index.html              # HTML 入口文件
│   └── ...                     # 其他静态图片及图标
├── src/                        # 源代码目录
│   ├── api/                    # API 接口管理
│   │   ├── index.js            # 通用接口（委托、用户、上传、通知等）
│   │   ├── user.js             # 用户端接口（发布/接收/确认等）
│   │   ├── mock.js             # Mock 配置
│   │   └── mockServeData/      # Mock 模拟数据处理逻辑
│   ├── assets/                 # 项目静态资源（图片、CSS 等）
│   ├── components/             # 公共组件
│   │   ├── CommonAside.vue     # 通用侧边栏
│   │   ├── CommonHeader.vue    # 通用头部
│   │   ├── CommonTag.vue       # 通用标签页导航
│   │   ├── RegisterForm.vue    # 注册表单
│   │   ├── LoginForm.vue       # 登录表单
│   │   ├── avatarShow.vue      # 头像展示/上传
│   │   ├── ImageUploader.vue   # 图片上传组件
│   │   └── ...                 # 其他业务组件
│   ├── layui/                  # Layui 集成配置
│   │   └── layuiInit.js
│   ├── router/                 # 路由配置
│   │   └── index.js            # 路由定义及拦截器
│   ├── store/                  # Vuex 状态管理
│   │   ├── index.js            # store 入口
│   │   ├── tab.js              # 菜单及标签页状态
│   │   ├── userInfo.js         # 用户信息状态
│   │   └── loading.js          # 加载状态
│   ├── utils/                  # 工具函数
│   │   ├── request.js          # Axios 封装（baseURL `/api/campus_entrustment`）
│   │   ├── globalConfirmAction.js  # 全局确认弹窗
│   │   ├── downloadFile.js     # 文件下载
│   │   └── taskNode.js         # 履约三节点打卡元数据（图标/颜色/中英文映射）
│   ├── views/                  # 页面组件
│   │   ├── auth/Login.vue      # 登录/注册页
│   │   ├── layout/Main.vue     # 主布局容器（侧边栏 + 头部）
│   │   ├── dashboard/Home.vue  # 首页
│   │   ├── public/Landing.vue  # 落地页
│   │   ├── admin/              # 管理员页面（审核、通知、用户管理等）
│   │   ├── user/               # 用户页面（发布委托、我的委托、打卡等）
│   │   └── test/               # 测试页面
│   ├── App.vue                 # 根组件
│   └── main.js                 # 入口文件
├── .gitignore                  # Git 忽略配置
├── babel.config.js             # Babel 配置
├── package.json                # 项目依赖及脚本配置
├── vue.config.js               # Vue CLI 项目配置
└── README.md                   # 项目说明
```

## 4. 核心模块说明

### 4.1 视图层（Views）

页面主要分为前台用户功能和后台管理功能：

- **登录/注册**: `views/auth/Login.vue`（含 `RegisterForm` / `LoginForm` 组件）
- **布局**: `views/layout/Main.vue` 是主要布局容器，包含侧边栏（`CommonAside`）和头部（`CommonHeader`）
- **Admin**: `views/admin/` 目录下包含管理员功能，如审核列表（`AuditList`）、通知管理（`Notifications`）、用户管理（`UserList`）、进度记录（`DelegationUpdateRecords`）等
- **User**: `views/user/` 目录下包含用户功能，如创建委托（`CreateDelegation`）、我的发布/接收（`MyDelegationPublishList` / `MyDelegationAcceptList`）、履约打卡等

### 4.2 状态管理（Store）

使用 Vuex 管理全局状态，主要模块包括：

- `tab`: 管理侧边栏菜单折叠状态及面包屑导航
- `userInfo`: 管理登录用户信息及权限
- `loading`: 全局加载状态

### 4.3 网络请求（API & Utils）

- `utils/request.js`: 封装了 Axios 实例，`baseURL` 默认 `/api/campus_entrustment`，请求头携带 `jwt` 令牌
- `api/index.js`、`api/user.js`: 定义各模块接口请求方法
- `mock`: 项目集成了 Mock.js，用于在无后端接口时模拟数据响应

## 5. 业务变更记录

### 5.1 工具类

- **移除 `utils/ailiyun.js`**：阿里云 OSS 已下线，前端不再持有 OSS 直传逻辑。
- **后端移除 `MailUtils.java`**：邮箱激活邮件已下线，项目无邮件服务依赖。

### 5.2 图片存储

- 唯一图片上传渠道为**七牛云 OSS**，前端通过 `POST /img/upload`、`POST /img/uploadAvatar` 上传，返回域名 + 路径。

### 5.3 委托进度模块

- 后端 `TaskUpdateType` 枚举新增三个**固定履约节点**：`CONTACTED`（已联系）、`PICKED_UP`（已取件）、`DELIVERED`（已送达）。
- 新增打卡上传接口 `POST /taskUpdate/node`（支持打卡图片、定位、备注）。
- 原有 `PROGRESS_UPDATE` 自由文本进度保留，仅作历史数据兼容。

### 5.4 用户模块

- 注册逻辑变更：**取消邮箱激活邮件**，新用户注册后 `isActive` 默认 `true`，可直接登录。

## 6. 常用命令

- **启动开发服务器**:
  ```bash
  npm run serve
  ```
- **构建生产环境**:
  ```bash
  npm run build
  ```
- **代码格式检查**:
  ```bash
  npm run lint
  ```
