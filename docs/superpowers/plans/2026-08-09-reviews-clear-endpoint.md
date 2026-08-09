# POST /reviews/clear 管理员清空接口实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 admin-only 的 `POST /reviews/clear` 接口，使 Business Validator 通过 DSL 自助、可重复、零侵入地复验「空 reviews 表 → 空 xlsx」契约（D-14 主路径）。

**Architecture:** 三层直改：Controller（路由+`@PreAuthorize` 鉴权）→ Service（`reviewsMapper.delete(null)` 全删）→ 返回删除行数。验证走 BV DSL 验收（TC-033 新增 `export_empty_contract` 场景）；执行方不预跑 BV 用例，仅 `mvn compile` 做构建校验，最终结论只认 report。

**Tech Stack:** Spring Boot 2.7.3 / mybatis-plus 3.4.3 / Spring Security 方法级鉴权 / EasyExcel

**设计文档:** `docs/superpowers/specs/2026-08-09-reviews-clear-endpoint-design.md`

---

### Task 1: Service 层加 `clearAll()`

**Files:**
- Modify: `src/main/java/com/lz/service/IReviewsService.java`
- Modify: `src/main/java/com/lz/service/impl/ReviewsServiceImpl.java`

- [ ] **Step 1: 在 `IReviewsService` 接口新增方法声明**

在接口内 `List<Reviews> exportExcel();` 之后追加：

```java
    /** 清空全部评价（测试支撑，仅管理员调用），返回删除行数 */
    int clearAll();
```

- [ ] **Step 2: 在 `ReviewsServiceImpl` 实现**

在 `exportExcel()` 方法之后追加：

```java
    @Override
    public int clearAll() {
        return reviewsMapper.delete(null);
    }
```

说明：`reviewsMapper.delete(null)` 是 mybatis-plus「无条件全删」惯用法（与现有 `selectList(null)` 同风格），返回 `int` 影响行数。

### Task 2: Controller 加 `POST /reviews/clear`

**Files:**
- Modify: `src/main/java/com/lz/controller/ReviewsController.java`

- [ ] **Step 1: 新增 import**

在现有 import 区追加两行：

```java
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
```

- [ ] **Step 2: 新增端点方法**

在 `exportExcel` 方法之后、类结束前追加：

```java
    @PostMapping("/clear")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ApiOperation("清空全部评价（测试支撑，仅管理员）")
    public Result<Integer> clearAll() {
        return Result.success(reviewsService.clearAll());
    }
```

类型说明：`clearAll()` 返回 `int`，编译器自动装箱为 `Integer`，命中 `Result.success(T data)` 泛型重载（`data` 位），**不会**误入 `success(String)` 的 `msg` 位（String 陷阱只影响字符串参数）。

### Task 3: 构建校验

- [ ] **Step 1: 仓库根执行编译**

```bash
mvn compile
```

期望：`BUILD SUCCESS`（无编译错误）。

### Task 4: 更新 TC-033 DSL 用例（新增空契约自助复验场景）

**Files:**
- Modify: `D:\workspace-dev\node\BusinessValidator\projects\campus-errand\cases\controller\export-contract\TC-033_导出文件契约.yaml`

- [ ] **Step 1: 新增 `export_empty_contract` 场景**

在 `export_xlsx_contract` 场景之后、`export_permission_boundary` 之前插入：

```yaml
  - name: export_empty_contract
    description: "空表契约自助复验：admin 调 POST /reviews/clear 清空评价，再导出 reviews 应得到空 xlsx（__contentType=xlsx），证明空数据不再返回 JSON-200"
    steps:
      - name: login_admin_ec
        actor: admin
        method: POST
        api: /user/login
        body:
          username: ${{actors.admin.username}}
          password: ${{actors.admin.password}}
      - name: clear_reviews
        actor: admin
        method: POST
        api: /reviews/clear
      - name: export_reviews_empty
        actor: admin
        method: GET
        api: /reviews/exportExcel
    assertions:
      - type: response
        target: clear_reviews
        expect:
          code: 1
      - type: response
        target: export_reviews_empty
        expect:
          __status: 200
          __contentType: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
      - type: database
        sql: "SELECT CAST(COUNT(*) AS SIGNED) AS cnt FROM reviews"
        expect:
          cnt: 0
```

说明：`data`（删除行数）不精确断言（首次=5、清空后再跑=0，变脆）；用 DB `cnt:0` 直接证明清空生效。

### Task 5: 部署后端 + 刷新 OpenAPI

- [ ] **Step 1: 重启后端**：用户从 IDEA 启动（或 `mvn spring-boot:run`），确认 80 端口监听、新端点生效。
- [ ] **Step 2: 从 BusinessValidator 仓库根重新拉取接口文档**

```bash
node projects/campus-errand/scripts/fetch-swagger.js --username zhangsan --password test123456
```

期望：`api/*.json` 重新生成，`api/reviews.json` 出现 `POST /reviews/clear` 条目。

### Task 6: 交 Business Validator 验收

- [ ] **Step 1: 跑 TC-033**：预期 `export_empty_contract` 绿（clear 返回 code:1、导出 `__contentType`=xlsx、reviews 表 0 行）；其余场景 `export_xlsx_contract` / `export_permission_boundary` / `export_data_source_ready` 不变仍绿。
- [ ] **Step 2: 收尾**：验收通过后，如无需保留空态，可用 `reviews_bak` 恢复（`INSERT INTO reviews SELECT * FROM reviews_bak`，由 Coding-Agent JDBC 脚本执行）。

---

## 已知约束与注意事项

- **非 admin 调用**：`@PreAuthorize` 方法级拒绝走 `GlobalControllerAdvice` → HTTP 200 + `code:0`（非 403），与现有行为一致，勿在 DSL 里断言 403。
- **副作用**：清空后信用分重算（`avgRatingByAcceptor`）读到空评分，被评用户信用分下降——测试支撑可接受，`reviews_bak` 可回滚。
- **不建配置门控**：常驻 API 面（设计已确认）。
