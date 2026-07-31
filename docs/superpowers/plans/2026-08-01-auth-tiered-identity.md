# P2 分阶梯实名认证 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地 L1 实名认证（学号/工号/其他编号 + 身份照片，管理员人工审核），并以 `auth_level ≥ 1` 作为全部委托流程操作的门槛；预留 `IIdentityVerifier` 教务系统对接接口与 L2 占位。

**Architecture:** `usersinfo` 加 `identity_no`/`auth_level` 字段；`IIdentityVerifier` 接口（`ManualIdentityVerifier` 当前实现，`app.identity-verifier.mode` 切换，未来 `EduSystemIdentityVerifier`）；`RealNameAuthenticationService.ensureL1/ensureCurrentUserL1` 逐处调用门禁，抛 `UnauthorizedRealNameException` 由 `GlobalControllerAdvice` 统一收口；认证提交走 `verify` + 人工审核，审核通过置 `auth_level=1`。

**Tech Stack:** Spring Boot 2.7.3 / MyBatis-Plus 3.4.3 / JUnit 5 / AssertJ / Mockito / JDBC（迁移脚本）/ Vue 2 + Element UI。

**设计依据：** `docs/auth-tiered-identity-design.md`（2026-08-01 定稿）。

**关键环境事实：**
- 现有 `com.lz.common.security.AuthenticationService` 是**未使用的死代码**，不动它；新建 `com.lz.service.RealNameAuthenticationService`。
- 全局异常处理器 `com.lz.exceptionHandling.GlobalControllerAdvice` 已有 `@ExceptionHandler(MyException.class)` 等；`ErrorCode.UNAUTHORIZED` 已存在。
- 现有查看类接口用 `auth_status == AUTHENTICATED` 校验（`PublisherController.getPublisher`、`TaskServiceImpl.publisherSearchTaskAndPublisherInfo`），统一改 `ensureL1`。
- 本机无 mysql CLI，改库用 JDBC 脚本 + `java -cp`（mysql-connector-java 8.0.30 在 `D:\CODE\mvn_repository`）。

---

## 文件结构

**新建（后端）：**
- `src/main/java/com/lz/pojo/Enum/VerifyResult.java` — 核验结果枚举
- `src/main/java/com/lz/Exception/UnauthorizedRealNameException.java` — 门禁异常
- `src/main/java/com/lz/verifier/IIdentityVerifier.java` — 教务对接接口（预留）
- `src/main/java/com/lz/verifier/ManualIdentityVerifier.java` — 手动审核实现
- `src/main/java/com/lz/service/RealNameAuthenticationService.java` — ensureL1 门禁
- `src/test/java/com/lz/service/RealNameAuthenticationServiceTest.java` — 门禁单测
- `src/test/java/com/lz/verifier/ManualIdentityVerifierTest.java` — 手动校验单测
- `scripts/AddAuthColumns.java`（一次性迁移，跑完删除）

**修改（后端）：**
- `src/main/java/com/lz/pojo/entity/UsersInfo.java` — 加 `identityNo`/`authLevel`
- `src/main/java/com/lz/pojo/dto/UserInfoDTO.java` — 加 `identityNo`
- `src/main/java/com/lz/service/impl/UsersInfoServiceImpl.java` — submit 调 verify + 写字段；confirm/refuse 置 auth_level
- `src/main/java/com/lz/exceptionHandling/GlobalControllerAdvice.java` — 加 `UnauthorizedRealNameException` 处理器
- 门禁接入（8 处）：`PublisherController`（confirmTask/getPublisher）、`TaskController`（auditTask）、`AcceptController`（accept）、`TaskServiceImpl`（publisherSearchTaskAndPublisherInfo/updateToCompleted/confirmTheRecipient）、`TaskUpdatesServiceImpl`（addNodeUpdate）、`ReviewsServiceImpl`（save）
- `src/main/resources/application.yml` — 加 `app.identity-verifier.mode`

