# Campus Errand · 校园委托跑腿平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.3-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-2.x-4fc08d)](https://vuejs.org)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

> 一个完整的前后端分离校园委托服务平台。学生可以发布跑腿任务（代买午餐、代取快递等），也可以接单赚取报酬，支持任务审核、评价互评、消息通知等完整流程。

---

## 功能概览

| 模块 | 功能 |
|------|------|
| **用户系统** | 注册登录（JWT）、个人信息管理、学生实名认证 |
| **委托任务** | 发布委托（支持草稿）、接受委托、任务状态流转（待接受 → 进行中 → 待确认 → 已完成） |
| **审核机制** | 管理员后台审核发布的任务，保障平台安全 |
| **评价系统** | 任务完成后双方互评，建立信用体系 |
| **消息通知** | 系统公告 + 任务状态变更实时通知 |
| **后台管理** | 用户管理、任务分类、系统设置、公告发布 |

---

## 技术栈

### Backend

| 技术 | 用途 |
|------|------|
| Spring Boot 2.7.3 | 核心框架 |
| MyBatis Plus 3.4.3 | ORM |
| MySQL + Druid | 数据库与连接池 |
| Redis | 缓存 |
| RabbitMQ | 消息队列 |
| Spring Security + JWT | 认证授权 |
| Aliyun OSS | 对象存储 |
| Knife4j | API 文档 |
| EasyExcel / POI | Excel 处理 |

### Frontend

| 技术 | 用途 |
|------|------|
| Vue 2 | 前端框架 |
| Vue Router | 路由 |
| Vuex | 状态管理 |
| Axios | HTTP 客户端 |
| Element UI | UI 组件库 |

---

## 快速启动

### 环境要求

- JDK 8+, Maven 3.6+
- Node.js + npm
- MySQL 5.7+, Redis, RabbitMQ

### 后端

```bash
# 1. 创建数据库 campus_entrustment，执行 SQL
mysql -u root -p campus_entrustment < src/main/resources/sql/校园委托0.99.sql

# 2. 配置数据库/Redis/RabbitMQ/OSS 连接信息
# 编辑 src/main/resources/application-dev*.yml

# 3. 启动
mvn spring-boot:run

# API 文档: http://localhost:8080/doc.html
```

### 前端

```bash
cd web
npm install
npm run serve

# 页面: http://localhost:8081
```

### Docker 部署

```bash
docker-compose up -d
```

---

## 项目结构

```
campus-errand/
├── web/                     # Vue 2 前端
│   ├── src/
│   │   ├── api/             # 接口封装
│   │   ├── components/      # 公共组件
│   │   ├── router/          # 路由
│   │   ├── store/           # 状态管理
│   │   ├── utils/           # 工具函数
│   │   └── views/           # 页面
│   └── package.json
│
├── src/                     # Spring Boot 后端
│   ├── main/java/com/lz/
│   │   ├── controller/      # 控制器
│   │   ├── service/         # 业务逻辑
│   │   ├── mapper/          # 数据访问
│   │   ├── pojo/            # 实体/DTO/VO
│   │   ├── config/          # 配置
│   │   ├── common/          # 通用模块
│   │   └── utils/           # 工具类
│   └── main/resources/
│       ├── mapper/          # MyBatis XML
│       └── application.yml
│
├── docker/                  # Dockerfile
├── docker-compose.yml       # 编排部署
├── scripts/                 # 构建脚本
└── pom.xml
```

---

## 相关链接

- GitHub: [liulasty/campus-errand](https://github.com/liulasty/campus-errand)
- 协议: [MIT License](LICENSE)
