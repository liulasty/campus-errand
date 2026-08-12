# 控制器路由重规划 + 目录整治 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `com.lz.controller` 从 21 个混装控制器重构为 29 个按业务域组织的控制器，URL 统一 kebab-case，管理员接口收敛到 `/admin/**`，并同步前端 API 层与安全白名单。

**Architecture:** 纯机械式重排：方法体原样迁移，只改 package/import、类级 `@RequestMapping`、方法级 `@RequestMapping`、`@Api` 标签。HTTP 动词与请求体结构不变。前端仅改 `web/src/api/index.js` + `web/src/api/user.js` 的 URL 字符串。安全白名单同步更新。设计源文件见 `docs/superpowers/specs/2026-08-12-controller-route-reorg-design.md`（URL 映射表权威）。

**Tech Stack:** Spring Boot 2.7.3 / Java 8 target / Maven 3.9.16（本机编译入口）/ Vue 2 前端。

**环境要点（必读）：**
- 编译：`D:\soft-tools\apache-maven-3.9.16\bin\mvn.cmd compile`（用 3.9.16，3.6.3 在 JDK21 下报 Lombok 找不到符号）。
- 编译工作目录：仓库根（含 pom.xml）。
- 前端构建：`cd web` 后 `npm run build`；`cd web` 后工作目录会停在 web/，回根目录要再 `cd ..` 或使用绝对路径。
- 后端测试按分工交给 Business Validator，本计划只做 `mvn compile` 编译验证 + grep 路由验证，**不跑测**。
- 每个 Task 结束后必须 `git add` 具体文件并 commit（消息风格参考近期提交，如 `refactor: ...`）。

---

### Task 0: 基线验证

**Files:**
- Verify only（无改动）

- [ ] **Step 1: 确认基线可编译**

```bash
D:\soft-tools\apache-maven-3.9.16\bin\mvn.cmd compile
```

Expected: `BUILD SUCCESS`（改动前基线）。

- [ ] **Step 2: 确认当前 git 状态**

```bash
git status --short
```

Expected: 仅 `M web/src/views/admin/UserList.vue`（既有未提交改动，勿动；所有提交只 add 本计划涉及的文件，不要 `git add .`）。

---

### Task 1: user/ 身份域（5 类新建，2 类删除）

**Files:**
- Create: `src/main/java/com/lz/controller/user/AuthController.java`
- Create: `src/main/java/com/lz/controller/user/UserProfileController.java`
- Create: `src/main/java/com/lz/controller/user/AuthenticationController.java`
- Create: `src/main/java/com/lz/controller/user/admin/UserAdminController.java`
- Create: `src/main/java/com/lz/controller/user/admin/AuthenticationAdminController.java`
- Delete: `src/main/java/com/lz/controller/UsersController.java`
- Delete: `src/main/java/com/lz/controller/UsersinfoController.java`

- [ ] **Step 1: 创建 AuthController**（从 UsersController 迁移 login/register/logout/check，方法体原样）

`src/main/java/com/lz/controller/user/AuthController.java`：
```java
package com.lz.controller.user;

// 保留 UsersController 中 login/register/logout/check 四个方法所需的所有 import（AuthenticationManager、JwtUtil、JwtTokenBlacklist、AppConfig、UserLoginDTO、UserDTO、NoReturnHandle 等）
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Api(tags = "认证接口")
@Slf4j
public class AuthController {
    // 依赖注入与构造函数原样迁移（IUsersService、IUsersInfoService、AppConfig、JwtTokenBlacklist、AuthenticationManager）
    @PostMapping("/login")   // 原 /user/login
    @NoReturnHandle
    public Result<UserLoginVO> login(...) { /* 方法体原样 */ }
    @PostMapping("/register") // 原 /user/register
    public Result<String> register(...) { /* 原样 */ }
    @DeleteMapping("/logout") // 原 /user/logout
    public Result<String> logout(...) { /* 原样 + 私有 tryLogout 一并迁移 */ }
    @GetMapping("/check")    // 原 /user/check
    public Result<String> check(...) { /* 原样 */ }
}
```

- [ ] **Step 2: 创建 UserProfileController**（迁移 getUserInfo/updateUserInfo/editPassword）

`src/main/java/com/lz/controller/user/UserProfileController.java`：
```java
package com.lz.controller.user;
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*", allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Api(tags = "用户自助接口")
@Slf4j
public class UserProfileController {
    // 注入 IUsersService、IUsersInfoService 等（按迁移方法需要）
    @GetMapping("/profile/{id}")   // 原 /user/getUserInfo/{id}
    public Result<Users> getUserInfo(...) { /* 原样 */ }
    @PutMapping("/profile")        // 原 /user/updateUserInfo
    public Result<String> updateUserInfo(...) { /* 原样 */ }
    @PutMapping("/password")       // 原 /user/editPassword
    public Result<String> editPassword(...) { /* 原样 */ }
}
```

