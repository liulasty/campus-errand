# M3 防断流 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 任务在履约阶段永不卡死 — ACCEPTED 任务节点超时自动推进、完成确认超时自动置 COMPLETED，均写 `AUTO_ADVANCE` 留痕。

**Architecture:** 新增 `AutoAdvanceJudge` 纯判定（无 DB 依赖，单测覆盖）与 `TaskAutoAdvance` 定时任务（分页扫描 → `task.version` 乐观锁 claim → 锚点计时 → 按判定结果插入 `AUTO_ADVANCE` 或置 `COMPLETED`）。`taskupdates.NodeIndex` 列持久化节点进度，手动打卡与自动推进都写该列。

**Tech Stack:** Spring Boot 2.7.3 / MyBatis-Plus 3.4.3 / JUnit 5 / AssertJ / Mockito / JDBC（迁移脚本）。

**设计依据：** `docs/M3_anti-break-design.md`（2026-08-01 定稿）。

**关键环境事实：**
- `taskupdates.UpdateType` 是 MySQL **ENUM 列**，`AUTO_ADVANCE` 需 `ALTER ... MODIFY` 枚举，不只是 Java 枚举。
- mybatis-plus 3.4.3 `selectCount` 返回 `Integer`。
- 本机无 mysql CLI，改库用 JDBC 脚本 + `java -cp`（mysql-connector-java 8.0.30 在 `D:\CODE\mvn_repository`）。
- 定时任务基建已启用（TaskOverDue / TaskAutoReview 在用 `@Scheduled`）。
- `Duration.between(anchor, now).toHours() > node-hours`：不足整小时不触发（`toHours()` 向下取整）。

---

## 文件结构

**新建（后端）：**
- `src/main/java/com/lz/task/AutoAdvanceJudge.java` — 纯判定方法
- `src/main/java/com/lz/task/TaskAutoAdvance.java` — 定时任务
- `src/test/java/com/lz/task/AutoAdvanceJudgeTest.java` — 纯判定单测
- `src/test/java/com/lz/task/TaskAutoAdvanceTest.java` — process 接线 Mockito 单测
- `scripts/AddAntiBreakColumns.java`（一次性迁移，跑完删除）

**修改（后端）：**
- `src/main/java/com/lz/pojo/Enum/TaskUpdateType.java` — 加 `AUTO_ADVANCE`
- `src/main/java/com/lz/pojo/entity/TaskUpdates.java` — 加 `nodeIndex` 字段
- `src/main/java/com/lz/pojo/entity/Task.java` — 加 `version` 字段
- `src/main/java/com/lz/service/impl/TaskUpdatesServiceImpl.java` — `addNodeUpdate` 写 `NodeIndex`
- `src/main/resources/application.yml` — 加 `app.auto-advance.*`

**修改（SQL）：**
- `src/main/resources/sql/校园委托0.99.sql` — `taskupdates` 加 `NodeIndex` 列 + 枚举加 `AUTO_ADVANCE`；`task` 加 `version` 列

---

### Task 1: DDL 迁移（本地库 + master SQL 同步）

**Files:**
- Create: `scripts/AddAntiBreakColumns.java`（一次性，跑完删除）
- Modify: `src/main/resources/sql/校园委托0.99.sql`

