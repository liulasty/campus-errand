

# Campus Errand · 校园委托跑腿平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.3-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-2.x-4fc08d)](https://vuejs.org)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

> 一个前后端分离的校园委托服务平台。学生可以发布跑腿任务（代买午餐、代取快递等），也可以接单赚取报酬。平台支持任务审核、评价互评、消息通知等完整流程，全程不碰资金，靠「信用 + 履约」驱动服务闭环。

---

## 核心特性

- **发布委托**：支持草稿保存、任务描述、地点、金额等信息管理
- **接单系统**：用户可承接委托任务，完成后提交审核
- **审核机制**：管理员审核发布的任务，保障平台安全
- **信用体系**：基于任务完成率和评价计算用户信用分
- **履约打卡**：三节点打卡机制 + 自动推进定时任务
- **消息通知**：系统公告与任务状态实时推送
- **后台管理**：用户管理、任务分类、系统设置、公告发布

---

## 技术栈

### 后端

| 技术 | 用途 |
|------|------|
| Spring Boot 2.7.3 | 核心框架 |
| MyBatis Plus 3.4.3 | ORM |
| MySQL + Druid | 数据库与连接池 |
| Redis | 缓存 |
| RabbitMQ | 消息队列 |
| Spring Security + JWT | 认证授权 |
| 七牛云对象存储 | 图片上传/头像 |
| Knife4j | API 文档 |
| EasyExcel / POI | Excel 处理 |

### 前端

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

- JDK 8~17（推荐 JDK 17）
- Maven 3.6+
- Node.js + npm
- MySQL 8.0+，Redis，RabbitMQ

> 注：Redis 和 RabbitMQ 未启动时后端可正常启动，但缓存、消息通知相关功能不可用。

### 后端

```bash
# 1. 创建数据库并执行 SQL
mysql -u root -p campus_entrustment < src/main/resources/sql/校园委托0.99.sql

# 2. 配置数据库/Redis/RabbitMQ/OSS 连接信息
# 编辑 src/main/resources/application-dev*.yml

# 3. 启动（默认端口 80，路径 /campus_entrustment）
mvn spring-boot:run

# API 文档: http://localhost/campus_entrustment/doc.html
```

### 前端

```bash
cd web
npm install
npm run serve

# 页面: http://localhost:8080/campus_entrustment/
```

### Docker 部署

```bash
docker-compose up -d
```

---

## 项目结构

```
campus-errand/
├── web/                      # Vue 2 前端
│   ├── src/
│   │   ├── api/              # 接口封装
│   │   ├── components/       # 公共组件
│   │   ├── router/           # 路由配置
│   │   ├── store/            # 状态管理
│   │   ├── utils/            # 工具函数
│   │   └── views/            # 页面视图
│   └── package.json
│
├── src/main/java/com/lz/     # Spring Boot 后端
│   ├── controller/           # 控制器层
│   ├── service/              # 业务逻辑层
│   ├── mapper/               # 数据访问层
│   ├── pojo/                 # 实体/DTO/VO
│   ├── config/               # 配置类
│   ├── common/               # 通用模块
│   ├── utils/                # 工具类
│   └── credit/               # 信用计算模块
│
├── src/main/resources/
│   ├── mapper/               # MyBatis XML
│   └── application.yml       # 配置文件
│
├── docker/                   # Docker 配置
├── scripts/                  # 构建脚本
└── docs/                     # 项目文档
```

---

## 文档索引

- [产品目标文档](docs/MVP_1.0.md)
- [实施路线图](docs/ROADMAP_1.0.md)
- [完整文档索引](docs/_SUMMARY.md)

---

## 协议

[MIT License](LICENSE)

---

## 相关链接

- GitHub: [liulasty/campus-errand](https://github.com/liulasty/campus-errand)
- Gitee: [campus-errand](https://gitee.com/Maybe_I_wrong/campus-errand)