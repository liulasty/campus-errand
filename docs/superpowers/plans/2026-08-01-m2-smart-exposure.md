# M2 智能曝光 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 任务大厅按发布者信用分降序排序，并在任务卡片展示发布者信用分（badge），复用 M1 `CreditScoreService`。

**Architecture:** `TaskServiceImpl.searchPage(int...)` 从 SQL 层 StartTime 排序改为内存排序：过滤查询（`LIMIT 1000` 硬上限）→ 去重 OwnerID 逐个调 `CreditScoreService.getScore` → `(credit DESC, StartTime DESC, TaskID DESC)` 排序 → 内存分页切片。排序/取分/分页抽成包私有纯方法 `applyCreditSortAndPage`，可直接 JUnit 单测（避开 TaskServiceImpl 多依赖 + `baseMapper`/`taskMapper` 同型字段导致的 `@InjectMocks` 歧义）。前端抽 `creditLevel.js` 工具函数（兜底+等级+配色），大厅卡片与 M1 信用档案页共用。

**Tech Stack:** Spring Boot 2.7.3 / MyBatis-Plus 3.4.3 / JUnit 5 / AssertJ / Vue 2 + Element UI。

**设计依据：** `docs/M2_smart-exposure-design.md`（2026-08-01 定稿）。**已知偏差说明：**
- mybatis-plus 3.4.3 `selectCount` 返回 `Integer`（非 `Long`）。
- `CreditScoreService.getScore` 返回基本类型 `int`，**永不为 null**；设计中「null 信用分」场景实际由「任务 `OwnerID` 为 null → `ownerCredit` 恒 null → 兜底 60」触发。
- 测试不走 `@InjectMocks` 整个 `TaskServiceImpl`（其 `taskMapper` 字段与父类 `ServiceImpl.baseMapper` 同型，`@InjectMocks` 注入有歧义风险），改为直接测包私有纯方法。

**验证前置说明：** 本机 Maven 本地仓库在 `D:\CODE\mvn_repository`；以你平时启动后端的方式运行 `mvn`。种子账号密码未知，无法登录做鉴权接口手工测试，故自动化验证 = 单测 + 编译 + 前端 build。

---

## 文件结构

**修改（后端）：**
- `src/main/java/com/lz/pojo/entity/Task.java` — 加 `ownerCredit` 瞬态字段
- `src/main/java/com/lz/service/impl/TaskServiceImpl.java` — 注入 `CreditScoreService`，`searchPage` 改内存排序/分页，抽 `applyCreditSortAndPage` + `buildSearchWrapper`

**新建（后端测试）：**
- `src/test/java/com/lz/service/impl/TaskServiceImplSearchPageTest.java` — `applyCreditSortAndPage` 纯逻辑 7 例

**新建（前端）：**
- `web/src/utils/creditLevel.js` — 信用分兜底 / 等级 / 配色工具

**修改（前端）：**
- `web/src/views/user/ViewOnGoingList.vue` — 表格加「信用分」列（badge + tooltip）
- `web/src/views/user/CreditProfile.vue` — computed 复用 `creditLevel.js`

---

### Task 1: Task 实体加 `ownerCredit` 瞬态字段

**Files:**
- Modify: `src/main/java/com/lz/pojo/entity/Task.java`

- [ ] **Step 1: 加字段**

在 `Task.java` 的 `private TaskStatus status;`（第 82 行）之后、类结束大括号前追加：

```java

    @ApiModelProperty(value = "发布者信用分（瞬态，不落库，大厅展示用）")
    @TableField(exist = false)
    private Integer ownerCredit;
```

（`@ApiModelProperty`、`@TableField` 已 import，无需新增 import。）

- [ ] **Step 2: 编译验证**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/lz/pojo/entity/Task.java
git commit -m "feat: Task 实体新增 ownerCredit 瞬态字段（不落库，大厅展示用）"
```

---

### Task 2: `TaskServiceImpl.searchPage` 内存排序 + `applyCreditSortAndPage`（TDD）

**Files:**
- Test: `src/test/java/com/lz/service/impl/TaskServiceImplSearchPageTest.java`
- Modify: `src/main/java/com/lz/service/impl/TaskServiceImpl.java`

- [ ] **Step 1: 写失败测试**

创建 `src/test/java/com/lz/service/impl/TaskServiceImplSearchPageTest.java`：

```java
package com.lz.service.impl;

import com.lz.pojo.entity.Task;
import com.lz.pojo.result.PageResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskServiceImpl.applyCreditSortAndPage 纯逻辑单测
 *
 * @author lz
 */
class TaskServiceImplSearchPageTest {

    private final TaskServiceImpl service = new TaskServiceImpl();

    private Task task(Long id, Long ownerId, Date startTime) {
        return Task.builder().taskId(id).ownerId(ownerId).startTime(startTime).build();
    }