- [ ] **Step 1: 写 JDBC 迁移脚本 `scripts/AddAntiBreakColumns.java`**

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class AddAntiBreakColumns {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/campus_entrustment?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        try (Connection conn = DriverManager.getConnection(url, "root", "1234");
             Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA='campus_entrustment' AND TABLE_NAME='taskupdates' AND COLUMN_NAME='NodeIndex'");
            rs.next();
            if (rs.getInt(1) == 0) {
                st.executeUpdate("ALTER TABLE taskupdates ADD COLUMN NodeIndex TINYINT NOT NULL DEFAULT 0 COMMENT '履约节点进度（0=无,1=已联系,2=已取件,3=已送达）'");
                System.out.println("NODEINDEX_ADDED");
            }
            st.executeUpdate("ALTER TABLE taskupdates MODIFY COLUMN UpdateType "
                    + "enum('CREATED','AUDITING','PUBLISHED','RESULT','FALLBACK_DRAFT','PROGRESS_UPDATE',"
                    + "'CONTACTED','PICKED_UP','DELIVERED','AUTO_ADVANCE') "
                    + "CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CREATED' COMMENT '委托更新记录类型'");
            System.out.println("ENUM_AUTO_ADVANCE_ADDED");
            ResultSet rs2 = st.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA='campus_entrustment' AND TABLE_NAME='task' AND COLUMN_NAME='version'");
            rs2.next();
            if (rs2.getInt(1) == 0) {
                st.executeUpdate("ALTER TABLE task ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（防多实例/并发重复推进）'");
                System.out.println("TASK_VERSION_ADDED");
            }
        }
    }
}
```

- [ ] **Step 2: 编译并执行迁移**

```bash
cd /d/workspace-dev/java/campus-errand/scripts
javac -cp "D:/CODE/mvn_repository/mysql/mysql-connector-java/8.0.30/mysql-connector-java-8.0.30.jar" AddAntiBreakColumns.java
java -cp ".;D:/CODE/mvn_repository/mysql/mysql-connector-java/8.0.30/mysql-connector-java-8.0.30.jar" AddAntiBreakColumns
rm -f AddAntiBreakColumns.java AddAntiBreakColumns.class
```
Expected: `NODEINDEX_ADDED`（或重复运行显示无）、`ENUM_AUTO_ADVANCE_ADDED`、`TASK_VERSION_ADDED`。

- [ ] **Step 3: 同步 master SQL — `taskupdates` 建表**

在 `校园委托0.99.sql` 的 `taskupdates` CREATE TABLE 中，`UpdateType` 枚举追加 `,'AUTO_ADVANCE'`（在 `'DELIVERED'` 之后），并在 `Location` 列之后加：

```sql
  `NodeIndex` tinyint NOT NULL DEFAULT '0' COMMENT '履约节点进度（0=无,1=已联系,2=已取件,3=已送达）',
```

- [ ] **Step 4: 同步 master SQL — `task` 建表**

在 `校园委托0.99.sql` 的 `task` CREATE TABLE 中，`STATUS` 列之后、`PRIMARY KEY` 之前加：

```sql
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号（防多实例/并发重复推进）',
```

- [ ] **Step 5: 存量数据兼容说明（写入提交说明）**

现有 `taskupdates` 行 `NodeIndex=0`（旧非节点事件不会被节点事件查询统计）；现有 `task` 行 `version=0`；`AUTO_ADVANCE` 仅新增使用，不改变既有枚举值语义。

- [ ] **Step 6: Commit**

```bash
git add "src/main/resources/sql/校园委托0.99.sql"
git commit -m "chore: 防断流 DDL — taskupdates.NodeIndex 与 AUTO_ADVANCE 枚举、task.version 乐观锁列"
```

---

### Task 2: 枚举 + 实体字段

**Files:**
- Modify: `src/main/java/com/lz/pojo/Enum/TaskUpdateType.java`
- Modify: `src/main/java/com/lz/pojo/entity/TaskUpdates.java`
- Modify: `src/main/java/com/lz/pojo/entity/Task.java`

- [ ] **Step 1: `TaskUpdateType` 加 `AUTO_ADVANCE`**

在 `DELIVERED("DELIVERED", 8, "已送达");` 之后追加：

```java

    /**
     * 系统自动推进（超时未操作）
     */
    AUTO_ADVANCE("AUTO_ADVANCE", 9, "自动推进");
```

- [ ] **Step 2: `TaskUpdates` 实体加 `nodeIndex` 字段**

在 `private String location;` 之后、类结束前追加：

```java

    @ApiModelProperty(value = "履约节点进度（0=无,1=已联系,2=已取件,3=已送达）")
    @TableField("NodeIndex")
    private Integer nodeIndex;
