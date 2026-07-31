# 分阶梯实名认证 · 详细设计

> **文档状态**：设计稿（供实现计划拆解与编码参考）
> **版本**：1.0
> **更新日期**：2026-08-01
> **前置**：[ROADMAP_1.0.md](ROADMAP_1.0.md) P2 延后清单（提前）；[MVP_1.0.md](MVP_1.0.md)「身份准入与信任基座」
> **决策基线**：本轮实现 **L1 实名认证** + 全委托流程门禁 + `IIdentityVerifier` 教务对接接口预留；**L2 校园卡**仅设计预留（2026-08-01 确认）。门禁实现采用 **`ensureL1` 辅助方法逐处调用**（非 AOP），统一抛 `UnauthorizedRealNameException` 由全局异常处理器收口。

---

## 1. 目标与范围

- **L1 实名认证**（本轮落地）：用户提交 姓名 + 身份标识（学号/工号/其他）+ 身份照片，管理员人工审核，生成基础实名身份（`auth_level=1`）。
- **门禁**：`auth_level ≥ 1` 为**全部委托流程操作**的硬性准入门槛；无认证用户仅保留只读游客权限。
- **旁证展示**：任务详情展示发布者认证身份（角色 + 掩码身份编号 + 认证等级徽章）。
- **教务对接预留**：`IIdentityVerifier` 接口，当前 `ManualIdentityVerifier`（人工审核），未来 `EduSystemIdentityVerifier`（调教务系统）无缝替换。

**不做**（本轮）：L2 校园卡认证实现（仅字段/接口/页面占位预留）；教务系统真实对接（仅接口预留）；自动核验。

---

## 2. 现状与关键事实

- 当前认证为**单级照片认证**：用户上传 `roleImgSrc`（身份照片）+ 选 `userRole`（student/teacher/other）→ 管理员审核 → `auth_status` 流转（UNAUTHORIZED/AUTHENTICATING/AUTHENTICATION_FAILED/AUTHENTICATED）。
- `usersinfo` 现字段：userId、name、phoneNumber、qqNumber、roleImgSrc、certifieTime、certifiedTime、userRole、auth_status。
- **发布缺口**：发布链路（`confirmTask`/`auditTask`）**无认证校验**；仅查看类接口（`getPublisher`、`publisherSearchTaskAndPublisherInfo`）校验 `auth_status == AUTHENTICATED`。导致「能发布、不能看发布者信息」的不一致。

---

## 3. 数据模型（`usersinfo` 扩展）

| 字段 | 类型 | 说明 | 状态 |
|---|---|---|---|
| `identity_no` | VARCHAR(50) | 学号/工号/其他校内编号，按 `userRole` 区分 | **新增（本轮落库）** |
| `auth_level` | TINYINT | 0=未认证 / 1=L1 实名 / 2=L2 校园卡 | **新增（本轮落库）** |
| `roleImgSrc` | VARCHAR(255) | 复用为 L1 身份照片 | 现有 |
| `campus_card_img` | VARCHAR(255) | L2 校园卡照片 | **设计预留（本轮不落库）** |

- `identity_no` 输入提示按角色：`student → 学号`、`teacher → 工号`、`other → 其他校内编号`。
- master SQL `校园委托0.99.sql` 同步 `usersinfo` 建表。

---

## 4. 认证状态机（`auth_status` + `auth_level`）

- 现有 `auth_status` 保持：UNAUTHORIZED(0)/AUTHENTICATING(1)/AUTHENTICATION_FAILED(2)/AUTHENTICATED(3)。
- 新增 `auth_level`：0/1/2，表示**已达成**的认证等级。
- **L1 申请流程**：MyInfo 填 姓名 + `identity_no`（学号/工号/其他）+ `roleImgSrc`（身份照片）→ 调 `IIdentityVerifier.verify(...)` → 手动模式下返回「待人工审核」→ `auth_status=AUTHENTICATING`。
- **管理员审核**：通过 → `auth_status=AUTHENTICATED` + `auth_level=1`；拒绝 → `auth_status=AUTHENTICATION_FAILED`。
- **L2（预留）**：L1 基础上申请 L2（上传校园卡照片 `campus_card_img`）→ 审核 → `auth_level=2`。本轮仅定义流程，不实现。

---

## 5. 门禁：L1 = 全部委托流程准入门槛