- [ ] **Step 3: 创建 AuthenticationController**（从 UsersinfoController 迁移 GET/POST 实名 + cancel）

`src/main/java/com/lz/controller/user/AuthenticationController.java`：
```java
package com.lz.controller.user;
@RestController
@RequestMapping("/authentications")
@CrossOrigin(origins = "*", allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Api(tags = "实名认证接口")
@Slf4j
public class AuthenticationController {
    // 注入 IUsersInfoService、IUsersService、ITaskService（按需）
    @GetMapping("/{id}")                       // 原 /userInfo/{id}
    public Result<UsersInfo> userInfo(...) { /* 原样（含 D-16 掩码逻辑 + 私有 getCurrentUser） */ }
    @PostMapping                              // 原 POST /userInfo
    public Result<String> save(...) { /* 原样 */ }
    @PutMapping("/{id}/cancel")                // 原 /userInfo/cancelUserInfoAuthentication/{id}
    public Result<String> cancelUserInfoAuthentication(...) { /* 原样 */ }
}
```

- [ ] **Step 4: 创建 user/admin/UserAdminController**（从 UsersController 迁移管理员方法）

`src/main/java/com/lz/controller/user/admin/UserAdminController.java`：
```java
package com.lz.controller.user.admin;
@RestController
@RequestMapping("/admin/users")
@CrossOrigin(origins = "*", allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Api(tags = "用户管理接口")
@Slf4j
public class UserAdminController {
    // 注入 IUsersService、IUsersInfoService
    @GetMapping                            // 原 /user/page（pageNum/pageSize/authStatus/username/email/isActive）
    public Result<PageResult> getUserInfoByPage(...) { /* 原样 */ }
    @GetMapping("/export")                 // 原 /user/exportExcel
    @NoReturnHandle
    public void exportExcel(...) { /* 原样 */ }
    @PutMapping                            // 原 /user/updateUserInfoByAdmin（@PreAuthorize 保留）
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<String> updateUserInfoByAdmin(...) { /* 原样 */ }
    @DeleteMapping("/{id}")                // 原 /user/deleteUserInfoById/{id}
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<String> deleteUserInfoById(...) { /* 原样 */ }
    @PutMapping("/{id}/activate")          // 原 /user/adminActivation/{id}
    public Result<String> adminActivation(...) { /* 原样 */ }
    @PutMapping("/{id}/disable")           // 原 /user/handleDisableByAdmin/{id}
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<String> disableUserByAdmin(...) { /* 原样 */ }
    @PutMapping("/{id}/enable")            // 原 /user/handleEnableByAdmin/{id}
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<String> cancelDisableUserByAdmin(...) { /* 原样 */ }
    @PostMapping("/{id}/reset-password")   // 原 /user/resetPassword
    public Result<String> resetPassword(...) { /* 原样 */ }
    @DeleteMapping                         // 原 POST /user/deleteUser（body int[] ids；@PreAuthorize 保留）
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<String> deleteAccounts(...) { /* 原样 */ }
}
```

> **注意**：原 `/user/deleteUserInfoByAdmin`（纯 removeByIds，无前端调用、被 deleteUser 覆盖）→ **不迁移，删除**。

- [ ] **Step 5: 创建 user/admin/AuthenticationAdminController**（从 UsersinfoController 迁移审核方法）

`src/main/java/com/lz/controller/user/admin/AuthenticationAdminController.java`：
```java
package com.lz.controller.user.admin;
@RestController
@RequestMapping("/admin/authentications")
@CrossOrigin(origins = "*", allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Api(tags = "实名审核接口")
@Slf4j
public class AuthenticationAdminController {
    // 注入 IUsersInfoService、ITaskService
    @PutMapping("/{id}/approve")   // 原 /userInfo/confirmToPassTheReview/{id}
    public Result<String> confirmToPassTheReview(...) { /* 原样 */ }
    @PutMapping("/{id}/reject")    // 原 /userInfo/refuseToPassReview/{id}
    public Result<String> refuseToPassReview(...) { /* 原样 */ }
    @DeleteMapping("/{id}")        // 原 DELETE /userInfo/{id}
    public Result<String> delete(...) { /* 原样（含任务校验） */ }
}
```

- [ ] **Step 6: 删除旧类并编译**

```bash
git rm src/main/java/com/lz/controller/UsersController.java src/main/java/com/lz/controller/UsersinfoController.java
D:\soft-tools\apache-maven-3.9.16\bin\mvn.cmd compile
```

