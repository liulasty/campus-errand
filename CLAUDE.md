# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概况

前后端分离 monorepo：**Spring Boot 2.7.3 后端**（根目录，Java 8 target，本机 JDK 21 可编译，lombok 1.18.30）+ **Vue 2 前端**（`web/`）。校园委托平台（MVP 1.0）：信息撮合 + 信用账本 + 流程协调，不碰资金。

已完成模块（2026-08-01 基线 `mvp1.0-release-base`）：
- **M1 信用引擎**：`com.lz.credit`（constant/dto/strategy）+ `CreditScoreService`，算法「均分60%+完成率40%」，纯新用户默认 60。
- **M2 智能曝光**：`users.CreditScore` 列 + `TaskMapper.searchHallPage/countHallPage` SQL JOIN 排序（DB 侧分页，零内存排序）。
- **M3 防断流**：`com.lz.task` 的 `AutoAdvanceJudge`（纯判定）+ `TaskAutoAdvance` 定时任务（分页扫描+version 乐观锁 claim+锚点计时），`taskupdates.NodeIndex` 记录节点进度，`AUTO_ADVANCE` 留痕。
- **P2 实名认证**：`RealNameAuthenticationService.ensureL1/ensureCurrentUserL1` 门禁（全部委托操作），`IIdentityVerifier` 接口（当前 `ManualIdentityVerifier`，`app.identity-verifier.mode` 切换，未来接教务系统），`usersinfo.identity_no/auth_level/reject_reason`。

## 常用命令

```bash
# 后端（工作目录须为仓库根，含 pom.xml）
mvn compile                          # 编译
mvn test -Dtest=SomeTest             # 跑单个测试类
mvn test -Dtest=A,B,C                # 跑多个
mvn spring-boot:run                  # 启动（端口 80，上下文 /campus_entrustment）

# 前端（先 cd web）
npm run serve                        # dev server（端口 8080，/api 代理到 localhost:80 并 rewrite /api）
npm run build                        # 构建验证
```

**工作目录陷阱**：`cd web` 后 Bash 工作目录会一直停在 `web/`，再跑 `mvn` / `git` 会报「无 POM / pathspec 不匹配」。跨目录操作先 `cd` 回仓库根。

## 架构要点

- 后端包：`controller`（+`admin`/`user` 子包）、`service`（+`impl`）、`mapper`、`pojo`（`entity`/`Enum`/`dto`/`vo`/`result`）、`task`（定时任务）、`credit`、`verifier`、`exceptionHandling`、`config`、`common.security`。
- 全局异常处理器：`com.lz.exceptionHandling.GlobalControllerAdvice`（`UnauthorizedRealNameException` → 返回「请先完成L1实名认证后再执行该操作」）。
- 前端响应约定：后端统一 `{code:1, msg, data}`，`code=1` 为成功；常量 `SUCCESS_CODE` 在 `web/src/constants/http.js`；全局日期过滤器 `| dateTime`（`web/src/main.js` 注册）。
- 安全白名单（`WebSecurityConfig.AUTH_WHITELIST`）：`/user/login`、`/user/register`、`doc.html`、`/img/upload*`、`/common/**` 等；其余接口需 `jwt` 请求头。

## 关键坑（改代码前必读）

1. **mybatis-plus 3.4.3 的 `selectCount` 返回 `Integer`**（不是 Long），需 `.map(Integer::longValue)` 或 `(long)` 强转。
2. **`taskupdates.UpdateType` 是 MySQL ENUM 列** — 新增 `TaskUpdateType` 枚举值必须同时 `ALTER TABLE taskupdates MODIFY UpdateType enum(...)`，否则插入报错。
3. **Vue 2 模板不能直接调用 import 的模块函数**（模板编译成 `with(this)`，只认 data/computed/methods）。import 函数必须挂进 `methods`（如 `creditScore: creditScore`）或包进 computed 才能用。
4. **状态列/枚举序列化**：后端 `@JsonValue` 返回中文 webValue（如 `status` 返回「委托发布中」而非 `ONGOING`）；比较时注意前端用的哪种。
5. **`/admin/task/list` 参数名是 `TypePhase`**（大写），值为 `EDITING_AND_AUDITING`/`PUBLISHING_AND_EXECUTION`/`LIFECYCLE_TERMINATION`；漏传会 `TaskPhase.fromValue(null)` 抛 500。
6. **`Task` 实体瞬态字段** `ownerCredit`/`ownerName`（`@TableField(exist=false)`）只在大厅/管理端查询里富化，其他场景为 null。
7. **无 mysql CLI**：改库用 JDBC 脚本 + `java -cp <mysql-connector.jar>`，驱动在 `D:\CODE\mvn_repository\mysql\mysql-connector-java\8.0.30\`。

## 环境约束（本机）

- **开发工具/配置统一在 `D:\soft-tools`**（Maven 3.6.3/3.9.16、Node/nvm、Redis、Git、IDEA、MySQL 客户端 SQLyog 等）。**不要随意更改该目录下的配置**；如需调整，先与用户确认。
- Maven 本地依赖仓库在 **`D:\soft-tools\.m2\repository`**（用户确认的规范位置；注意 `D:\soft-tools\apache-maven-*/conf/settings.xml` 的 `localRepository` 目前仍指向 `D:\CODE\mvn_repository`，两者并存 — 不要改 soft-tools 下的配置）。
- 后端端口 80、上下文 `/campus_entrustment`（在 gitignored 的 `application-dev.yml`）；前端 8080。
- MySQL：`localhost:3306` 库 `campus_entrustment`，root/1234。SQL 脚本 `src/main/resources/sql/校园委托0.99.sql`。
- **Redis 安装目录在 `D:\soft-tools\redis`，RabbitMQ 未装**：缓存与消息通知功能是否可用取决于对应服务是否启动（后端惰性连接可启动）；定时任务 `TaskAutoAdvance`（`app.auto-advance.*`，node-hours=6/complete-hours=24）依赖 MySQL 可跑。
- 测试账号（MVP 基线）：管理员 `majiaqi/admin123456`；用户 `zhangsan`（student）、`lisiyuan`（teacher）、`testapply` 均 `test123456`。
- 远程：`origin`=github（网络偶发 443 连不上，可重试）、`gitee`=Gitee（较稳定）。代码改动常需同时推两个。

## 技术债（改动前先读）

权威清单在 `docs/MVP_1.0_closure.md`（P0/P1/P2 分级）。**P0 待修复项**：`/user/accept` 空 body NPE；大厅状态列（后端返回中文 webValue `"委托发布中"`，前端比较 `'ONGOING'` 不匹配）；前端方案 A 的 catch/SUCCESS_CODE 未覆盖全部写操作页。Redis/RabbitMQ 未装（消息通知不可用）属环境限制，非必做。其他：旁证展示、信用看板、L2 校园卡、教务对接等均为延后项。

## 文档索引

`docs/_SUMMARY.md` 是总索引。设计稿：`docs/M1_credit-engine-design.md`、`docs/M2_smart-exposure-design.md`、`docs/M3_anti-break-design.md`、`docs/auth-tiered-identity-design.md`、`docs/MVP_1.0_closure.md`（测试账号/技术债/废弃路由清单）。
