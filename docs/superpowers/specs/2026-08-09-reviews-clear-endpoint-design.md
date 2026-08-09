# 设计：POST /reviews/clear 管理员清空评价接口（测试支撑）

> 状态：已批准（2026-08-09）
> 关联缺陷：D-14 三个 Excel 导出接口空数据返回 JSON-200 违反文件契约

## 一、背景与动机

D-14 修复后，「空 reviews 表 → 空 xlsx 工作簿」是必须可复验的契约路径。但 Business Validator 零侵入（只读 SELECT，不写库），复验空表需要人为清空 reviews——此前由 Coding-Agent 用 JDBC 脚本手动完成，不可自助、不可重复。

新增一个 admin-only 清空接口，写进 DSL 场景/cleanup，使 TC-033 可随时自助复验空表契约：零侵入（BV 仍只发 HTTP + 只读 SELECT）且可重复。

## 二、关键决策（已与用户确认）

| 决策点 | 结论 | 理由 |
|---|---|---|
| 路由 | `POST /reviews/clear` | 语义明确，不重载集合 DELETE，避免与按 id 删除混淆 |
| 暴露方式 | 常驻 admin-only，**不做配置门控** | MVP 无生产部署；最简单；BV 直接可用 |
| 鉴权 | `@PreAuthorize("hasAuthority('ADMIN')")` | 与 UsersController / SystemAnnouncementsController 同款模式 |
| 返回 | `Result.success(deletedCount)` | 删除行数便于 DSL 断言（outputs/data） |
| 语义 | `DELETE FROM reviews`（全删） | 不动 `reviews_bak` 备份表 |

## 三、接口契约

- **路径**：`POST /campus_entrustment/reviews/clear`，无请求体
- **成功**：`HTTP 200`，`{code:1, data:<删除行数>}`
- **非 admin**：`HTTP 200` + `{code:0, errorCode:FORBIDDEN}` —— 方法级 `@PreAuthorize` 拒绝走 `GlobalControllerAdvice`，**非 403**（与现有行为一致，DSL 断言按 `code:0` 写）
- **未登录**：`HTTP 401`（filter chain authenticationEntryPoint）

## 四、实现落点

| 层 | 改动 |
|---|---|
| `IReviewsService` | 新增 `int clearAll();` |
| `ReviewsServiceImpl` | `clearAll()` → `reviewsMapper.delete(null)`（mybatis-plus 全删，返回影响行数） |
| `ReviewsController` | 新增 `@PostMapping("/clear")` + `@PreAuthorize("hasAuthority('ADMIN')")` + `@ApiOperation`，返回 `Result<Integer>` |

说明：`reviewsMapper.delete(null)` 是 mybatis-plus 的「无条件删除全部」惯用法，返回 `int`。

## 五、副作用（已核实）

- **信用分重算依赖 reviews**：`CreditScoreService.loadMetrics` 用 `reviewsMapper.avgRatingByAcceptor` 聚合评分均值。清空 reviews 后，后续任何 `recomputeAndSave` 触发（评价/接单确认/任务完成/访问 `/credit`）会读到空评分 → 被评用户信用分下降。
- 属测试支撑可接受副作用；`reviews_bak`（5 行，D-14 验证时备份）保留可回滚。
- 清除接口本身**不**提供恢复能力——恢复走 `reviews_bak`（JDBC 脚本），保持接口最小。

## 六、DSL 落地

TC-033（`cases/controller/export-contract/TC-033_导出文件契约.yaml`）新增场景 `export_empty_contract`，替代 Coding-Agent 手动 JDBC 清库：

```
admin 登录
→ POST /reviews/clear（断言 code:1）
→ GET /reviews/exportExcel（断言 __status:200 + __contentType: xlsx）
```

从此空表契约自助、可重复、零侵入复验。

## 七、交付物与验收

- 后端 3 处改动 + 重启
- Swagger/OpenAPI 刷新（`node projects/campus-errand/scripts/fetch-swagger.js`）
- TC-033 用例更新
- 验收：Business Validator 跑 TC-033（Coding-Agent 不预跑，结论只认 report）

## 八、已知取舍

- 常驻 API 面（非配置门控）——已确认接受
- reviews 清空后保持空态（符合「环境不清理」约定）；需要恢复走 `reviews_bak`