Expected: `BUILD SUCCESS`。若有 import 残留报错（如其它文件引用这两个类），逐一排查修正。

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/lz/controller/user/
git commit -m "refactor: 身份域控制器按业务域重组（auth/user/authentications + admin 审核）"
```

---

### Task 2: task/ 委托域（11 类新建，8 类删除）

**Files:**
- Create: `src/main/java/com/lz/controller/task/TaskController.java`
- Create: `src/main/java/com/lz/controller/task/TaskHallController.java`
- Create: `src/main/java/com/lz/controller/task/AcceptController.java`
- Create: `src/main/java/com/lz/controller/task/PublisherController.java`
- Create: `src/main/java/com/lz/controller/task/TaskUpdateController.java`
- Create: `src/main/java/com/lz/controller/task/ReviewController.java`
- Create: `src/main/java/com/lz/controller/task/CategoryController.java`
- Create: `src/main/java/com/lz/controller/task/admin/TaskAdminController.java`
- Create: `src/main/java/com/lz/controller/task/admin/TaskUpdateAdminController.java`
- Create: `src/main/java/com/lz/controller/task/admin/ReviewAdminController.java`
- Create: `src/main/java/com/lz/controller/task/admin/CategoryAdminController.java`
- Delete: `src/main/java/com/lz/controller/TaskController.java`
- Delete: `src/main/java/com/lz/controller/TaskUpdatesController.java`
- Delete: `src/main/java/com/lz/controller/ReviewsController.java`
- Delete: `src/main/java/com/lz/controller/DelegationCategoriesController.java`
- Delete: `src/main/java/com/lz/controller/admin/TaskAdminController.java`
- Delete: `src/main/java/com/lz/controller/user/TaskUserController.java`
- Delete: `src/main/java/com/lz/controller/user/AcceptController.java`
- Delete: `src/main/java/com/lz/controller/user/PublisherController.java`

**TaskController（新 `com.lz.controller.task`，base `/tasks`）**：原 TaskController 迁入，**剔除** `getTaskCategory`（→ CategoryController）与 `searchPage`（→ TaskAdminController）。方法级映射：

| 动词 | 新 @RequestMapping | 原路径 | 说明 |
|---|---|---|---|
| POST | `/drafts` | /task/addTaskDraft | 创建草稿 |
| PUT | `/drafts` | /task/updateTaskDraft | 更新草稿 |
| DELETE | `/drafts/{id}` | /task/deleteTaskDraft/{id} | 删草稿 |
| GET | `/drafts`（params: userId） | /task/getUserDelegateDraft/{userId} | 草稿列表（`@PathVariable` 改 `@RequestParam("userId")`） |
| PUT | `/drafts/{id}/submit` | /task/auditTask/{id} | 提交审核 |
| GET | `/`（params: status） | POST /task/getTask | 按状态列表（`@RequestBody` 改 `@RequestParam status`，缺省 AUDITING） |
| GET | `/{id}` | /task/getTask/{id} | 详情 |
| GET | `/newest/{userId}` | /task/getNewTask/{id} | 首页最新（参数语义是 userId，改名为 userId） |
| DELETE | `/{id}` | POST /task/deleteTask | 删除委托（`@RequestBody` 改 `@PathVariable`） |
| GET | `/{id}/confirm` | /task/confirmTask/{id} | 获取待发布委托 |
| PUT | `/{id}/cancel` | /task/cancelTaskByUser/{id} | 取消发布 |
| POST | `/audit-result` | /task/auditResult | 提交审核结果（body AuditResultDTO 含 delegateId，路径不带 id，契约不变） |
| GET | `/{id}/audit-reason` | /task/getReason/{id} | 驳回原因 |
| GET | `/{id}/status` | /task/getTaskStatus/{id} | 状态 |

> `auditResult` 的原 body 是 `AuditResultDTO`（含 delegateId）。迁移时若 `@PathVariable("id")` 与 body 冲突，**保留 body 结构、路径用 `/audit-result`（无 id 段）**，即 `POST /tasks/audit-result`，避免契约改动。以实际 DTO 字段为准。

**TaskHallController（原 TaskUserController，base `/tasks/hall`）**：
| GET | `/` | /user/task/page | 大厅分页 |
| GET | `/{id}` | /user/task/{id} | 大厅详情 |
| GET | `/categories` | /user/task/categories | 大厅分类 |

**AcceptController（原 user/AcceptController，base `/tasks/accepts`）**：
| GET | `/{id}` | /user/accept/{id} | 接单记录 |
| POST | `/` | /user/accept | 接单 |
| GET | `/` | /user/accept/page | 我的接单列表 |
| PUT | `/{id}/cancel` | /user/accept/cancel/{id} | 取消接单 |

**PublisherController（原 user/PublisherController，base `/tasks/publisher`）**：
| GET | `/{id}` | /user/publisher/{id} | 发布者公开资料 |
| GET | `/tasks/{id}` | /user/publisher/getTask/{id} | 委托详情（发布者视角） |
| GET | `/tasks` | /user/publisher/page | 我的发布列表 |
| PUT | `/tasks/{id}/publish` | /user/publisher/confirmTask/{id} | 发布委托 |
| PUT | `/accepts/{id}/confirm` | /user/publisher/confirm/{id} | 确认接单者 |
| PUT | `/tasks/{id}/cancel-publish` | /user/publisher/cancel/{id} | 取消发布 |
| PUT | `/tasks/{id}/completed` | /user/publisher/completed/{id} | 标记完成 |
| DELETE | `/tasks/{id}` | /user/publisher/{id} | 删除委托 |

**TaskUpdateController（用户侧，base `/tasks/updates`）**：
| POST | `/` | /taskUpdate/add | 添加进度更新 |
| POST | `/node` | /taskUpdate/node | 节点打卡 |
| GET | `/`（params: taskId） | /taskUpdate/getTask/{id} | 查看委托动态（`@PathVariable` 改 `@RequestParam`） |

**ReviewController（base `/reviews`）**：
| POST | `/` | /reviews/addReviews | 提交评价 |

**CategoryController（用户向选项，base `/categories`）**：
| GET | `/options` | /task/getTaskCategory | 分类选项（NameAndDescription） |

**task/admin/TaskAdminController（base `/admin/tasks`）**：原 admin/TaskAdminController 迁入 + 并入 `searchPage`。方法级映射：
| GET | `/` | /admin/task/list | 委托列表 |
| GET | `/export` | /admin/task/exportExcel | 导出 |
| GET | `/{id}` | /admin/task/{TaskID} | 详情 |
| DELETE | `/{id}` | /admin/task/{TaskID} | 删除 |
| PUT | `/{id}/fallback-draft` | /admin/task/getFallbackDraft/{TaskID} | 回退草稿 |
| PUT | `/{id}/allow-publish` | /admin/task/allowPublish/{TaskID} | 允许发布 |
| PUT | `/{id}/reject-publish` | /admin/task/notAllowed/{TaskID} | 拒绝发布 |
| PUT | `/{id}/enable` | /admin/task/handleEnableAdmin/{id} | 启用 |
| PUT | `/{id}/disable` | /admin/task/handleDisableAdmin/{id} | 禁用 |
| PUT | `/{id}/withdraw-release` | /admin/task/withdrawReleaseByTaskID/{id} | 撤回发布 |
| POST | `/search` | /task/searchPage | 审核/通用分页（从原 TaskController 迁入） |

**task/admin/TaskUpdateAdminController（base `/admin/tasks/updates`）**：
| GET | `/` | /taskUpdate/list | 分页动态列表 |
| GET | `/types` | /taskUpdate/type | 动态类型 |
| DELETE | `/{id}` | /taskUpdate/{id} | 删除动态 |

**task/admin/ReviewAdminController（base `/admin/reviews`）**：
| GET | `/export` | /reviews/exportExcel | 导出（@PreAuthorize 保留） |
| DELETE | `/` | /reviews/clear | 清空（@PreAuthorize 保留） |

**task/admin/CategoryAdminController（base `/admin/categories`）**：
| GET | `/` | /delegation_categories/list | 分类列表 |
| GET | `/{id}` | /delegation_categories/{id} | 分类详情 |
| POST | `/` | /delegation_categories | 新增 |
| PUT | `/` | /delegation_categories | 修改 |
| DELETE | `/{id}` | /delegation_categories/{id} | 删除 |
| PUT | `/{id}/enable` | /delegation_categories/enable/{id} | 启用/停用 |

- [ ] **Step 1–11: 按上表创建 11 个新类**

每个类：`package com.lz.controller.task[.admin];` + 原有 import（缺啥补啥）+ 类级注解（`@RestController`/`@RequestMapping`/`@CrossOrigin`/`@Api(tags=域中文名)`/`@Slf4j`）+ 方法体原样 + 新 `@RequestMapping`。依赖注入按迁移方法需要的服务迁移。

- [ ] **Step 12: 删除 8 个旧类并编译**

```bash
git rm src/main/java/com/lz/controller/TaskController.java \
  src/main/java/com/lz/controller/TaskUpdatesController.java \
  src/main/java/com/lz/controller/ReviewsController.java \
  src/main/java/com/lz/controller/DelegationCategoriesController.java \
  src/main/java/com/lz/controller/admin/TaskAdminController.java \
  src/main/java/com/lz/controller/user/TaskUserController.java \
  src/main/java/com/lz/controller/user/AcceptController.java \
  src/main/java/com/lz/controller/user/PublisherController.java