**修改（SQL/前端）：**
- `src/main/resources/sql/校园委托0.99.sql` — `usersinfo` 加 `identity_no`/`auth_level`
- `web/src/views/user/MyInfo.vue` — 申请表单加身份标识；游客「去认证」引导

---

### Task 1: DDL 迁移（`usersinfo` 加列 + 存量回填 + master SQL）

**Files:**
- Create: `scripts/AddAuthColumns.java`（一次性，跑完删除）
- Modify: `src/main/resources/sql/校园委托0.99.sql`

- [ ] **Step 1: 写 JDBC 迁移脚本**

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class AddAuthColumns {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/campus_entrustment?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        try (Connection conn = DriverManager.getConnection(url, "root", "1234");
             Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA='campus_entrustment' AND TABLE_NAME='usersinfo' AND COLUMN_NAME='auth_level'");
            rs.next();
            if (rs.getInt(1) == 0) {
                st.executeUpdate("ALTER TABLE usersinfo ADD COLUMN identity_no VARCHAR(50) DEFAULT NULL COMMENT '学号/工号/其他校内编号（按 userRole 区分）'");
                st.executeUpdate("ALTER TABLE usersinfo ADD COLUMN auth_level TINYINT NOT NULL DEFAULT 0 COMMENT '认证等级（0=未认证,1=L1实名,2=L2校园卡）'");
                st.executeUpdate("ALTER TABLE usersinfo ADD COLUMN reject_reason VARCHAR(255) DEFAULT NULL COMMENT '管理员驳回原因'");
                System.out.println("AUTH_COLUMNS_ADDED");
            } else {
                System.out.println("AUTH_COLUMNS_EXIST");
            }
            // 存量回填：历史照片认证通过（auth_status=3）→ auth_level=1（分批，事务）
            conn.setAutoCommit(false);
            int updated = 0;
            int batch;
            do {
                batch = st.executeUpdate("UPDATE usersinfo SET auth_level = 1 WHERE auth_status = 3 AND auth_level = 0 LIMIT 1000");
                updated += batch;
                conn.commit();
            } while (batch == 1000);
            System.out.println("BACKFILLED=" + updated);
        }
    }
}
```

- [ ] **Step 2: 编译执行 + 核对**

```bash
cd /d/workspace-dev/java/campus-errand/scripts
javac -cp "D:/CODE/mvn_repository/mysql/mysql-connector-java/8.0.30/mysql-connector-java-8.0.30.jar" AddAuthColumns.java
java -cp ".;D:/CODE/mvn_repository/mysql/mysql-connector-java/8.0.30/mysql-connector-java-8.0.30.jar" AddAuthColumns
rm -f AddAuthColumns.java AddAuthColumns.class
```
Expected: `AUTH_COLUMNS_ADDED`（或 EXISTS）+ `BACKFILLED=<待回填数>`。核对：`SELECT auth_status, auth_level, COUNT(*) FROM usersinfo GROUP BY auth_status, auth_level`。

- [ ] **Step 3: 同步 master SQL**

`校园委托0.99.sql` 的 `usersinfo` CREATE TABLE 中，在 `auth_status` 列之后加：

```sql
  `identity_no` varchar(50) DEFAULT NULL COMMENT '学号/工号/其他校内编号（按 userRole 区分）',
  `auth_level` tinyint NOT NULL DEFAULT '0' COMMENT '认证等级（0=未认证,1=L1实名,2=L2校园卡）',
  `reject_reason` varchar(255) DEFAULT NULL COMMENT '管理员驳回原因',
