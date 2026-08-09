# 后端日志本地化配置设计

日期：2026-08-09
状态：已批准（方案 A）

## 需求

后端项目增加日志本地化配置，包含三部分：
1. **本地文件落盘** —— 日志输出到本地磁盘文件（按天滚动），当前只打控制台、重启即丢。
2. **时区/编码本地化** —— 日志时间强制 Asia/Shanghai，文件 UTF-8 编码。
3. **控制台中文乱码修复** —— Windows/IDEA 控制台中文日志乱码问题。

## 方案

采用**方案 A：纯 `application.yml` 配置**，不新增 `logback-spring.xml`，利用 Spring Boot 2.7 原生日志属性。

## 改动清单

### 1. `src/main/resources/application.yml` — logging 块改造

保留 `group`（ebank: com.lz.controller）与 `level`（mapper debug / service info / controller info / config debug / root INFO 等）**不动**。

变更点：
- 移除现有 `pattern.file`（改用带时区的 pattern）
- 新增 `charset.console` / `charset.file` 为 `UTF-8`
- 新增 `pattern.console` / `pattern.file`，时间格式 `%d{yyyy-MM-dd HH:mm:ss.SSS, Asia/Shanghai}`，消息格式沿用现有风格 `[%thread] %-6level %logger{36} - %msg%n`
- 新增 `file.name: logs/server.log`
- 新增 `logback.rollingpolicy`：
  - `file-name-pattern: logs/server.%d{yyyy-MM-dd}.%i.log`（按天滚动）
  - `max-file-size: 100MB`
  - `max-history: 30`（保留 30 天）
  - `total-size-cap: 10GB`（磁盘上限保险）

Spring Boot 2.7 默认 Logback 配置据此自动生成 `SizeAndTimeBasedRollingPolicy`。

### 2. `.gitignore` — 排除日志目录

新增 `logs/`（现有 `*.log` 只覆盖文件不覆盖目录内命名）。

## 不动的部分

- `logging.level.*` 与 `logging.group` 保留原值，Spring Boot 仍会应用到 Logback logger。
- MyBatis `log-impl: StdOutImpl`（SQL 打控制台）不变。
- `application-dev*.yml` 无日志配置，不涉及。

## 控制台中文乱码（IDEA 侧一次性设置）

编码由 JVM 输出 + IDEA 解码两端共同决定，仅改项目配置无法完全解决。需在 IDEA：`Run/Debug Configuration → Console → Encoding = UTF-8`（JDK 21 默认 file.encoding 即 UTF-8，`charset.console=UTF-8` 后 IDEA 按 UTF-8 解码即正常显示中文）。

## 验证方式

启动后端（`mvn spring-boot:run` 或 IDEA），确认：
- 仓库根 `logs/` 目录自动生成 `server.log`，内容 UTF-8 中文正常、时间为东八区
- 控制台中文正常显示
- 次日滚动生成 `server.2026-08-10.0.log`

## 影响面

纯配置改动，无 Java 代码变更；不影响任何接口行为。