```

- [ ] **Step 3: `Task` 实体加 `version` 字段**

在 `private TaskStatus status;` 之后、`ownerCredit` 之前追加：

```java

    @ApiModelProperty(value = "乐观锁版本号（防多实例/并发重复推进）")
    private Integer version;
```

> 普通字段，**不加 `@Version` 注解**，避免触发 MyBatis-Plus 乐观锁插件影响既有 `updateById`。

- [ ] **Step 4: 编译验证**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/lz/pojo/Enum/TaskUpdateType.java src/main/java/com/lz/pojo/entity/TaskUpdates.java src/main/java/com/lz/pojo/entity/Task.java
git commit -m "feat: 新增 TaskUpdateType.AUTO_ADVANCE，实体加 nodeIndex/version 字段"
```

---

### Task 3: 手动打卡 `addNodeUpdate` 写 `NodeIndex`

**Files:**
- Modify: `src/main/java/com/lz/service/impl/TaskUpdatesServiceImpl.java`

- [ ] **Step 1: 修改 `addNodeUpdate`**

当前 `addNodeUpdate` 在 `String description = ...` 之前已校验 `nodeType` 为 CONTACTED/PICKED_UP/DELIVERED。在 `String description = ...` 之后、`TaskUpdates updates = TaskUpdates.builder()` 之前插入：

```java
        int level = levelOf(nodeType);
        Integer currentMax = list(new QueryWrapper<TaskUpdates>()
                .eq("TaskID", task.getTaskId())
                .in("UpdateType", TaskUpdateType.CONTACTED.getDbValue(),
                        TaskUpdateType.PICKED_UP.getDbValue(),
                        TaskUpdateType.DELIVERED.getDbValue(),
                        TaskUpdateType.AUTO_ADVANCE.getDbValue()))
                .stream()
                .map(TaskUpdates::getNodeIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        int nodeIndex = Math.max(currentMax, level);
```

在 builder 中 `updateTime(new Date())` 之后加：

```java
                .nodeIndex(nodeIndex)
```

在类内新增私有方法（`levelOf` 与 `currentMax` 使用到的导入：`java.util.Objects`、`com.baomidou.mybatisplus.core.conditions.query.QueryWrapper`、`com.lz.pojo.Enum.TaskUpdateType`）：

```java
    private int levelOf(TaskUpdateType nodeType) {
        if (nodeType == TaskUpdateType.CONTACTED) {
            return 1;
        }
        if (nodeType == TaskUpdateType.PICKED_UP) {
            return 2;
        }
        if (nodeType == TaskUpdateType.DELIVERED) {
            return 3;
        }
        return 0;
    }
```

> 当前类已导入 `TaskUpdateType`，**需补两个 import**：在 `import com.baomidou.mybatisplus.core.metadata.IPage;` 前加 `import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;`，在 `import java.util.Date;` 前加 `import java.util.Objects;`。`list(...)` 是 ServiceImpl 自带方法。

- [ ] **Step 2: 编译验证**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/lz/service/impl/TaskUpdatesServiceImpl.java
git commit -m "feat: 手动节点打卡写入 NodeIndex（取既有最大值与本次级别较大者，容忍乱序）"
```

---

### Task 4: `AutoAdvanceJudge` 纯判定（TDD）

**Files:**
- Test: `src/test/java/com/lz/task/AutoAdvanceJudgeTest.java`
- Create: `src/main/java/com/lz/task/AutoAdvanceJudge.java`

- [ ] **Step 1: 写失败测试**

创建 `src/test/java/com/lz/task/AutoAdvanceJudgeTest.java`：

```java
package com.lz.task;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AutoAdvanceJudge 纯判定单测
 *
 * @author lz
 */
class AutoAdvanceJudgeTest {

