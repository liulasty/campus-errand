# 控制器路由重规划 + 目录整治 设计

> **文档状态**：设计已确认
> **日期**：2026-08-12
> **范围**：`src/main/java/com/lz/controller` 全部 21 个控制器 + 前端 API 层同步 + 安全白名单同步

---

## 一、背景与现状

控制器层存在三类问题，本次一并整治：

1. **路由命名混乱**：驼峰（`/taskUpdate`、`/notificationReadStatus`、`/userInfo`）、下划线（`/delegation_categories`）、连字符（`/system-announcements`）、小写粘连（`/adminsettings`）混用。
2. **目录与职责不对齐**：21 个类散在根目录（16）+ `user/`（4）+ `admin/`（1）。`UsersController` 混入登录注册 + 用户管理；`UsersinfoController` 混入实名提交 + 管理员审核；`NotificationsController`/`SystemAnnouncementsController`/`DelegationCategoriesController`/`ReviewsController`/`SensitiveWordController` 均混有用户向与管理员向接口。
3. **安全边界靠不住**：`WebSecurityConfig` 仅对 `/admin/**` 做 `ADMIN` 权限兜底，大量管理员接口挂在 `/user/...`、`/userInfo/...`、`/sensitive/...` 下，只依赖个别 `@PreAuthorize`（`AdminsettingsController` 甚至无注解）。

另有一处死代码：`DelegateAuditRecordsController` 全部方法被注释。

## 二、设计原则

1. **URL 一律 kebab-case 小写连字符**（对齐现有 `/system-announcements`）：`/tasks/accepts`、`/admin/notification-read-status`。
2. **按业务域分包**：8 个顶层业务域包，管理员控制器放 `<域>/admin/` 子包。
3. **管理员接口统一收敛到 `/admin/**` 前缀**：安全边界从「方法级注解」升级为「URL 前缀 + 注解」双保险。
4. **只改路径前缀/命名，不改 HTTP 动词与请求体结构**：前端仅改 URL 字符串，契约结构不变。
5. **拆 god-class、删死代码**。

## 三、目标目录结构（29 类）

```
com.lz.controller
├── user/                        身份域
│   ├── AuthController               /auth/*
│   ├── UserProfileController        /user/*
│   ├── AuthenticationController     /authentications/*
│   └── admin/
│       ├── UserAdminController             /admin/users/*
│       └── AuthenticationAdminController   /admin/authentications/*
├── task/                        委托域
│   ├── TaskController               /tasks/*
│   ├── TaskHallController           /tasks/hall/*
│   ├── AcceptController             /tasks/accepts/*
│   ├── PublisherController          /tasks/publisher/*
│   ├── TaskUpdateController         /tasks/updates/*
│   ├── ReviewController             /reviews/*
│   ├── CategoryController           /categories/*
│   └── admin/
│       ├── TaskAdminController           /admin/tasks/*
│       ├── TaskUpdateAdminController     /admin/tasks/updates/*
│       ├── ReviewAdminController         /admin/reviews/*
│       └── CategoryAdminController       /admin/categories/*
├── notice/                      通知公告域
│   ├── NotificationController       /notifications/*
│   ├── AnnouncementController       /announcements/*
│   └── admin/
│       ├── NotificationAdminController     /admin/notifications/*
│       ├── AnnouncementAdminController     /admin/announcements/*
│       └── ReadStatusAdminController       /admin/notification-read-status/*
├── system/                      系统域
│   ├── SensitiveWordController       /sensitive-words/*（仅 check）
│   ├── StatsController               /stats/*
│   └── admin/
│       ├── SettingsAdminController       /admin/settings/*
│       └── SensitiveWordAdminController  /admin/sensitive-words/*
├── credit/   CreditController   /credit/*
├── file/     FileController     /files/*
├── common/   CommonController   /common/*
└── error/    MyErrorController  /error
```

### 与批准草图的细化差异

- `AdminsettingsController`（纯管理员配置）→ `system/admin/SettingsAdminController`，`/admin/settings`。
- `SensitiveWordController` 的 CRUD 拆到 `SensitiveWordAdminController`（`/admin/sensitive-words`），`check` 留 `/sensitive-words/check`。
- 通知/公告/分类/评价/履约动态的写操作 → 各域 `admin` 子包 `*AdminController`；读操作留用户侧共享。
- `DelegateAuditRecordsController` → 删除。

