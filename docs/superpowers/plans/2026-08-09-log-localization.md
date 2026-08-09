# 日志本地化配置实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为后端增加本地文件落盘 + Asia/Shanghai 时区 + UTF-8 编码的日志配置，修复 Windows 控制台中文乱码。

**Architecture:** 纯配置改动，不新增 XML。利用 Spring Boot 2.7 原生 `logging.*` 属性（`charset`/`pattern`/`file`/`logback.rollingpolicy`），Spring Boot 默认 Logback 自动生成 `SizeAndTimeBasedRollingPolicy`。`logging.level.*` 与 `group` 保留不动。

**Tech Stack:** Spring Boot 2.7.3 / Logback（默认）/ application.yml

参考设计：`docs/superpowers/specs/2026-08-09-log-localization-design.md`

---

### Task 1: 改造 `application.yml` logging 块

**Files:**
- Modify: `src/main/resources/application.yml:28-55`（logging 块）

- [ ] **Step 1: 确认当前 logging 块内容**

读取 `src/main/resources/application.yml`，确认第 28-55 行为当前 logging 块（含 `group`/`level`/`pattern.file` 及若干注释行）。

- [ ] **Step 2: 替换 logging 块**

用下方内容替换当前 logging 块。保留 `group` 与 `level` 原值不变；移除旧 `pattern.file` 与所有 `#` 注释行（`#logging:`、`#  config:`、`#    console:`、`#  file:`、`#  logback:` 及其子行）；新增 `charset`、`pattern.console/file`（带 `Asia/Shanghai`）、`file.name`、`logback.rollingpolicy`。

```yaml
logging:
  # 设置日志组
  group:
    # 自定义组名，设置当前组中所包含的包
    ebank: com.lz.controller
  level:
    com:
      lz:
        mapper: debug
        service: info
        controller: info
        config: debug
    root: INFO
    org.springframework.data.convert.CustomConversions: ERROR
    org.springframework.data.mongodb: info
    ebank: info
  charset:
    console: UTF-8
    file: UTF-8
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS, Asia/Shanghai} [%thread] %-6level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS, Asia/Shanghai} [%thread] %-6level %logger{36} - %msg%n"
  file:
    name: logs/server.log
  logback:
    rollingpolicy:
      file-name-pattern: logs/server.%d{yyyy-MM-dd}.%i.log
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 10GB
```

- [ ] **Step 3: 校验 YAML 语法**

用 IDEA 打开 `application.yml` 确认无红框、缩进正确（`logging` 下各级均为 2 空格缩进）。

- [ ] **Step 4: 提交**

```bash
git add src/main/resources/application.yml
git commit -m "feat: 日志本地化配置（UTF-8 编码 / Asia/Shanghai 时区 / logs 按天滚动 100MB 保留30天）"
```

### Task 2: `.gitignore` 排除日志目录

**Files:**
- Modify: `.gitignore:1-30`

- [ ] **Step 1: 新增 `logs/` 排除项**

在 `.gitignore` 的 `*.log` 一行（第 16 行）下方追加 `logs/`：

```text
*.log
logs/
```

- [ ] **Step 2: 提交**

```bash
git add .gitignore
git commit -m "chore: gitignore 排除 logs/ 日志目录"
```

### Task 3: 用户重启后验证日志落盘与时区/编码

**Files:**（只读验证，不改代码）

**由用户在 IDEA 重启后端**（用户已确认自行重启，重启即加载新日志配置）。

- [ ] **Step 1: 确认日志文件生成**

用户重启后，确认：
```bash
ls logs/
```
预期：存在 `logs/server.log`（首次启动自动创建 `logs/` 目录）。

- [ ] **Step 2: 确认文件内容编码与时区**

读取 `logs/server.log` 开头若干行，确认：
- 时间戳为东八区（当前本地时间）
- 中文内容显示正常（UTF-8，无乱码）

- [ ] **Step 3: 问题回退**

若验证发现问题（如文件未生成、时间不对、乱码），回到 Task 1 修正 `application.yml` 并重新提交，再请用户重启复验。

验证本身不改代码，无提交。

---

## 说明

- 无 Java 代码变更，无单测/集成测试（纯配置，TDD 不适用）。
- **后端重启由用户自行执行**（用户已确认），Coding-Agent 不代为启停，只负责改配置 + 提交。
- 控制台中文乱码的 IDEA 侧设置（`Run/Debug Configuration → Console → Encoding = UTF-8`）为一次性人工操作，不在代码计划内，见设计文档「控制台中文乱码」节。
- 本次为配置改动，无接口契约变化，不输出 DSL 用例。