    private Date hoursAgo(long hours) {
        return new Date(System.currentTimeMillis() - hours * 3600 * 1000L);
    }

    @Test
    void noNode_expired_advanceToNode1() {
        assertThat(AutoAdvanceJudge.judge(0, hoursAgo(10), new Date(), 6, 24)).isEqualTo(1);
    }

    @Test
    void noNode_notExpired_noAction() {
        assertThat(AutoAdvanceJudge.judge(0, hoursAgo(2), new Date(), 6, 24)).isEqualTo(0);
    }

    @Test
    void nodeReached2_expired_advanceTo3() {
        assertThat(AutoAdvanceJudge.judge(2, hoursAgo(10), new Date(), 6, 24)).isEqualTo(3);
    }

    @Test
    void nodeReached3_completeExpired_complete() {
        assertThat(AutoAdvanceJudge.judge(3, hoursAgo(30), new Date(), 6, 24)).isEqualTo(-1);
    }

    @Test
    void nodeReached3_completeNotExpired_noAction() {
        assertThat(AutoAdvanceJudge.judge(3, hoursAgo(10), new Date(), 6, 24)).isEqualTo(0);
    }

    @Test
    void nullAnchor_noAction() {
        assertThat(AutoAdvanceJudge.judge(0, null, new Date(), 6, 24)).isEqualTo(0);
    }