D:\soft-tools\apache-maven-3.9.16\bin\mvn.cmd compile
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 13: Commit**

```bash
git add src/main/java/com/lz/controller/task/
git commit -m "refactor: 委托域控制器重组（task/hall/accepts/publisher/updates/review/category + admin 子包）"
```

---

### Task 3: notice/ 通知公告域（5 类新建，3 类删除）

**Files:**
- Create: `src/main/java/com/lz/controller/notice/NotificationController.java`
- Create: `src/main/java/com/lz/controller/notice/AnnouncementController.java`
- Create: `src/main/java/com/lz/controller/notice/admin/NotificationAdminController.java`
- Create: `src/main/java/com/lz/controller/notice/admin/AnnouncementAdminController.java`
- Create: `src/main/java/com/lz/controller/notice/admin/ReadStatusAdminController.java`
- Delete: `src/main/java/com/lz/controller/NotificationsController.java`
- Delete: `src/main/java/com/lz/controller/SystemAnnouncementsController.java`
- Delete: `src/main/java/com/lz/controller/NotificationReadStatusController.java`

**NotificationController（读 + 用户侧，base `/notifications`）**：
| GET | `/` | /notifications/list | 分页列表 |
| GET | `/types` | /notifications/type | 类型枚举 |
| GET | `/{id}` | /notifications/{id} | 通知详情 |
| GET | `/info/{id}` | /notifications/info/{id} | 消息详情 VO |
| GET | `/by-type/{str}` | /notifications/getList/{str} | 按类型查 |
| GET | `/my` | /notifications/my | 消息中心 |
| PUT | `/{id}/read` | /notifications/read/{id} | 标记已读 |