## 四、完整 URL 映射表（old → new）

> 标注：`✓`=前端在调用；`✗`=前端无调用（保留路由，规划期核实去留）；`?`=规划期核实。

### user/ 身份域

**AuthController**（原 UsersController 登录注册部分）→ `/auth`
| 动词 | 旧路径 | 新路径 | 前端 |
|---|---|---|---|
| POST | /user/login | /auth/login | ✓ |
| POST | /user/register | /auth/register | ✓ |
| DELETE | /user/logout | /auth/logout | ✓ |
| GET | /user/check | /auth/check | ✗ |

**UserProfileController** → `/user`
| GET | /user/getUserInfo/{id} | /user/profile/{id} | ✓ |
| PUT | /user/updateUserInfo | /user/profile | ✗ 核实 |
| PUT | /user/editPassword | /user/password | ✗ 核实 |

**AuthenticationController**（原 UsersinfoController 用户向）→ `/authentications`
| GET | /userInfo/{id} | /authentications/{id} | ✓ |
| POST | /userInfo | /authentications | ✓ |
| PUT | /userInfo/cancelUserInfoAuthentication/{id} | /authentications/{id}/cancel | ✓ |

**user/admin/UserAdminController** → `/admin/users`
| GET | /user/page | /admin/users | ✓ |
| GET | /user/exportExcel | /admin/users/export | ✓ |
| PUT | /user/updateUserInfoByAdmin | /admin/users | ✗ 核实 |
| DELETE | /user/deleteUserInfoById/{id} | /admin/users/{id} | ✗ 核实 |
| ~~DELETE /user/deleteUserInfoByAdmin~~ | — | 已删除：无前端调用且被 deleteUser 覆盖 | — |
| PUT | /user/adminActivation/{id} | /admin/users/{id}/activate | ✓ |
| PUT | /user/handleDisableByAdmin/{id} | /admin/users/{id}/disable | ✓ |
| PUT | /user/handleEnableByAdmin/{id} | /admin/users/{id}/enable | ✓ |
| POST | /user/resetPassword | /admin/users/reset-password | ✗ 核实（body Users 携带 userId，路径不带 id） |
| POST | /user/deleteUser | DELETE /admin/users (body) | ✓ |

> ✅ 决策（2026-08-12 实施）：`deleteUserInfoByAdmin`（纯 `removeByIds`）无前端调用且被 `deleteUser`（带 USER 角色+实名记录校验的 `deleteUsers`）覆盖 → **删除**，`DELETE /admin/users` 仅保留 `deleteUser` 一个批量删除。

**user/admin/AuthenticationAdminController** → `/admin/authentications`
| PUT | /userInfo/confirmToPassTheReview/{id} | /admin/authentications/{id}/approve | ✓ |
| PUT | /userInfo/refuseToPassReview/{id} | /admin/authentications/{id}/reject | ✓ |
| DELETE | /userInfo/{id} | /admin/authentications/{id} | ✓ |

### task/ 委托域

**TaskController**（原 TaskController，剔除分类/审核分页）→ `/tasks`
| POST | /task/addTaskDraft | /tasks/drafts | ✓ |
| POST | /task/updateTaskDraft | PUT /tasks/drafts | ✓ |
| DELETE | /task/deleteTaskDraft/{id} | /tasks/drafts/{id} | ✓ |
| GET | /task/getUserDelegateDraft/{userId} | /tasks/drafts?userId={userId} | ✓ |
| PUT | /task/auditTask/{id} | /tasks/drafts/{id}/submit | ✓ |
| GET | /task/getTask/{id} | /tasks/{id} | ✓ |
| POST | /task/getTask | GET /tasks?status={status} | ✗ |
| GET | /task/getNewTask/{id} | /tasks/newest/{userId} | ✓ |
| POST | /task/deleteTask | DELETE /tasks/{id} | ✗ |
| GET | /task/confirmTask/{id} | /tasks/{id}/confirm | ✓ |
| PUT | /task/cancelTaskByUser/{id} | /tasks/{id}/cancel | ✗ 核实 |
| POST | /task/auditResult | /tasks/{id}/audit-result | ✗ 与 admin 审核重叠，核实 |
| GET | /task/getReason/{id} | /tasks/{id}/audit-reason | ✓ |
| GET | /task/getTaskStatus/{id} | /tasks/{id}/status | ✗ |