    @Test
    void lessThanFullHour_notTrigger() {
        Date anchor = new Date(System.currentTimeMillis() - (5 * 3600 * 1000L + 59 * 60 * 1000L));
        assertThat(AutoAdvanceJudge.judge(0, anchor, new Date(), 6, 24)).isEqualTo(0);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -q -Dtest=AutoAdvanceJudgeTest test`
Expected: `BUILD FAILURE` — `AutoAdvanceJudge` 不存在。

- [ ] **Step 3: 实现 `src/main/java/com/lz/task/AutoAdvanceJudge.java`**

```java
package com.lz.task;

import java.time.Duration;
import java.util.Date;

/**
 * 防断流动作判定（纯逻辑，无 DB 依赖）
 *
 * @author lz
 */
public final class AutoAdvanceJudge {

    private AutoAdvanceJudge() {
    }

    /**
     * 判定防断流动作。
     *
     * @param nodeReached    该任务已到达的最高节点级别（0=无,1=已联系,2=已取件,3=已送达）
     * @param anchor         锚点时间（最新节点事件时间，或接单确认时间）
     * @param now            当前时间
     * @param nodeHours      节点超时小时数
     * @param completeHours  完成确认超时小时数
     *
     * @return 1..3 = 推进至该 NodeIndex；-1 = 置 COMPLETED；0 = 不处理
     */
    public static int judge(int nodeReached, Date anchor, Date now,
            long nodeHours, long completeHours) {
        if (anchor == null || now == null) {
            return 0;
        }
        long elapsedHours = Duration.between(anchor.toInstant(), now.toInstant()).toHours();
        if (nodeReached < 3) {
            return elapsedHours > nodeHours ? nodeReached + 1 : 0;
        }
        return elapsedHours > completeHours ? -1 : 0;
    }
}
```

- [ ] **Step 4: 运行测试，确认全绿**

Run: `mvn -q -Dtest=AutoAdvanceJudgeTest test`
Expected: `BUILD SUCCESS`，7 例全 PASS。

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/lz/task/AutoAdvanceJudgeTest.java src/main/java/com/lz/task/AutoAdvanceJudge.java
git commit -m "feat: 新增 AutoAdvanceJudge 纯判定方法（节点超时推进/完成超时完成），含 7 例单测"
```

---

### Task 5: `TaskAutoAdvance` 定时任务（TDD）

**Files:**
- Test: `src/test/java/com/lz/task/TaskAutoAdvanceTest.java`
- Create: `src/main/java/com/lz/task/TaskAutoAdvance.java`

- [ ] **Step 1: 写失败测试**

创建 `src/test/java/com/lz/task/TaskAutoAdvanceTest.java`：

```java
package com.lz.task;

import com.lz.mapper.TaskAcceptRecordsMapper;
import com.lz.mapper.TaskMapper;
import com.lz.mapper.TaskUpdatesMapper;
import com.lz.pojo.Enum.TaskUpdateType;
import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.TaskAcceptRecords;
import com.lz.pojo.entity.TaskUpdates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskAutoAdvance.process 接线单测（Mockito，验证乐观锁抢占 + 动作执行）
 *
 * @author lz
 */
@ExtendWith(MockitoExtension.class)
class TaskAutoAdvanceTest {

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskUpdatesMapper taskUpdatesMapper;
    @Mock
    private TaskAcceptRecordsMapper taskAcceptRecordsMapper;

    private TaskAutoAdvance newTask() {
        TaskAutoAdvance t = new TaskAutoAdvance();
        ReflectionTestUtils.setField(t, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(t, "taskUpdatesMapper", taskUpdatesMapper);
        ReflectionTestUtils.setField(t, "taskAcceptRecordsMapper", taskAcceptRecordsMapper);
        // @Value 字段在纯单测中未注入，显式设默认值使判定确定性
        ReflectionTestUtils.setField(t, "nodeHours", 6L);
        ReflectionTestUtils.setField(t, "completeHours", 24L);
        return t;
    }

    private Task task100() {
        Task task = new Task();
        task.setTaskId(100L);
        task.setVersion(0);
        return task;
    }

    @Test
    void process_claimLost_skipsNoAction() {
        when(taskMapper.update(any(), any())).thenReturn(0);

        boolean acted = newTask().process(task100());

        verify(taskUpdatesMapper, never()).insert(any(TaskUpdates.class));
    }

    @Test
    void process_noNodesAndAdoptTimeout_insertsAutoAdvance() {
        when(taskMapper.update(any(), any())).thenReturn(1);
        when(taskUpdatesMapper.selectList(any())).thenReturn(Collections.emptyList());
        TaskAcceptRecords rec = new TaskAcceptRecords();
        rec.setAdoptTime(new Date(System.currentTimeMillis() - 10 * 3600 * 1000L));
        when(taskAcceptRecordsMapper.selectOne(any())).thenReturn(rec);

        boolean acted = newTask().process(task100());

        verify(taskUpdatesMapper).insert(argThat(u -> u.getUpdateType() == TaskUpdateType.AUTO_ADVANCE
                && u.getNodeIndex() == 1));
    }

    @Test
    void process_nodeReached3AndCompleteTimeout_completes() {
        when(taskMapper.update(any(), any())).thenReturn(1);
        TaskUpdates delivered = TaskUpdates.builder()
                .updateType(TaskUpdateType.DELIVERED)
                .nodeIndex(3)
                .updateTime(new Date(System.currentTimeMillis() - 30 * 3600 * 1000L))
                .build();
        when(taskUpdatesMapper.selectList(any())).thenReturn(Collections.singletonList(delivered));

        boolean acted = newTask().process(task100());

        verify(taskMapper).update(any(), argThat(w -> w.getSqlSet().contains("COMPLETED")));
    }
}
```

> 注：第三个用例中 `process` 会调两次 `taskMapper.update`（claim + 置 COMPLETED）。Mockito 严格桩下 claim 桩被使用、置 COMPLETED 的调用用 `verify` 验证；如报 UnnecessaryStubbing，可将 claim 桩改用 `when(...).thenReturn(1, 1)` 或对两次调用分别打桩，以实际执行为准调整。

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -q -Dtest=TaskAutoAdvanceTest test`
Expected: `BUILD FAILURE` — `TaskAutoAdvance` 不存在。

- [ ] **Step 3: 实现 `src/main/java/com/lz/task/TaskAutoAdvance.java`**

```java
package com.lz.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lz.mapper.TaskAcceptRecordsMapper;
import com.lz.mapper.TaskMapper;
import com.lz.mapper.TaskUpdatesMapper;
import com.lz.pojo.Enum.AcceptStatus;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.Enum.TaskUpdateType;
import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.TaskAcceptRecords;
import com.lz.pojo.entity.TaskUpdates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 履约防断流：节点超时自动推进 / 完成确认超时自动完成
 *
 * @author lz
 */
@Component
@Slf4j
public class TaskAutoAdvance {

    private static final int BATCH_SIZE = 200;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskUpdatesMapper taskUpdatesMapper;

    @Autowired
    private TaskAcceptRecordsMapper taskAcceptRecordsMapper;

    @Value("${app.auto-advance.enabled:true}")
    private boolean enabled;

    @Value("${app.auto-advance.node-hours:6}")
    private long nodeHours;

    @Value("${app.auto-advance.complete-hours:24}")
    private long completeHours;

    @Scheduled(cron = "${app.auto-advance.cron:0 */5 * * * ?}")
    public void autoAdvance() {
        if (!enabled) {
            return;
        }
        log.info("履约防断流检查开始...");
        int page = 1;
        int processed = 0;
        while (true) {
            List<Task> batch = taskMapper.selectList(new QueryWrapper<Task>()
                    .eq("STATUS", TaskStatus.ACCEPTED.getDbValue())
                    .and(w -> w.isNull("EndTime").or().gt("EndTime", new Date()))
                    .orderByAsc("TaskID")
                    .last("LIMIT " + BATCH_SIZE + " OFFSET " + (page - 1) * BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            for (Task task : batch) {
                if (process(task)) {
                    processed++;
                }
            }
            page++;
            if (batch.size() < BATCH_SIZE) {
                break;
            }
        }
        log.info("履约防断流检查结束，本次处理 {} 条", processed);
    }

    /** 处理单条任务；乐观锁抢占成功才处理。返回是否产生了推进/完成动作。 */
    boolean process(Task task) {
        int claimed = taskMapper.update(null, new UpdateWrapper<Task>()
                .eq("TaskID", task.getTaskId())
                .eq("version", task.getVersion())
                .setSql("version = version + 1"));
        if (claimed == 0) {
            return false;
        }

        List<TaskUpdates> nodeEvents = taskUpdatesMapper.selectList(new QueryWrapper<TaskUpdates>()
                .eq("TaskID", task.getTaskId())
                .in("UpdateType", TaskUpdateType.CONTACTED.getDbValue(),
                        TaskUpdateType.PICKED_UP.getDbValue(),
                        TaskUpdateType.DELIVERED.getDbValue(),
                        TaskUpdateType.AUTO_ADVANCE.getDbValue())
                .orderByDesc("UpdateTime"));

        int nodeReached = 0;
        Date anchor;
        if (nodeEvents.isEmpty()) {
            TaskAcceptRecords checked = taskAcceptRecordsMapper.selectOne(new QueryWrapper<TaskAcceptRecords>()
                    .eq("taskId", task.getTaskId())
                    .eq("status", AcceptStatus.CHECKED.getDbValue())
                    .last("LIMIT 1"));
            anchor = checked != null ? checked.getAdoptTime() : null;
        } else {
            anchor = nodeEvents.get(0).getUpdateTime();
            nodeReached = nodeEvents.stream()
                    .map(TaskUpdates::getNodeIndex)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
        }

        int action = AutoAdvanceJudge.judge(nodeReached, anchor, new Date(), nodeHours, completeHours);
        if (action > 0) {
            TaskUpdates updates = TaskUpdates.builder()
                    .taskId(task.getTaskId())
                    .userId(0L)
                    .updateType(TaskUpdateType.AUTO_ADVANCE)
                    .nodeIndex(action)
                    .updateDescription("节点【" + action + "】超时未打卡，系统自动推进至下一履约节点")
                    .updateTime(new Date())
                    .build();
            taskUpdatesMapper.insert(updates);
            log.info("任务 {} 节点超时，自动推进至节点 {}", task.getTaskId(), action);
            return true;
        } else if (action == -1) {
            taskMapper.update(null, new UpdateWrapper<Task>()
                    .eq("TaskID", task.getTaskId())
                    .set("STATUS", TaskStatus.COMPLETED.getDbValue()));
            log.info("任务 {} 完成确认超时，自动置 COMPLETED", task.getTaskId());
            return true;
        }
        return false;
    }
}
```

> 说明：置 COMPLETED 用 `UpdateWrapper`（不经过实体），避免把内存中未递增的 `version` 写回；claim 用 `SET version=version+1` 原子抢占。

- [ ] **Step 4: 运行测试，确认全绿**

Run: `mvn -q -Dtest=TaskAutoAdvanceTest,AutoAdvanceJudgeTest test`
Expected: `BUILD SUCCESS`，3 + 7 = 10 例全 PASS。

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/lz/task/TaskAutoAdvanceTest.java src/main/java/com/lz/task/TaskAutoAdvance.java
git commit -m "feat: 新增 TaskAutoAdvance 定时任务（分页扫描+乐观锁 claim+双规则推进），含接线单测"
```

---

### Task 6: `application.yml` 配置

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 追加配置**

在 `app.auto-review` 块之后追加：

```yaml
  auto-advance:
    enabled: true
    # 节点超时未打卡的小时数（演示时可调小快速触发）
    node-hours: 6
    # 三节点完成后发布者未确认完成的小时数
    complete-hours: 24
    cron: "0 */5 * * * ?"
```

- [ ] **Step 2: 编译验证（配置不参与编译，确认 yml 缩进合法）**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "chore: 新增 app.auto-advance 定时任务配置（enabled/node-hours/complete-hours/cron）"
```

---

### Task 7: 全量集成自测

- [ ] **Step 1: 全量单测回归（不含已知失败的 MQ/DB 集成测试）**

Run: `mvn -q -Dtest=AutoAdvanceJudgeTest,TaskAutoAdvanceTest,TaskServiceImplHallSearchTest,CreditScoreServiceTest,SimpleCreditCalculatorTest test`
Expected: `BUILD SUCCESS`，2 + 3 + 2 + 3 + 6 = 16 例全绿。

- [ ] **Step 2: 编译**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: 重启后端**

停旧后端（TaskStop + 杀端口 80 进程），`mvn spring-boot:run` 后台启动，确认 `Started Application`。

- [ ] **Step 4: HTTP 造数验证防断流（利用已有 m2test01 账号或新注册）**

用 JDBC 直接造数加速验证（把 `app.auto-advance.node-hours` 演示期临时调小，或直接用历史时间造数）：
1. 造一条 ACCEPTED 任务：`INSERT INTO task (OwnerID, ReceiverID, Description, StartTime, EndTime, TaskType, STATUS) VALUES (3, 2, 'M3防断流测试', NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), 1, 'ACCEPTED')`；并写一条 `taskacceptrecords`（AccepterId=2, taskId=该任务, status='Checked', adoptTime=NOW()）。
2. 把该任务的接单确认时间改旧：`UPDATE taskacceptrecords SET adoptTime = DATE_SUB(NOW(), INTERVAL 10 HOUR) WHERE ...`。
3. 等一次定时任务周期（或手动触发），验证 `taskupdates` 出现 `UpdateType='AUTO_ADVANCE'`、`NodeIndex=1` 的记录。
4. 重复推进至 `NodeIndex=3`（可连改 adoptTime/锚点）后，再验证完成超时 → 任务 `STATUS='COMPLETED'`。
- [ ] **Step 5: 清理造数数据**：`DELETE FROM task WHERE Description LIKE 'M3防断流测试%'`（连同其 taskupdates/taskacceptrecords）。