**notice/admin/NotificationAdminController（base `/admin/notifications`）**：
| POST | `/` | POST /notifications | 新增 |
| PUT | `/` | PUT /notifications | 修改 |
| POST | `/send` | /notifications/send | 发送 |
| DELETE | `/{id}` | /notifications/{id} | 删除 |

**AnnouncementController（base `/announcements`）**：
| GET | `/` | /system-announcements/list | 公告列表 |
| GET | `/{id}` | /system-announcements/{id} | 公告详情 |

**notice/admin/AnnouncementAdminController（base `/admin/announcements`）**：
| POST | `/` | POST /system-announcements | 新增（@PreAuthorize 保留） |
| PUT | `/` | PUT /system-announcements | 修改（@PreAuthorize 保留） |
| DELETE | `/{id}` | /system-announcements/{id} | 删除 |

**notice/admin/ReadStatusAdminController（base `/admin/notification-read-status`）**：
| GET | `/` | /notificationReadStatus/list | 读取记录 |
| DELETE | `/{id}` | /notificationReadStatus/{id} | 删除记录 |

- [ ] **Step 1–5: 创建 5 类**（方法体原样，按上表改注解）
- [ ] **Step 6: 删除 3 旧类并编译**（`git rm` + `mvn compile`，Expected `BUILD SUCCESS`）
- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/lz/controller/notice/
git commit -m "refactor: 通知公告域控制器重组（notification/announcement/read-status + admin 写操作）"
```

---

### Task 4: system/ 系统域（4 类新建，3 类删除）

**Files:**
- Create: `src/main/java/com/lz/controller/system/SensitiveWordController.java`
- Create: `src/main/java/com/lz/controller/system/StatsController.java`
- Create: `src/main/java/com/lz/controller/system/admin/SettingsAdminController.java`
- Create: `src/main/java/com/lz/controller/system/admin/SensitiveWordAdminController.java`
- Delete: `src/main/java/com/lz/controller/SensitiveWordController.java`
- Delete: `src/main/java/com/lz/controller/StatsController.java`
- Delete: `src/main/java/com/lz/controller/AdminsettingsController.java`

**SensitiveWordController（仅 check，base `/sensitive-words`）**：
| POST | `/check` | /sensitive/check | 校验文本 |

**system/admin/SensitiveWordAdminController（base `/admin/sensitive-words`）**：
| GET | `/` | /sensitive/words | 列表 |
| POST | `/` | /sensitive/words | 新增 |
| DELETE | `/{id}` | /sensitive/words/{id} | 删除 |

**StatsController（base `/stats`）**：GET `/`（原 `/stats`），方法体原样。

**system/admin/SettingsAdminController（base `/admin/settings`）**：
| GET | `/` | GET /adminsettings | 设置列表 |
| POST | `/` | POST /adminsettings | 新增（noop save 原样保留） |
| GET | `/current` | /adminsettings/enable | 当前状态 |
| PUT | `/` | /adminsettings/update | 更新开关 |

- [ ] **Step 1–4: 创建 4 类**（方法体原样）
- [ ] **Step 5: 删除 3 旧类并编译**（`git rm` + `mvn compile`，Expected `BUILD SUCCESS`）
- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/lz/controller/system/
git commit -m "refactor: 系统域控制器重组（sensitive-word/stats/settings + admin 子包）"
```

---

### Task 5: credit/file/common/error 域（4 类新建，4 类删除）

**Files:**
- Create: `src/main/java/com/lz/controller/credit/CreditController.java`
- Create: `src/main/java/com/lz/controller/file/FileController.java`
- Create: `src/main/java/com/lz/controller/common/CommonController.java`
- Create: `src/main/java/com/lz/controller/error/MyErrorController.java`
- Delete: `src/main/java/com/lz/controller/CreditController.java`
- Delete: `src/main/java/com/lz/controller/ImgController.java`
- Delete: `src/main/java/com/lz/controller/user/CommonController.java`
- Delete: `src/main/java/com/lz/controller/MyErrorController.java`