    private Date time(long millis) {
        return new Date(millis);
    }

    /** 计数打点 + 按 map 返回信用分，用于校验去重后调用次数 */
    private static class CountingScorer implements Function<Long, Integer> {
        int calls = 0;
        final Map<Long, Integer> scores;

        CountingScorer(Map<Long, Integer> scores) {
            this.scores = scores;
        }

        @Override
        public Integer apply(Long ownerId) {
            calls++;
            return scores.get(ownerId);
        }
    }

    private List<Task> list(Task... tasks) {
        return new ArrayList<>(Arrays.asList(tasks));
    }

    @Test
    void sortByCreditDesc() {
        Map<Long, Integer> scores = new LinkedHashMap<>();
        scores.put(1L, 100);
        scores.put(2L, 60);
        scores.put(3L, 40);
        CountingScorer scorer = new CountingScorer(scores);

        List<Task> tasks = list(task(1L, 1L, time(100)), task(2L, 2L, time(200)), task(3L, 3L, time(300)));
        PageResult<Task> result = service.applyCreditSortAndPage(tasks, 100L, 0, 1, 10, scorer);

        assertThat(result.getRecords()).extracting(Task::getOwnerId).containsExactly(1L, 2L, 3L);
        assertThat(scorer.calls).isEqualTo(3);
    }

    @Test
    void sameCreditSameTime_fallsBackToTaskIdDesc() {
        CountingScorer scorer = new CountingScorer(new LinkedHashMap<>(Map.of(1L, 60, 2L, 60)));
        List<Task> tasks = list(task(1L, 1L, time(100)), task(2L, 2L, time(100)));
        PageResult<Task> result = service.applyCreditSortAndPage(tasks, 100L, 0, 1, 10, scorer);
        assertThat(result.getRecords()).extracting(Task::getTaskId).containsExactly(2L, 1L);
    }

    @Test
    void paginateAcrossPages_noOverlapNoLoss() {
        List<Task> tasks = new ArrayList<>();
        Map<Long, Integer> scores = new LinkedHashMap<>();
        for (long i = 1; i <= 20; i++) {
            tasks.add(task(i, i, time(i)));
            scores.put(i, 60);
        }
        CountingScorer scorer = new CountingScorer(scores);

        PageResult<Task> page1 = service.applyCreditSortAndPage(new ArrayList<>(tasks), 20L, 0, 1, 10, scorer);
        PageResult<Task> page2 = service.applyCreditSortAndPage(new ArrayList<>(tasks), 20L, 0, 2, 10, scorer);

        assertThat(page1.getRecords()).hasSize(10);
        assertThat(page2.getRecords()).hasSize(10);
        List<Long> p1 = page1.getRecords().stream().map(Task::getTaskId).collect(Collectors.toList());
        List<Long> p2 = page2.getRecords().stream().map(Task::getTaskId).collect(Collectors.toList());
        assertThat(p1).doesNotContainAnyElementsOf(p2);
        assertThat(page1.getTotal()).isEqualTo(20L);
    }

    @Test
    void ownerCreditFilledFromScorer() {
        CountingScorer scorer = new CountingScorer(new LinkedHashMap<>(Map.of(1L, 88)));
        List<Task> tasks = list(task(1L, 1L, time(100)));
        PageResult<Task> result = service.applyCreditSortAndPage(tasks, 100L, 0, 1, 10, scorer);
        assertThat(result.getRecords().get(0).getOwnerCredit()).isEqualTo(88);
    }

    @Test
    void nullStartTime_sinksToBottom() {
        CountingScorer scorer = new CountingScorer(new LinkedHashMap<>(Map.of(1L, 60, 2L, 60)));
        List<Task> tasks = list(task(1L, 1L, null), task(2L, 2L, time(100)));
        PageResult<Task> result = service.applyCreditSortAndPage(tasks, 100L, 0, 1, 10, scorer);
        assertThat(result.getRecords()).extracting(Task::getTaskId).containsExactly(2L, 1L);
    }

    @Test
    void nullOwnerId_treatedAsDefaultScoreAndNotScored() {
        CountingScorer scorer = new CountingScorer(new LinkedHashMap<>(Map.of(1L, 40, 2L, 40)));
        // ownerId=null 的任务：ownerCredit 保持 null → 按默认 60 参与排序；不触发 getScore
        List<Task> tasks = list(task(1L, null, time(100)), task(2L, 1L, time(100)), task(3L, 2L, time(100)));
        PageResult<Task> result = service.applyCreditSortAndPage(tasks, 100L, 0, 1, 10, scorer);
        // null-owner(60) 最前；owner1/owner2(40) 并列 → TaskID DESC → 3, 2
        assertThat(result.getRecords()).extracting(Task::getTaskId).containsExactly(1L, 3L, 2L);
        assertThat(result.getRecords().get(0).getOwnerCredit()).isNull();
        assertThat(scorer.calls).isEqualTo(2);
    }