```

- [ ] **Step 4: Commit**

```bash
git add "src/main/resources/sql/校园委托0.99.sql"
git commit -m "chore: 实名认证 DDL — usersinfo 加 identity_no/auth_level，存量认证用户回填 level=1"
```

---

### Task 2: 实体字段 + VerifyResult 枚举 + UnauthorizedRealNameException

**Files:**
- Modify: `src/main/java/com/lz/pojo/entity/UsersInfo.java`
- Create: `src/main/java/com/lz/pojo/Enum/VerifyResult.java`
- Create: `src/main/java/com/lz/Exception/UnauthorizedRealNameException.java`

- [ ] **Step 1: `UsersInfo` 加字段**

在 `private AuthenticationStatus authStatus;` 之后、类结束前追加：

```java

    @ApiModelProperty(value = "学号/工号/其他校内编号（按 userRole 区分）")
    @TableField(value = "identity_no")
    private String identityNo;

    @ApiModelProperty(value = "认证等级（0=未认证,1=L1实名,2=L2校园卡）")
    @TableField(value = "auth_level")
    private Integer authLevel;

    @ApiModelProperty(value = "管理员驳回原因")
    @TableField(value = "reject_reason")
    private String rejectReason;
```

- [ ] **Step 2: 创建 `VerifyResult.java`**

```java
package com.lz.pojo.Enum;

import lombok.Getter;

/**
 * 身份核验结果
 *
 * @author lz
 */
@Getter
public enum VerifyResult {
    /** 待人工审核 */
    PENDING_MANUAL_AUDIT("待人工审核"),
    /** 核验通过 */
    PASS("核验通过"),
    /** 核验驳回 */
    REJECT("核验驳回");

    private final String description;

    VerifyResult(String description) {
        this.description = description;
    }
}
```

- [ ] **Step 3: 创建 `UnauthorizedRealNameException.java`**

```java
package com.lz.Exception;

/**
 * 未完成 L1 实名认证门禁异常
 *
 * @author lz
 */
public class UnauthorizedRealNameException extends RuntimeException {

    public static final String MESSAGE = "请先完成L1实名认证后再执行该操作";