**CreditController（base `/credit`）**：GET `/`，方法体原样。
**FileController（base `/files`）**：
| POST | `/images` | /img/upload | 上传图片 |
| POST | `/images/avatar` | /img/uploadAvatar | 上传头像 |
| DELETE | `/images` | /img/delete | 删除图片 |
**CommonController（base `/common`）**：
| GET | `/ip` | /common/getUserIp | 获取 IP |
**error/MyErrorController（`/error`）**：类级 `@RequestMapping` 去掉（保持 `@RestController + implements ErrorController`，`@RequestMapping("/error")` 在方法上原样）。

- [ ] **Step 1–4: 创建 4 类**
- [ ] **Step 5: 删除 4 旧类并编译**（`git rm` + `mvn compile`，Expected `BUILD SUCCESS`）
- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/lz/controller/credit/ src/main/java/com/lz/controller/file/ \
  src/main/java/com/lz/controller/common/ src/main/java/com/lz/controller/error/
git commit -m "refactor: 信用/文件/通用/错误控制器迁移至业务域包"
```

---

### Task 6: 前端 API 层同步（2 文件）

**Files:**
- Modify: `web/src/api/index.js`
- Modify: `web/src/api/user.js`

按 spec 映射表替换 URL 字符串。**只改 URL 字符串**，不改函数签名与调用点。逐行核对：

**`web/src/api/index.js`** 替换清单：
| 函数 | 旧 URL | 新 URL |
|---|---|---|
| getData | `/task/getNewTask/` | `/tasks/newest/` |
| adminActivation | `/user/adminActivation/` | `/admin/users/`+id+`/activate`（`http.put('/admin/users/' + id + '/activate')`） |
| deleteAccounts | `http.post('/user/deleteUser', data)` | `http.delete('/admin/users', { data })` |
| getTaskCategories | `/task/getTaskCategory` | `/categories/options` |
| addTaskDraft | `/task/addTaskDraft` | `/tasks/drafts` |
| getTaskDraftById | `/task/getUserDelegateDraft/` | `/tasks/drafts`（`{ params: { userId: id } }`） |
| getDraftDetailsBasedOnCommissionId | `/task/getTask/` | `/tasks/` |
| updateTaskDraft | `http.post('/task/updateTaskDraft', data)` | `http.put('/tasks/drafts', data)` |
| deleteTaskDraft | `/task/deleteTaskDraft/` | `/tasks/drafts/` |
| submitTaskDraft | `/task/auditTask/` | `/tasks/drafts/`+id+`/submit` |
| confirmTask | `/task/confirmTask/` | `/tasks/`+id+`/confirm` |
| publishingDelegation | `/user/publisher/confirmTask/` | `/tasks/publisher/tasks/`+id+`/publish` |
| getReason | `/task/getReason/` | `/tasks/`+id+`/audit-reason` |
| getUserInfo | `/userInfo/` | `/authentications/` |
| fetchUserBasicInfo | `/user/getUserInfo/` | `/user/profile/` |
| uploadImg / updateImg | `/img/upload` | `/files/images` |
| submitCertificationInformation | `/userInfo` | `/authentications` |
| confirmToPassTheReview | `/userInfo/confirmToPassTheReview/` | `/admin/authentications/`+id+`/approve` |
| refuseToPassReview | `/userInfo/refuseToPassReview/` | `/admin/authentications/`+id+`/reject` |
| getUserList | `/user/page` | `/admin/users` |
| listDelegateRecords | `/admin/task/list` | `/admin/tasks` |
| listDelegateUpdateRecords | `/taskUpdate/list` | `/admin/tasks/updates` |
| getDelegateUpdateType | `/taskUpdate/type` | `/admin/tasks/updates/types` |
| getDelegateByTaskID | `/admin/task/` | `/admin/tasks/` |
| delDelegateUpdateRecords | `/taskUpdate/` | `/admin/tasks/updates/` |
| addTaskUpdate | `/taskUpdate/add` | `/tasks/updates` |
| addTaskNodeUpdate | `/taskUpdate/node` | `/tasks/updates/node` |
| deleteCertificationRecords / deleteAuthenticationInformation | `/userInfo/` | `/admin/authentications/` |
| cancelUserInfoAuthentication | `/userInfo/cancelUserInfoAuthentication/` | `/authentications/`+id+`/cancel` |
| delDelegate | `/admin/task/` | `/admin/tasks/` |
| FallbackDraft | `/admin/task/getFallbackDraft/` | `/admin/tasks/`+id+`/fallback-draft` |
| allowPublish | `/admin/task/allowPublish/` | `/admin/tasks/`+id+`/allow-publish` |
| notAllowed | `/admin/task/notAllowed/` | `/admin/tasks/`+id+`/reject-publish` |
| handleEnableAdmin | `/user/handleEnableByAdmin/` | `/admin/users/`+id+`/enable` |
| handleDisableAdmin | `/user/handleDisableByAdmin/` | `/admin/users/`+id+`/disable` |
| getViewDelegateRecord | `/taskUpdate/getTask/` | `/tasks/updates`（`{ params: { taskId: id } }`） |
| getSystemBulletinList | `/system-announcements/list` | `/announcements` |
| getSystemBulletinById / deleteSystemBulletin | `/system-announcements/` | `/announcements/` |
| updateSystemBulletin / createSystemBulletin | `/system-announcements` | `/admin/announcements` |
| listNotifications | `/notifications/list` | `/notifications` |
| getNotificationsType | `/notifications/type` | `/notifications/types` |
| addNotification / updateNotificationAdmin | `/notifications` | `/admin/notifications` |
| sendNotification | `/notifications/send` | `/admin/notifications/send` |
| delNotification | `/notifications/` | `/admin/notifications/` |
| listNotificationReadRecords | `/notificationReadStatus/list` | `/admin/notification-read-status` |
| getDelegationTypeList | `/delegation_categories/list` | `/admin/categories` |
| getDelegationTypeById | `/delegation_categories/` | `/admin/categories/` |
| updateDelegationTypeAdmin / addDelegationTypeAdmin | `/delegation_categories` | `/admin/categories` |
| deleteDelegationType | `/delegation_categories/` | `/admin/categories/` |
| enableDelegationType | `/delegation_categories/enable/` | `/admin/categories/`+id+`/enable` |
| withdrawReleaseByTaskIDAdmin | `/admin/task/withdrawReleaseByTaskID/` | `/admin/tasks/`+id+`/withdraw-release` |
| uploadAvatar | `/img/uploadAvatar` | `/files/images/avatar` |
| deleteImg | `/img/delete` | `/files/images` |
| login | `/user/login` | `/auth/login` |
| logout | `user/logout`（缺 `/`，顺带修） | `/auth/logout` |
| register | `/user/register` | `/auth/register` |
| exportExcel | `/reviews/exportExcel` | `/admin/reviews/export` |
| markNotificationRead | `/notifications/read/` | `/notifications/`+id+`/read` |
| addReview | `/reviews/addReviews` | POST `/reviews` |
| listSensitiveWords / addSensitiveWord | `/sensitive/words` | `/admin/sensitive-words` |
| deleteSensitiveWord | `/sensitive/words/` | `/admin/sensitive-words/` |
| checkSensitiveText | `/sensitive/check` | `/sensitive-words/check` |
| exportTaskList | `/admin/task/exportExcel` | `/admin/tasks/export` |
| exportUserList | `/user/exportExcel` | `/admin/users/export` |
| **getTaskList** | `/task/page` | **删除此函数**（后端无此路由，死函数） |

> 不变项：`/notifications/my`、`/notifications/info/`、`/notifications/{id}`（getNotificationById）、`/credit`、`/stats`、`getPersonalNoticeList` 的 `/notifications/getList/`（→ `/notifications/by-type/`）。

**`web/src/api/user.js`** 替换清单：
| 函数 | 旧 URL | 新 URL |
|---|---|---|
| listViewOnGoingList | `/user/task/page` | `/tasks/hall` |
| getTaskCategoriesUser | `/user/task/categories` | `/tasks/hall/categories` |
| queryTheEntrustmentDetailsByEntrustmentNumber | `/user/publisher/getTask/` | `/tasks/publisher/tasks/` |
| getTaskAndPublishUserInfoByTaskId | `/user/task/` | `/tasks/hall/` |
| acceptCommission | `/user/accept` | `/tasks/accepts` |
| publishDelegationList | `/user/publisher/page` | `/tasks/publisher/tasks` |
| getTaskAcceptById | `/user/accept/` | `/tasks/accepts/` |
| acceptDelegationList | `/user/accept/page` | `/tasks/accepts` |
| cancelAcceptorByAcceptor | `/user/accept/cancel/` | `/tasks/accepts/`+id+`/cancel` |
| confirmTheRecipient | `/user/publisher/confirm/` | `/tasks/publisher/accepts/`+id+`/confirm` |
| getPersonalNoticeList | `/notifications/getList/` | `/notifications/by-type/` |
| cancelPublishUser | `/user/publisher/cancel/` | `/tasks/publisher/tasks/`+id+`/cancel-publish` |
| updateDelegationCompleted | `/user/publisher/completed/` | `/tasks/publisher/tasks/`+id+`/completed` |

> 不变项：`getNoticeById` 的 `/notifications/info/`。

- [ ] **Step 1: 改 `web/src/api/index.js`**（逐行按清单替换）
- [ ] **Step 2: 改 `web/src/api/user.js`**
- [ ] **Step 3: 校验无旧路由残留**

```bash
cd web && grep -rn "/user/accept\|/user/publisher\|/user/task\|/task/\|/taskUpdate\|/delegation_categories\|/userInfo\|/img/\|/user/login\|/user/register\|/user/logout\|/user/exportExcel\|/notifications/\|/system-announcements\|/sensitive/words\|/reviews/" src/api/ || echo "CLEAN"
```

Expected: `CLEAN`（或仅命中注释/说明文字，无 URL 字符串）。

- [ ] **Step 4: 前端构建验证**

```bash
npm run build
```

Expected: `Compiled successfully` / 构建产物生成，无报错。

- [ ] **Step 5: Commit**

```bash
git add web/src/api/index.js web/src/api/user.js
git commit -m "refactor: 前端 API 层同步新路由（kebab-case + /admin 收敛）"
```

---

### Task 7: 安全白名单 + 死代码清理

**Files:**
- Modify: `src/main/java/com/lz/config/WebSecurityConfig.java`（AUTH_WHITELIST 数组，约 47-65 行）
- Delete: `src/main/java/com/lz/controller/DelegateAuditRecordsController.java`

- [ ] **Step 1: 更新 AUTH_WHITELIST**

把：
```java
PATH_SEPARATOR + "/user/login",  "/user/login",
"/user/register",
PATH_SEPARATOR + "/user/check",  "/user/check",
...
"/img/upload",  "/img/uploadAvatar",
"/user/logout",
```
改为：
```java
PATH_SEPARATOR + "/auth/login",  "/auth/login",
"/auth/register",
PATH_SEPARATOR + "/auth/check",  "/auth/check",
...
"/files/images",  "/files/images/avatar",
"/auth/logout",
```
（`/common/**`、`/user/logout` 之外的原有项保持不变；注意保留双形式：带/不带 `PATH_SEPARATOR`。）

- [ ] **Step 2: 删除死代码控制器**

```bash
git rm src/main/java/com/lz/controller/DelegateAuditRecordsController.java
```

- [ ] **Step 3: 编译**

```bash
D:\soft-tools\apache-maven-3.9.16\bin\mvn.cmd compile
```

Expected: `BUILD SUCCESS`。若 DelegateAuditRecords 有引用残留（如 service import），一并清理。

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/lz/config/WebSecurityConfig.java
git commit -m "refactor: 安全白名单同步新路由，删除全注释死代码控制器"
```

---

### Task 8: 全量验证

**Files:**
- Verify only

- [ ] **Step 1: 全后端编译**

```bash
D:\soft-tools\apache-maven-3.9.16\bin\mvn.cmd compile
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 旧路由全量清零（后端 + 前端）**

```bash
grep -rn "RequestMapping(\"/user/\|RequestMapping(\"/task\|RequestMapping(\"/userInfo\|/user/task\|/user/accept\|/user/publisher\|/delegation_categories\|/taskUpdate\|/system-announcements\|/notificationReadStatus\|/adminsettings\|/sensitive/words\|/reviews/exportExcel\|/img/" src/main/java web/src 2>/dev/null || echo "CLEAN"
```

Expected: `CLEAN`。

- [ ] **Step 3: 新路由抽样存在**

```bash
grep -rn "RequestMapping(\"/auth\|RequestMapping(\"/tasks\|RequestMapping(\"/admin/tasks\|RequestMapping(\"/admin/users\|RequestMapping(\"/authentications\|RequestMapping(\"/notifications\|RequestMapping(\"/admin/notifications\|RequestMapping(\"/announcements\|RequestMapping(\"/categories\|RequestMapping(\"/files" src/main/java/com/lz/controller | head -40
```

Expected: 命中所有新域 base 路径。

- [ ] **Step 4: 类数核对**

```bash
ls src/main/java/com/lz/controller/**/*.java src/main/java/com/lz/controller/*.java 2>/dev/null | wc -l
```

Expected: 29。

- [ ] **Step 5: 前端构建**

```bash
cd web && npm run build
```

Expected: 构建成功。

- [ ] **Step 6: 汇总改动交用户**

把 spec 的「URL 映射表」作为**验证器用例更新清单**输出给用户转交 Business Validator（`controller/` 层按新控制器名组织目录）。

---

## Self-Review 记录

- **Spec 覆盖**：映射表每条 old→new 均有对应 Task（Task1 身份域 / Task2 委托域 / Task3 通知域 / Task4 系统域 / Task5 其余域 / Task6 前端 / Task7 白名单死代码 / Task8 验证）。删除项（DelegateAuditRecords、getTaskList、deleteUserInfoByAdmin）在 Task7/Task6/Task1 落实。
- **占位符**：所有方法迁移标注「方法体原样」，新 `@RequestMapping` 逐条列出；无 TBD/TODO。
- **类型一致**：`auditResult` 路径冲突已注明二选一策略；`getNewTask` 参数改名 userId 已在两端一致；`getTaskDraftById`/`getViewDelegateRecord` 的 path→query 改动在前端与后端两处成对出现。