    @Test
    void fewerThanPageSize_returnsAll() {
        List<Task> tasks = new ArrayList<>();
        Map<Long, Integer> scores = new LinkedHashMap<>();
        for (long i = 1; i <= 5; i++) {
            tasks.add(task(i, i, time(i)));
            scores.put(i, 60);
        }
        PageResult<Task> result = service.applyCreditSortAndPage(tasks, 5L, 0, 1, 10, new CountingScorer(scores));
        assertThat(result.getRecords()).hasSize(5);
        assertThat(result.getTotal()).isEqualTo(5L);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -q -Dtest=TaskServiceImplSearchPageTest test`
Expected: `BUILD FAILURE` — 编译错误，`applyCreditSortAndPage` 方法不存在。

- [ ] **Step 3: 实现 `TaskServiceImpl` 改动**

**(a) 新增 import**（在现有 import 区追加）：

```java
import java.util.Set;
import java.util.function.Function;

import com.lz.credit.constant.CreditConstant;
import com.lz.service.CreditScoreService;
```

**(b) 新增注入字段**（在 `private TaskMapper taskMapper;` 之后）：

```java
    @Autowired
    private CreditScoreService creditScoreService;
```

**(c) 替换 `searchPage(int...)` 方法**（原 `TaskServiceImpl.java:546-575`），并新增两个辅助方法。整段替换为：

```java
    @Override
    public PageResult<Task> searchPage(int pageNum, int pageSize,
            String location, String description,
            Long taskTypeId, Integer queryRules,
            TaskStatus status) {
        long total = taskMapper.selectCount(
                buildSearchWrapper(location, description, taskTypeId, status));
        List<Task> tasks = taskMapper.selectList(
                buildSearchWrapper(location, description, taskTypeId, status).last("LIMIT 1000"));
        return applyCreditSortAndPage(tasks, total, queryRules, pageNum, pageSize,
                creditScoreService::getScore);
    }

    private QueryWrapper<Task> buildSearchWrapper(String location, String description,
            Long taskTypeId, TaskStatus status) {
        QueryWrapper<Task> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("Status", status);
        } else {
            wrapper.in("Status", TaskStatus.ACCEPTED,
                    TaskStatus.ONGOING, TaskStatus.COMPLETED);
        }
        if (taskTypeId != null) {
            wrapper.eq("TaskType", taskTypeId);
        }
        if (location != null && !"".equals(location)) {
            wrapper.eq("Location", location);
        }
        if (description != null && !"".equals(description)) {
            wrapper.like("Description", description);
        }
        return wrapper;
    }