**TaskHallController**（原 TaskUserController）→ `/tasks/hall`
| GET | /user/task/page | /tasks/hall | ✓ |
| GET | /user/task/{id} | /tasks/hall/{id} | ✓ |
| GET | /user/task/categories | /tasks/hall/categories | ✓ |

**AcceptController**（原 user/AcceptController）→ `/tasks/accepts`
| GET | /user/accept/{id} | /tasks/accepts/{id} | ✓ |
| POST | /user/accept | /tasks/accepts | ✓ |
| GET | /user/accept/page | /tasks/accepts | ✓ |
| PUT | /user/accept/cancel/{id} | /tasks/accepts/{id}/cancel | ✓ |

**PublisherController**（原 user/PublisherController）→ `/tasks/publisher`
| GET | /user/publisher/{id} | /tasks/publisher/{id} | ? 核实 |
| GET | /user/publisher/getTask/{id} | /tasks/publisher/tasks/{id} | ✓ |
| GET | /user/publisher/page | /tasks/publisher/tasks | ✓ |
| PUT | /user/publisher/confirmTask/{id} | /tasks/publisher/tasks/{id}/publish | ✓ |
| PUT | /user/publisher/confirm/{id} | /tasks/publisher/accepts/{id}/confirm | ✓ |
| PUT | /user/publisher/cancel/{id} | /tasks/publisher/tasks/{id}/cancel-publish | ✓ |
| PUT | /user/publisher/completed/{id} | /tasks/publisher/tasks/{id}/completed | ✓ |
| DELETE | /user/publisher/{id} | /tasks/publisher/tasks/{id} | ? 核实 |

**TaskUpdateController**（用户侧）→ `/tasks/updates`
| POST | /taskUpdate/add | /tasks/updates | ✓ |
| POST | /taskUpdate/node | /tasks/updates/node | ✓ |
| GET | /taskUpdate/getTask/{id} | /tasks/updates/{id} | ✓（实为按 read_status 记录 id 查单条，非 taskId） |

**task/admin/TaskUpdateAdminController** → `/admin/tasks/updates`
| GET | /taskUpdate/list | /admin/tasks/updates | ✓ |
| GET | /taskUpdate/type | /admin/tasks/updates/types | ✓ |
| DELETE | /taskUpdate/{id} | /admin/tasks/updates/{id} | ✓ |

**ReviewController** → `/reviews`
| POST | /reviews/addReviews | POST /reviews | ✓ |

**task/admin/ReviewAdminController** → `/admin/reviews`
| GET | /reviews/exportExcel | /admin/reviews/export | ✓ |
| POST | /reviews/clear | DELETE /admin/reviews | ✗ 核实 |

**CategoryController**（用户向选项）→ `/categories`
| GET | /task/getTaskCategory | /categories/options | 分类选项 NameAndDescription（发布/管理页共用） | ✓ |

> 大厅分类选项（原 `/user/task/categories`）→ `/tasks/hall/categories`，归属 TaskHallController。

**task/admin/CategoryAdminController** → `/admin/categories`
| GET | /delegation_categories/list | /admin/categories | 分类列表（管理端 DelegationType.vue） | ✓ |
| GET | /delegation_categories/{id} | /admin/categories/{id} | 分类详情（管理端） | ✓ |
| POST | /delegation_categories | /admin/categories | 新增分类 | ✓ |
| PUT | /delegation_categories | /admin/categories | 修改分类 | ✓ |
| DELETE | /delegation_categories/{id} | /admin/categories/{id} | 删除分类 | ✓ |
| PUT | /delegation_categories/enable/{id} | /admin/categories/{id}/enable | 启用/停用 | ✓ |