### 5.1 实现方式（已确认：辅助方法，非 AOP）

`AuthenticationService.ensureL1(Long userId)`：校验当前用户 `auth_level ≥ 1`，否则抛出 `UnauthorizedRealNameException`。

```java
public void ensureL1(Long userId) {
    UsersInfo info = usersInfoMapper.selectById(userId);
    int level = info != null && info.getAuthLevel() != null ? info.getAuthLevel() : 0;
    if (level < 1) {
        throw new UnauthorizedRealNameException();
    }
}
```

`UnauthorizedRealNameException`（新增业务异常）由**全局异常处理器**统一收口，返回提示：**「请先完成 L1 实名认证后再执行该操作」**。各门禁接口无需重复写提示。

### 5.2 门禁范围（`ensureL1` 逐处调用）

| 操作 | 接口 |
|---|---|
| 发布委托 | `PUT /user/publisher/confirmTask/{id}`、`PUT /task/auditTask/{id}` |
| 接单 | `POST /user/accept` |
| 查看联系方式/发布者 | `GET /user/publisher/{id}`、`publisherSearchTaskAndPublisherInfo` |
| 履约打卡 | `POST /taskUpdate/node` |
| 确认完成/确认接收人 | `PUT /user/publisher/completed/{id}`、`PUT /user/publisher/confirm/{id}` |
| 评价 | `POST /reviews/addReviews` |

> 现有查看类接口原校验 `auth_status == AUTHENTICATED`，统一改为 `ensureL1`（`auth_level ≥ 1`），消除「能发布不能看」的不一致。

### 5.3 游客只读

无认证用户（游客）：可浏览大厅、任务列表、任务基本信息；上述委托操作被 `ensureL1` 拦截。前端登录后无认证时展示「去认证」引导。

---

## 6. 认证作为委托旁证

任务详情/卡片展示发布者认证身份：
- `userRole`（学生/教师/其他）
- `identity_no` **掩码**（如 `2024*****01`，保留前 4 后 2）
- 认证等级徽章（L1 / L2）

兼顾隐私保护与履约可信度，与 M1 信用分展示体系统一。

---

## 7. 教务系统对接预留（`IIdentityVerifier`）

```java
public interface IIdentityVerifier {
    /** 校验身份，返回核验结果 */
    VerifyResult verify(String identityNo, String name, String role);
}
```

- `ManualIdentityVerifier`（当前实现，`@Component`）：返回「待人工审核」→ 走 `auth_status=AUTHENTICATING` 人工流程。
- `EduSystemIdentityVerifier`（未来实现）：调教务系统接口自动核验，通过则直接 `auth_level=1`。
- 配置切换：`app.identity-verifier.mode: manual | edu-system`，Spring 条件装配选择实现，业务代码无侵入。

---

## 8. 存量数据迁移

- 现有 `auth_status=AUTHENTICATED`（照片认证通过）用户 → 回填 `auth_level=1`（`identity_no` 为空，申请 L2 时补填）。
- 其余状态（AUTHENTICATING/AUTHENTICATION_FAILED/UNAUTHORIZED）保持 `auth_level=0`。
- 迁移走一次 JDBC `UPDATE usersinfo SET auth_level=1 WHERE auth_status=3` + master SQL 默认值。

---

## 9. 测试

| # | 场景 | 断言 |
| :-- | :--- | :--- |
| 1 | `ensureL1` 已认证用户 | 不抛异常 |
| 2 | `ensureL1` 未认证/游客 | 抛 `UnauthorizedRealNameException` |
| 3 | `ensureL1` 无 usersinfo 记录 | 抛异常（视为 level 0） |
| 4 | `ManualIdentityVerifier.verify` | 返回「待人工审核」结果 |
| 5 | 全局异常处理器捕获 `UnauthorizedRealNameException` | 返回统一提示文案 |
| 6 | 门禁接入：发布/接单等操作未认证被拦截 | 前端提示 + 后端异常 |

---

## 10. 已知限制

- **手动审核依赖管理员**：无自动核验，审核时效取决于管理员。
- **L2 校园卡本轮不实现**：字段/接口/页面占位预留。
- **学号/工号掩码展示**：保护隐私，前端按掩码规则渲染。
- **游客只读范围**：任务详情是否全量可见由展示侧控制，本轮按「基本信息可见」实现。