    /** 内存排序 + 分页（纯逻辑，便于单测）。scoreProvider 对去重后的 owner 各调用一次；返回 null 视为默认 60。 */
    PageResult<Task> applyCreditSortAndPage(List<Task> tasks, long total, Integer queryRules,
            int pageNum, int pageSize, Function<Long, Integer> scoreProvider) {
        Set<Long> ownerIds = tasks.stream()
                .map(Task::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Integer> scoreByOwner = new HashMap<>();
        for (Long ownerId : ownerIds) {
            Integer score = scoreProvider.apply(ownerId);
            scoreByOwner.put(ownerId,
                    score == null ? CreditConstant.DEFAULT_USER_SCORE : score);
        }
        for (Task task : tasks) {
            if (task.getOwnerId() != null) {
                task.setOwnerCredit(scoreByOwner.get(task.getOwnerId()));
            }
        }

        boolean timeAsc = queryRules != null && queryRules != 0;
        tasks.sort((a, b) -> {
            int c = Integer.compare(creditOf(b), creditOf(a));
            if (c != 0) {
                return c;
            }
            int t = compareStartTime(a.getStartTime(), b.getStartTime(), timeAsc);
            if (t != 0) {
                return t;
            }
            return Long.compare(b.getTaskId(), a.getTaskId());
        });

        int from = Math.min((pageNum - 1) * pageSize, tasks.size());
        int to = Math.min(from + pageSize, tasks.size());
        List<Task> records = new ArrayList<>(tasks.subList(from, to));
        return new PageResult<>(total, records);
    }

    private int creditOf(Task task) {
        return task.getOwnerCredit() == null
                ? CreditConstant.DEFAULT_USER_SCORE : task.getOwnerCredit();
    }

    private int compareStartTime(Date a, Date b, boolean asc) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int c = a.compareTo(b);
        return asc ? c : -c;
    }
```

> 说明：`selectCount` 返回 `Integer`（mybatis-plus 3.4.3），赋给 `long total` 自动拆箱加宽；`selectCount` 用不含 `last("LIMIT...")` 的独立 wrapper，避免 COUNT 带 LIMIT。

- [ ] **Step 4: 运行测试，确认全绿**

Run: `mvn -q -Dtest=TaskServiceImplSearchPageTest test`
Expected: `BUILD SUCCESS`，7 例全 PASS。

- [ ] **Step 5: 编译 + 全量 M1 回归**

Run: `mvn -q -Dtest=TaskServiceImplSearchPageTest,SimpleCreditCalculatorTest,CreditScoreServiceTest test`
Expected: `BUILD SUCCESS`，15 例全 PASS（7 + 6 + 2）。

- [ ] **Step 6: Commit**

```bash
git add src/test/java/com/lz/service/impl/TaskServiceImplSearchPageTest.java src/main/java/com/lz/service/impl/TaskServiceImpl.java
git commit -m "feat: 任务大厅按发布者信用分内存排序分页，复用 CreditScoreService，含 7 例单测"
```

---

### Task 3: 前端信用分工具函数 + 大厅卡片 badge

**Files:**
- Create: `web/src/utils/creditLevel.js`
- Modify: `web/src/views/user/ViewOnGoingList.vue`

- [ ] **Step 1: 创建 `web/src/utils/creditLevel.js`**

```js
// 信用分工具：兜底 / 等级 / 配色（阈值与后端 CreditConstant 一致）
export function creditScore(value) {
    return value == null ? 60 : Number(value)
}

export function creditLevel(value) {
    const s = creditScore(value)
    if (s < 60) return '待提升'
    if (s < 80) return '良好'
    return '优秀'
}

export function creditColor(value) {
    const s = creditScore(value)
    if (s < 60) return 'info'
    if (s < 80) return 'primary'
    return 'success'
}
```

- [ ] **Step 2: `ViewOnGoingList.vue` 引入工具函数**

在 script 区（现有 import 附近，如 `import { listViewOnGoingList ... }` 上方）追加：

```js
    import { creditScore, creditLevel, creditColor } from '@/utils/creditLevel'
```

- [ ] **Step 3: 表格加「信用分」列**

在 `<el-table-column label="发布者ID" align="center" prop="ownerId" width="100" />` 之后、`<el-table-column label="委托类型" ...>` 之前插入：

```html
                <el-table-column label="信用分" align="center" width="110">
                    <template slot-scope="scope">
                        <el-tooltip :content="'信用等级：' + creditLevel(scope.row.ownerCredit)" placement="top">
                            <el-tag :type="creditColor(scope.row.ownerCredit)" size="small">{{ creditScore(scope.row.ownerCredit) }}</el-tag>
                        </el-tooltip>
                    </template>
                </el-table-column>
```

- [ ] **Step 4: 前端构建验证**

Run: `cd web && npm run build`
Expected: 构建完成无报错（vue-cli 输出 `Build complete`，无 ESLint 致命错误）。

- [ ] **Step 5: Commit**

```bash
git add web/src/utils/creditLevel.js web/src/views/user/ViewOnGoingList.vue
git commit -m "feat: 任务大厅卡片展示发布者信用分 badge（含等级/配色工具函数）"
```

---

### Task 4: 信用档案页复用信用分工具函数

**Files:**
- Modify: `web/src/views/user/CreditProfile.vue`

- [ ] **Step 1: 引入工具函数**

在 script 区现有 `import { getCreditProfile } from '@/api/'` 之后追加：

```js
    import { creditScore, creditLevel } from '@/utils/creditLevel'
```

- [ ] **Step 2: 替换 `computed`**

将现有 `computed` 块替换为：

```js
        computed: {
            displayScore() {
                return creditScore(this.profile && this.profile.creditScore)
            },
            displayLevel() {
                return creditLevel(this.profile && this.profile.creditScore)
            }
        },
```

- [ ] **Step 3: 前端构建验证**

Run: `cd web && npm run build`
Expected: 构建完成无报错。

- [ ] **Step 4: Commit**

```bash
git add web/src/views/user/CreditProfile.vue
git commit -m "refactor: 信用档案页信用分等级逻辑复用 creditLevel 工具函数"
```

---

## 全量回归（收尾）

- [ ] Run: `mvn -q -Dtest=TaskServiceImplSearchPageTest,SimpleCreditCalculatorTest,CreditScoreServiceTest test`
  Expected: `BUILD SUCCESS`，15 例全绿。
- [ ] Run: `mvn -q compile`
  Expected: `BUILD SUCCESS`。
- [ ] Run: `cd web && npm run build`
  Expected: 构建成功。
- [ ] 确认 `searchPage(TaskPageDTO)` 重载与管理端 `TaskAdminController` 未改动（`git diff --name-only` 不含其调用链文件）。
- [ ] 确认无 `application.yml` / SQL 表结构改动。