**task/admin/TaskAdminController** → `/admin/tasks`
| GET | /admin/task/list | /admin/tasks | ✓ |
| GET | /admin/task/exportExcel | /admin/tasks/export | ✓ |
| GET | /admin/task/{TaskID} | /admin/tasks/{id} | ✓ |
| DELETE | /admin/task/{TaskID} | /admin/tasks/{id} | ✓ |
| PUT | /admin/task/getFallbackDraft/{TaskID} | /admin/tasks/{id}/fallback-draft | ✓ |
| PUT | /admin/task/allowPublish/{TaskID} | /admin/tasks/{id}/allow-publish | ✓ |
| PUT | /admin/task/notAllowed/{TaskID} | /admin/tasks/{id}/reject-publish | ✓ |
| PUT | /admin/task/handleEnableAdmin/{id} | /admin/tasks/{id}/enable | ✗ 核实 |
| PUT | /admin/task/handleDisableAdmin/{id} | /admin/tasks/{id}/disable | ✗ 核实 |
| PUT | /admin/task/withdrawReleaseByTaskID/{id} | /admin/tasks/{id}/withdraw-release | ✓ |
| POST | /task/searchPage | /admin/tasks/search | ✗（validator TC-014） |

### notice/ 通知公告域

**NotificationController**（读 + 用户侧）→ `/notifications`
| GET | /notifications/list | /notifications | ✓ |
| GET | /notifications/type | /notifications/types | ✓ |
| GET | /notifications/{id} | /notifications/{id} | ✓ |
| GET | /notifications/info/{id} | /notifications/info/{id} | ✓ |
| GET | /notifications/getList/{str} | /notifications/by-type/{str} | ✓ |
| GET | /notifications/my | /notifications/my | ✓ |
| PUT | /notifications/read/{id} | /notifications/{id}/read | ✓ |

**notice/admin/NotificationAdminController** → `/admin/notifications`
| POST | /notifications | /admin/notifications | ✓ |
| PUT | /notifications | /admin/notifications | ✓ |
| POST | /notifications/send | /admin/notifications/send | ✓ |
| DELETE | /notifications/{id} | /admin/notifications/{id} | ✓ |

**AnnouncementController** → `/announcements`
| GET | /system-announcements/list | /announcements | ✓ |
| GET | /system-announcements/{id} | /announcements/{id} | ✓ |

**notice/admin/AnnouncementAdminController** → `/admin/announcements`
| POST | /system-announcements | /admin/announcements | ✓ |
| PUT | /system-announcements | /admin/announcements | ✓ |
| DELETE | /system-announcements/{id} | /admin/announcements/{id} | ✓ |

**notice/admin/ReadStatusAdminController** → `/admin/notification-read-status`
| GET | /notificationReadStatus/list | /admin/notification-read-status | ✓ |
| DELETE | /notificationReadStatus/{id} | /admin/notification-read-status/{id} | ✗ |

### system/ 系统域

**SensitiveWordController**（仅 check）→ `/sensitive-words`
| POST | /sensitive/check | /sensitive-words/check | ✓ |

**system/admin/SensitiveWordAdminController** → `/admin/sensitive-words`
| GET | /sensitive/words | /admin/sensitive-words | ✓ |
| POST | /sensitive/words | /admin/sensitive-words | ✓ |
| DELETE | /sensitive/words/{id} | /admin/sensitive-words/{id} | ✓ |

**system/admin/SettingsAdminController** → `/admin/settings`
| GET | /adminsettings | /admin/settings | ✗（validator admin-settings） |
| POST | /adminsettings | /admin/settings | — |
| GET | /adminsettings/enable | /admin/settings/current | — |
| PUT | /adminsettings/update | /admin/settings | — |

**StatsController** → `/stats`
| GET | /stats | /stats | ✓ |

### 其余

| 动词 | 旧路径 | 新路径 | 前端 |
|---|---|---|---|
| GET | /credit | /credit | ✓ |
| POST | /img/upload | /files/images | ✓ |
| POST | /img/uploadAvatar | /files/images/avatar | ✓ |
| DELETE | /img/delete | /files/images | ✓ |
| GET | /common/getUserIp | /common/ip | ✗ |
| 任意 | /error | /error | — |