    public UnauthorizedRealNameException() {
        super(MESSAGE);
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/lz/pojo/entity/UsersInfo.java src/main/java/com/lz/pojo/Enum/VerifyResult.java src/main/java/com/lz/Exception/UnauthorizedRealNameException.java
git commit -m "feat: 实名认证实体字段 identityNo/authLevel、VerifyResult 枚举、UnauthorizedRealNameException"
```

---

### Task 3: `IIdentityVerifier` 接口 + `ManualIdentityVerifier` + 配置（TDD）

**Files:**
- Test: `src/test/java/com/lz/verifier/ManualIdentityVerifierTest.java`
- Create: `src/main/java/com/lz/verifier/IIdentityVerifier.java`
- Create: `src/main/java/com/lz/verifier/ManualIdentityVerifier.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 写失败测试**

创建 `src/test/java/com/lz/verifier/ManualIdentityVerifierTest.java`：

```java
package com.lz.verifier;

import com.lz.pojo.Enum.VerifyResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ManualIdentityVerifier 单测
 *
 * @author lz
 */
class ManualIdentityVerifierTest {

    private final ManualIdentityVerifier verifier = new ManualIdentityVerifier();

    @Test
    void verify_alwaysPendingManualAudit() {
        assertThat(verifier.verify("20240101", "张三", "student"))
                .isEqualTo(VerifyResult.PENDING_MANUAL_AUDIT);
        assertThat(verifier.verify("T1001", "李四", "teacher"))
                .isEqualTo(VerifyResult.PENDING_MANUAL_AUDIT);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -q -Dtest=ManualIdentityVerifierTest test`
Expected: `BUILD FAILURE` — `ManualIdentityVerifier` 不存在。

- [ ] **Step 3: 实现接口与手动实现**

创建 `src/main/java/com/lz/verifier/IIdentityVerifier.java`：

```java
package com.lz.verifier;

import com.lz.pojo.Enum.VerifyResult;

/**
 * 身份核验接口（教务系统对接预留）
 *
 * @author lz
 */
public interface IIdentityVerifier {

    /**
     * 校验身份标识与姓名是否匹配
     *
     * @param identityNo 学号/工号/其他校内编号
     * @param name       姓名
     * @param role       student/teacher/other
     *
     * @return 核验结果
     */
    VerifyResult verify(String identityNo, String name, String role);
}
```

创建 `src/main/java/com/lz/verifier/ManualIdentityVerifier.java`：

```java
package com.lz.verifier;

import com.lz.pojo.Enum.VerifyResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 手动审核实现：恒返回「待人工审核」，由管理员后台核对
 *
 * @author lz
 */
@Component
@ConditionalOnProperty(name = "app.identity-verifier.mode", havingValue = "manual", matchIfMissing = true)
public class ManualIdentityVerifier implements IIdentityVerifier {

    @Override
    public VerifyResult verify(String identityNo, String name, String role) {
        return VerifyResult.PENDING_MANUAL_AUDIT;
    }
}
```

- [ ] **Step 4: `application.yml` 加配置**

在 `app.auto-advance` 块之后追加：

```yaml
  identity-verifier:
    # 身份核验模式：manual=管理员人工审核（当前）；edu-system=教务系统自动核验（未来预留）
    mode: manual
```

- [ ] **Step 5: 运行测试，确认全绿**

Run: `mvn -q -Dtest=ManualIdentityVerifierTest test`
Expected: `BUILD SUCCESS`，1 例 PASS。

- [ ] **Step 6: Commit**

```bash
git add src/test/java/com/lz/verifier/ManualIdentityVerifierTest.java src/main/java/com/lz/verifier/IIdentityVerifier.java src/main/java/com/lz/verifier/ManualIdentityVerifier.java src/main/resources/application.yml
git commit -m "feat: IIdentityVerifier 接口 + ManualIdentityVerifier 手动实现 + mode 配置"
```

---

### Task 4: `RealNameAuthenticationService.ensureL1`（TDD）

**Files:**
- Test: `src/test/java/com/lz/service/RealNameAuthenticationServiceTest.java`
- Create: `src/main/java/com/lz/service/RealNameAuthenticationService.java`

- [ ] **Step 1: 写失败测试**

创建 `src/test/java/com/lz/service/RealNameAuthenticationServiceTest.java`：

```java
package com.lz.service;

import com.lz.Exception.UnauthorizedRealNameException;
import com.lz.mapper.UsersInfoMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.entity.UsersInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * RealNameAuthenticationService.ensureL1 单测
 *
 * @author lz
 */
@ExtendWith(MockitoExtension.class)
class RealNameAuthenticationServiceTest {

    @Mock
    private UsersInfoMapper usersInfoMapper;

    private RealNameAuthenticationService newService() {
        RealNameAuthenticationService s = new RealNameAuthenticationService();
        ReflectionTestUtils.setField(s, "usersInfoMapper", usersInfoMapper);
        return s;
    }

    @Test
    void ensureL1_authenticated_passes() {
        UsersInfo info = UsersInfo.builder().userId(1L).authLevel(1).build();
        when(usersInfoMapper.selectById(1L)).thenReturn(info);

        assertThatCode(() -> newService().ensureL1(1L)).doesNotThrowAnyException();
    }

    @Test
    void ensureL1_unauthenticated_throws() {
        UsersInfo info = UsersInfo.builder().userId(1L).authLevel(0).build();
        when(usersInfoMapper.selectById(1L)).thenReturn(info);

        assertThatThrownBy(() -> newService().ensureL1(1L))
                .isInstanceOf(UnauthorizedRealNameException.class);
    }

    @Test
    void ensureL1_noRecord_throws() {
        when(usersInfoMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> newService().ensureL1(1L))
                .isInstanceOf(UnauthorizedRealNameException.class);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -q -Dtest=RealNameAuthenticationServiceTest test`
Expected: `BUILD FAILURE` — `RealNameAuthenticationService` 不存在。

- [ ] **Step 3: 实现 `RealNameAuthenticationService.java`**

```java
package com.lz.service;

import com.lz.Exception.UnauthorizedRealNameException;
import com.lz.mapper.UsersInfoMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.entity.Users;
import com.lz.pojo.entity.UsersInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 实名认证门禁服务
 *
 * @author lz
 */
@Service
public class RealNameAuthenticationService {

    @Autowired
    private UsersInfoMapper usersInfoMapper;

    @Autowired
    private UsersMapper usersMapper;

    /** 校验指定用户是否已 L1 实名认证（auth_level ≥ 1），否则抛门禁异常 */
    public void ensureL1(Long userId) {
        UsersInfo info = usersInfoMapper.selectById(userId);
        int level = info != null && info.getAuthLevel() != null ? info.getAuthLevel() : 0;
        if (level < 1) {
            throw new UnauthorizedRealNameException();
        }
    }

    /** 校验当前登录用户是否已 L1 实名认证 */
    public void ensureCurrentUserL1() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = usersMapper.getByUsername(username);
        ensureL1(user.getUserId());
    }
}
```

- [ ] **Step 4: 运行测试，确认全绿**

Run: `mvn -q -Dtest=RealNameAuthenticationServiceTest test`
Expected: `BUILD SUCCESS`，3 例全 PASS。

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/lz/service/RealNameAuthenticationServiceTest.java src/main/java/com/lz/service/RealNameAuthenticationService.java
git commit -m "feat: RealNameAuthenticationService.ensureL1 门禁（空记录兜底 level=0），含 3 例单测"
```

---

### Task 5: 全局异常处理器 + 认证提交/审核改造

**Files:**
- Modify: `src/main/java/com/lz/exceptionHandling/GlobalControllerAdvice.java`
- Modify: `src/main/java/com/lz/pojo/dto/UserInfoDTO.java`
- Modify: `src/main/java/com/lz/service/impl/UsersInfoServiceImpl.java`

- [ ] **Step 1: `GlobalControllerAdvice` 加处理器**

在 `@ExceptionHandler(MyException.class)` 之后加：

```java
    @ExceptionHandler(UnauthorizedRealNameException.class)
    public Result<?> unauthorizedRealNameException(UnauthorizedRealNameException e) {
        log.error("实名认证门禁拦截: {}", e.getMessage());
        return Result.error(ErrorCode.UNAUTHORIZED, e.getMessage());
    }
```

并加 import：`import com.lz.Exception.UnauthorizedRealNameException;`。

- [ ] **Step 2: `UserInfoDTO` 加 `identityNo` 字段**

```java
    private String identityNo;
```

- [ ] **Step 3: `UsersInfoServiceImpl` 改造**

**(a) 注入 `IIdentityVerifier`**：

```java
    @Autowired
    private IIdentityVerifier identityVerifier;
```

（import：`com.lz.verifier.IIdentityVerifier`、`com.lz.pojo.Enum.VerifyResult`。）

**(b) `submitCertificationInformation` 改造** — 写入 `identityNo`，调用 `verify`，手动模式一律 `AUTHENTICATING`：

```java
    @Override
    @PostMapping
    public void submitCertificationInformation(UserInfoDTO dto) throws MyException {
        Users byId = usersMapper.selectById(dto.getId());
        if (byId == null) {
            throw new MyException("用户不存在");
        }
        // 入参非空校验：身份标识、姓名、角色
        if (dto.getIdentityNo() == null || dto.getIdentityNo().trim().isEmpty()
                || dto.getName() == null || dto.getRole() == null) {
            throw new MyException("身份标识、姓名、角色不能为空");
        }
        VerifyResult result = identityVerifier.verify(dto.getIdentityNo(), dto.getName(), dto.getRole());

        UsersInfo usersInfo = UsersInfo.builder()
                .roleImgSrc(dto.getImgUrl())
                .name(dto.getName())
                .qqNumber(dto.getQq())
                .userId(dto.getId())
                .phoneNumber(dto.getPhone())
                .userRole(dto.getRole())
                .identityNo(dto.getIdentityNo())
                .authStatus(AuthenticationStatus.AUTHENTICATING)
                .certifieTime(new Date(System.currentTimeMillis()))
                .build();
        // edu 模式自动审核：PASS 直接 L1；REJECT 回认证失败（预留）
        if (result == VerifyResult.PASS) {
            usersInfo.setAuthStatus(AuthenticationStatus.AUTHENTICATED);
            usersInfo.setAuthLevel(1);
            usersInfo.setCertifiedTime(new Date(System.currentTimeMillis()));
        } else if (result == VerifyResult.REJECT) {
            usersInfo.setAuthStatus(AuthenticationStatus.AUTHENTICATION_FAILED);
        }
        save(usersInfo);
    }
```

> 手动模式恒返回 `PENDING_MANUAL_AUDIT`，走 `AUTHENTICATING` 人工流程。

**(c) `confirmToPassTheReview` 置 `auth_level=1` + 清空驳回原因**：

在 `usersInfo.setAuthStatus(AuthenticationStatus.AUTHENTICATED);` 之后加：

```java
            usersInfo.setAuthLevel(1);
            usersInfo.setRejectReason(null);
```

**(d) `refuseToPassReview` 置 `auth_level=0` + 存驳回原因** — 方法签名加 `String reason` 参数：

```java
    @Override
    public Boolean refuseToPassReview(Long id, String reason) throws MyException {
        UsersInfo usersInfo = getById(id);
        if (usersInfo != null) {
            if (usersInfo.getAuthStatus() != AuthenticationStatus.AUTHENTICATING){
                throw new MyException(MessageConstants.USER_STATUS_ERROR);
            }
            usersInfo.setAuthStatus(AuthenticationStatus.AUTHENTICATION_FAILED);
            usersInfo.setAuthLevel(0);
            usersInfo.setRejectReason(reason);
            usersInfo.setCertifiedTime(new Date(System.currentTimeMillis()));
            return updateById(usersInfo);
        }else {
            throw new MyException(MessageConstants.USER_NOT_EXIST);
        }
    }
```

> 同步改接口 `IUsersInfoService.refuseToPassReview` 签名，及控制器 `refuseToPassReview/{id}` 增加 `@RequestParam(value="reason", required=false) String reason`。

- [ ] **Step 4: 编译验证**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/lz/exceptionHandling/GlobalControllerAdvice.java src/main/java/com/lz/pojo/dto/UserInfoDTO.java src/main/java/com/lz/service/impl/UsersInfoServiceImpl.java
git commit -m "feat: 认证提交接入 IIdentityVerifier 并写 identityNo/authLevel；全局处理 UnauthorizedRealNameException"
```

---

### Task 6: 门禁接入（8 处 ensureL1）

**Files（均为 Modify）：**
- `src/main/java/com/lz/controller/user/PublisherController.java`（confirmTask + getPublisher）
- `src/main/java/com/lz/controller/TaskController.java`（auditTask）
- `src/main/java/com/lz/controller/user/AcceptController.java`（accept）
- `src/main/java/com/lz/service/impl/TaskServiceImpl.java`（publisherSearchTaskAndPublisherInfo + updateToCompleted + confirmTheRecipient）
- `src/main/java/com/lz/service/impl/TaskUpdatesServiceImpl.java`（addNodeUpdate）
- `src/main/java/com/lz/service/impl/ReviewsServiceImpl.java`（save）

每个文件加注入字段：

```java
    @Autowired
    private RealNameAuthenticationService realNameAuthenticationService;
```

（import：`com.lz.service.RealNameAuthenticationService`。）

- [ ] **Step 1: `PublisherController.confirmTask` 开头加 `ensureCurrentUserL1`**

在 `confirmTask` 方法体第一行加：

```java
        realNameAuthenticationService.ensureCurrentUserL1();
```

- [ ] **Step 2: `PublisherController.getPublisher` 替换原 auth_status 校验**

将：

```java
        if (usersInfo.getAuthStatus() != AuthenticationStatus.AUTHENTICATED) {
            throw new MyException(MessageConstants.USER_AUTHENTICATION_INFO_EXISTING);
        }
```

替换为：

```java
        realNameAuthenticationService.ensureL1(id);
```

- [ ] **Step 3: `TaskController.auditTask` 开头加 `ensureCurrentUserL1`**

在 `auditTask` 方法体第一行加：

```java
        realNameAuthenticationService.ensureCurrentUserL1();
```

- [ ] **Step 4: `AcceptController.accept` 开头加 `ensureCurrentUserL1`**

在 `accept` 方法体第一行加：

```java
        realNameAuthenticationService.ensureCurrentUserL1();
```

- [ ] **Step 5: `TaskServiceImpl.publisherSearchTaskAndPublisherInfo` 替换原 auth_status 校验**

将：

```java
        if (usersInfo == null || usersInfo.getAuthStatus() != AuthenticationStatus.AUTHENTICATED) {
            throw new MyException(MessageConstants.USER_NOT_EXIST);
        }
```

替换为：

```java
        realNameAuthenticationService.ensureL1(task.getOwnerId());
```

- [ ] **Step 6: `TaskServiceImpl.updateToCompleted` 开头加 `ensureCurrentUserL1`**

在 `updateToCompleted` 方法体第一行（`Users users = getCurrentAdmin();` 之前）加：

```java
        realNameAuthenticationService.ensureCurrentUserL1();
```

- [ ] **Step 7: `TaskServiceImpl.confirmTheRecipient` 开头加 `ensureCurrentUserL1`**

在 `confirmTheRecipient` 方法体第一行加：

```java
        realNameAuthenticationService.ensureCurrentUserL1();
```

- [ ] **Step 8: `TaskUpdatesServiceImpl.addNodeUpdate` 开头加 `ensureCurrentUserL1`**

在 `addNodeUpdate` 方法体第一行（`Users user = getCurrentAdmin();` 之前）加：

```java
        realNameAuthenticationService.ensureCurrentUserL1();
```

- [ ] **Step 9: `ReviewsServiceImpl.save` 开头加 `ensureCurrentUserL1`**

在 `save` 方法体第一行加：

```java
        realNameAuthenticationService.ensureCurrentUserL1();
```

- [ ] **Step 10: 编译验证 + 单测回归**

Run: `mvn -q compile` → `BUILD SUCCESS`；`mvn -q -Dtest=RealNameAuthenticationServiceTest,ManualIdentityVerifierTest test` → 4 例全绿。

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/lz/controller/user/PublisherController.java src/main/java/com/lz/controller/TaskController.java src/main/java/com/lz/controller/user/AcceptController.java src/main/java/com/lz/service/impl/TaskServiceImpl.java src/main/java/com/lz/service/impl/TaskUpdatesServiceImpl.java src/main/java/com/lz/service/impl/ReviewsServiceImpl.java
git commit -m "feat: 全部委托流程操作接入 L1 门禁，查看发布者校验统一为 ensureL1"
```

---

### Task 7: 前端 — MyInfo 实名认证表单改造 + 管理端驳回原因

**Files:**
- Modify: `web/src/views/user/MyInfo.vue`
- Modify: `web/src/views/admin/UserList.vue`

- [ ] **Step 1: 认证状态分区展示（四态）**

按 `infoForm.authStatus`（未认证/认证中/认证失败/认证通过）分区：
- 未认证：展示「去认证」按钮，申请表单可用
- 认证中：禁用表单，展示「审核中，请等待」
- 认证失败：红色警示展示驳回原因 `infoForm.rejectReason`，表单可重提
- 认证通过：隐藏申请表单，展示「已认证」徽章 + L2 占位按钮

顶部状态卡片：

```html
      <el-card v-if="authState !== '认证通过'" shadow="hover" style="margin-bottom: 10px;">
        <el-alert v-if="authState === '认证失败'" :title="'认证被驳回：' + (infoForm.rejectReason || '材料不符')"
          type="error" :closable="false" show-icon />
        <el-alert v-else :title="authState === '认证中' ? '审核中，请等待管理员审核' : '完成 L1 实名认证后可发布委托、接单、打卡'"
          type="warning" :closable="false" show-icon>
          <el-button v-if="authState === '未认证'" slot="title" type="primary" size="mini" @click="dialogUserInfo = true">去认证</el-button>
        </el-alert>
      </el-card>
```

`computed` 加：

```js
      authState() {
        return this.infoForm.authStatus || '未认证'
      },
```

- [ ] **Step 2: 申请表单加「身份标识」+ 角色联动占位**

在 `认证角色` radio 之后、`</el-form>` 之前加：

```html
        <el-form-item label="身份标识" prop="identityNo">
          <el-input v-model="infoAddForm.identityNo"
            :placeholder="infoAddForm.role === 'student' ? '请输入学号' : infoAddForm.role === 'teacher' ? '请输入工号' : '请输入校内编号'"></el-input>
        </el-form-item>
```

`data` 的 `infoAddForm` 加 `identityNo: ''`、`role: 'student'`（默认角色，切换 radio 时 placeholder 联动）。

- [ ] **Step 3: 提交前基础校验（姓名/身份编号/照片必填）**

`submitAnApplication` 开头加：

```js
      if (!this.infoAddForm.name) { this.$message.warning('请填写姓名'); return }
      if (!this.infoAddForm.identityNo) { this.$message.warning('请填写身份标识'); return }
      const img = this.$refs.imageSet && this.$refs.imageSet.imageUrls
      if (!img || !img.length) { this.$message.warning('请上传身份照片'); return }
```

- [ ] **Step 4: L2 校园卡入口占位（本轮不开发）**

认证信息卡片内加禁用占位按钮：

```html
        <el-button type="info" size="small" disabled title="即将上线">L2 校园卡认证（即将上线）</el-button>
```

- [ ] **Step 5: 提交带 `identityNo`**

`submitCertificationInformation` 的入参加上 `identityNo`；`getUserInfo` 响应已含 `rejectReason`（后端 Task 5 已加字段），前端读取展示。

- [ ] **Step 6: 管理端驳回原因输入**

`web/src/views/admin/UserList.vue` 的驳回认证操作改为 `$prompt` 输入原因：

```js
      this.$prompt('请输入驳回原因', '驳回认证', { inputValidator: v => !!v })
        .then(({ value }) => this.$refs.form.refuse(value))
```

调用 `refuseToPassReview(userId, reason)`（传 `reason` 参数）。

- [ ] **Step 7: 前端构建验证**

Run: `cd web && npm run build`
Expected: 构建完成无报错。

- [ ] **Step 8: Commit**

```bash
git add web/src/views/user/MyInfo.vue web/src/views/admin/UserList.vue
git commit -m "feat: MyInfo 实名认证表单（状态区分/角色联动/驳回回显/L2占位/必填校验），管理端驳回带原因"
```

> **旁证展示（设计 §6）延后**：任务详情展示发布者掩码身份编号 + L1 徽章涉及任务详情/大厅多页展示改造，本轮聚焦门禁与申请流程，旁证展示列为后续增强（设计稿已定义掩码规则）。

---

### Task 8: 全量集成自测

- [ ] **Step 1: 全量单测回归**

Run: `mvn -q -Dtest=RealNameAuthenticationServiceTest,ManualIdentityVerifierTest,AutoAdvanceJudgeTest,TaskAutoAdvanceTest,TaskServiceImplHallSearchTest,CreditScoreServiceTest,SimpleCreditCalculatorTest test`
Expected: `BUILD SUCCESS`（3 + 1 + 7 + 3 + 2 + 3 + 6 = 25 例）。

- [ ] **Step 2: 编译**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: 重启后端 + HTTP 验证**

停旧后端、重启 `mvn spring-boot:run`。用 m2test01（未认证，auth_level=0）登录拿 token：
1. `POST /user/accept` 或 `GET /user/task/page` 之外的受门禁接口 → 预期返回「请先完成L1实名认证后再执行该操作」。
2. `POST /userInfo` 提交认证（identityNo="20249999", role="student", imgUrl 占位）→ `auth_status=认证中`。
3. 管理员（如 seed admin）审核通过 → `auth_level=1`；再访问门禁接口 → 放行。

- [ ] **Step 4: 前端构建 + 手动验收**（登录后 MyInfo 显示身份标识输入、游客引导）

Run: `cd web && npm run build` → 成功。

- [ ] **Step 5: 清理造数**（如创建了测试认证记录，按 userId 清理 usersinfo 测试行）
