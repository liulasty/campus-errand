# 校园委托服务平台 (Campus Entrustment Platform)

一个完整的前后端分离校园委托服务平台（跑腿系统），旨在为校园内的师生提供一个便捷、高效的互助平台。

## ✨ 核心功能

- **用户模块**: 注册登录（JWT）、个人信息管理、学生实名认证
- **委托任务模块**: 发布委托（支持草稿）、接受委托、任务状态流转（待接受→进行中→待确认→已完成）、后台审核
- **评价系统**: 任务完成后发布者和接受者可互评
- **消息通知**: 系统公告 + 任务状态变更通知
- **后台管理**: 用户管理、任务分类管理、系统设置

## 🛠 技术栈

### 后端
- **核心框架**: Spring Boot 2.7.3
- **ORM 框架**: MyBatis Plus 3.4.3
- **数据库**: MySQL 5.7+
- **缓存**: Redis
- **消息队列**: RabbitMQ
- **权限安全**: Spring Security + JWT
- **对象存储**: Aliyun OSS
- **API 文档**: Knife4j (Swagger)
- **工具库**: Lombok, EasyExcel, POI, FastJSON

### 前端
- **框架**: Vue 2
- **路由**: Vue Router
- **状态管理**: Vuex
- **HTTP 请求**: Axios
- **UI 组件**: Element UI, Layui
- **构建工具**: Vue CLI

## 🚀 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- Node.js + npm
- MySQL 5.7+
- Redis
- RabbitMQ

### 后端启动

```bash
# 1. 数据库初始化
# 创建数据库 campus_entrustment，执行 src/main/resources/sql/校园委托0.99.sql

# 2. 配置修改
# 编辑 src/main/resources/application.yml 和相关 profile 配置

# 3. 启动
mvn spring-boot:run

# 访问 API 文档: http://localhost:8080/doc.html
```

### 前端启动

```bash
cd web
npm install
npm run serve

# 访问: http://localhost:8081
```

## 📂 目录结构

```
├── web/                         # 前端 (Vue 2)
│   ├── src/
│   │   ├── api/                 # API 接口封装
│   │   ├── components/          # 公共组件
│   │   ├── layout/              # 布局组件
│   │   ├── router/              # 路由配置
│   │   ├── store/               # 状态管理
│   │   ├── utils/               # 工具函数
│   │   └── views/               # 页面视图
│   ├── package.json
│   └── vue.config.js
│
├── src/                         # 后端 (Spring Boot)
│   ├── main/java/com/lz/
│   │   ├── common/              # 通用模块
│   │   ├── config/              # 配置类
│   │   ├── controller/          # 控制器层
│   │   ├── core/                # 核心框架
│   │   ├── mapper/              # DAO 层
│   │   ├── pojo/                # 实体/DTO/VO
│   │   ├── service/             # 业务逻辑层
│   │   └── utils/               # 工具类
│   └── main/resources/
│       ├── mapper/              # MyBatis XML
│       ├── sql/                 # SQL 脚本
│       └── application.yml
│
├── docker/                      # Docker 部署配置
├── docker-compose.yml
├── scripts/                     # 构建部署脚本
└── pom.xml
```

## 📄 许可证

[MIT License](LICENSE)