### 删除项

- `DelegateAuditRecordsController`（全注释死代码）。
- 前端 `api/index.js` 的 `getTaskList`（调 `/task/page`，后端无此路由 → 死函数）。

## 五、类拆分/合并/删除清单

| 原类 | 处置 | 目标类 |
|---|---|---|
| UsersController | 拆 3 | AuthController + UserProfileController + admin/UserAdminController |
| UsersinfoController | 拆 2 | AuthenticationController + admin/AuthenticationAdminController |
| TaskController | 部分迁移 | TaskController（删分类/getTaskCategory→Category、searchPage→TaskAdmin）+ getTaskCategory 迁 CategoryController |
| TaskUserController | 更名 | TaskHallController |
| AcceptController | 迁移 | task/AcceptController |
| PublisherController | 迁移 | task/PublisherController |
| TaskUpdatesController | 拆 2 | task/TaskUpdateController + task/admin/TaskUpdateAdminController |
| ReviewsController | 拆 2 | task/ReviewController + task/admin/ReviewAdminController |
| DelegationCategoriesController | 拆 2 | task/CategoryController + task/admin/CategoryAdminController |
| TaskAdminController | 迁移 | task/admin/TaskAdminController（并入 /task/searchPage） |
| NotificationsController | 拆 2 | notice/NotificationController + notice/admin/NotificationAdminController |
| NotificationReadStatusController | 迁移 | notice/admin/ReadStatusAdminController |
| SystemAnnouncementsController | 拆 2 | notice/AnnouncementController + notice/admin/AnnouncementAdminController |
| AdminsettingsController | 迁移 | system/admin/SettingsAdminController |
| SensitiveWordController | 拆 2 | system/SensitiveWordController + system/admin/SensitiveWordAdminController |
| StatsController | 迁移 | system/StatsController |
| CreditController | 迁移 | credit/CreditController |
| ImgController | 更名 | file/FileController |
| CommonController | 迁移 | common/CommonController |
| MyErrorController | 迁移 | error/MyErrorController |
| DelegateAuditRecordsController | 删除 | — |

> 拆分后 `@PreAuthorize("hasAuthority('ADMIN')")` 注解保留（双保险），可视为冗余但无害。

## 六、同步点

1. **前端**：`web/src/api/index.js` + `web/src/api/user.js` 的 URL 字符串按映射表替换；`logout` 缺 `/` 前缀顺带修正；`getTaskList` 删除。
2. **安全白名单** `WebSecurityConfig.AUTH_WHITELIST`：
   - `/img/upload` → `/files/images`、`/img/uploadAvatar` → `/files/images/avatar`
   - `/user/login` → `/auth/login`、`/user/register` → `/auth/register`
   - `/user/logout` → `/auth/logout`、`/user/check` → `/auth/check`
   - 补 `/files/**` 是否需要公开（仅上传两张白名单化，其余保持鉴权）
3. **业务验证器 DSL 用例**（外部平台，不在本仓库）：路由全变，旧用例全失配。需输出一份映射清单交用户转交验证器更新，或按新路径重写用例（重点 `controller/` 层按控制器名组织的目录）。
4. **Swagger 注解**：`@Api(tags=...)` 随类迁移更新。

## 七、风险与验证策略

- 改动面大：后端 21 文件 + 前端 2 文件 + 白名单 + 验证器用例。**一次性提交**，`mvn compile`（3.9.16）验证编译通过。
- 规划期需全量检索 `.vue` 确认 `✗`/`?` 项的死活：确认死路由删除或保留。
- 后端跑测按既定分工交给 Business Validator，Coding-Agent 不预跑。
- 路径参数改名 `{TaskID}` → `{id}` 等仅为 URL 写法，不影响绑定。

## 八、不做（Out of Scope）

- HTTP 动词规范化（POST updateTaskDraft → PUT 等）——只改路径，不动动词。
- RESTful 资源化（`/tasks/1` 完整资源语义）——保持现有动词+动作式路径。
- 前端 .vue 内部逻辑改动（除 URL 调用点）。
- `AdminSettingsController` noop `save()` 的清理。
