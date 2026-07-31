# M1 信用引擎 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为每个用户计算 0–100 信用分（均分 60% + 完成率 40%），通过现有 `GET /credit` 返回并在信用档案页展示，纯新用户默认 60。

**Architecture:** 新增 `com.lz.credit` 领域包（constant / dto / strategy），`CreditScoreService` 负责用现有 mapper 聚合三项指标（acceptedCount / completedCount / ratingAvg，3 次 SQL）→ 组装 `CreditMetrics` → 调用可插拔 `CreditCalculator` 策略计算信用分。`CreditController` 瘦身为组装 VO。纯函数算法用 JUnit5 单测覆盖（含 Mockito 服务测试）。

**Tech Stack:** Spring Boot 2.7.3 / MyBatis-Plus 3.4.3 / Lombok 1.18.30 / JUnit 5 / Mockito / Vue 2 + Element UI。

**设计依据：** `docs/M1_credit-engine-design.md`（2026-08-01 定稿）。**编码约束：** 状态值一律走枚举 `AcceptStatus.CHECKED.getDbValue()` / `TaskStatus.COMPLETED.getDbValue()`，禁止硬编码字符串；舍入统一 `Math.round`（half-up）。

**验证前置说明：** 本机 Maven 本地仓库在 `D:\CODE\mvn_repository`（非默认 `~/.m2`，`$HOME` 为空），以你平时启动后端的方式运行 `mvn`。种子账号密码未知，无法登录做鉴权接口手工测试，故自动化验证 = 单测 + 编译通过；前端页面视觉验证留待有可用账号时进行。

---

## 文件结构

**新建（后端）：**
- `src/main/java/com/lz/credit/constant/CreditConstant.java` — 信用分常量
- `src/main/java/com/lz/credit/dto/CreditMetrics.java` — 计算输入 DTO
- `src/main/java/com/lz/credit/strategy/CreditCalculator.java` — 策略接口
- `src/main/java/com/lz/credit/strategy/SimpleCreditCalculator.java` — v1 实现（均分 60% + 完成率 40%）
- `src/main/java/com/lz/service/CreditScoreService.java` — 聚合 + 调策略
- `src/test/java/com/lz/credit/SimpleCreditCalculatorTest.java` — 算法单测
- `src/test/java/com/lz/credit/CreditScoreServiceTest.java` — 服务（Mockito）单测

**修改（后端）：**
- `src/main/java/com/lz/pojo/vo/CreditProfileVO.java` — 增 `creditScore` 字段
- `src/main/java/com/lz/controller/CreditController.java` — 注入 `CreditScoreService`，聚合收拢到服务

**修改（前端）：**
- `web/src/views/user/CreditProfile.vue` — 展示信用分 + 等级文案

---

### Task 1: 信用领域基础类（常量 / DTO / 策略接口）

无行为逻辑，直接创建后编译验证。

**Files:**
- Create: `src/main/java/com/lz/credit/constant/CreditConstant.java`
- Create: `src/main/java/com/lz/credit/dto/CreditMetrics.java`
- Create: `src/main/java/com/lz/credit/strategy/CreditCalculator.java`

- [ ] **Step 1: 创建 `CreditConstant.java`**

```java
package com.lz.credit.constant;

/**
 * 信用分常量
 *
 * @author lz
 */
public final class CreditConstant {

    public static final double RATING_WEIGHT = 0.6;
    public static final double COMPLETION_WEIGHT = 0.4;
    public static final int DEFAULT_USER_SCORE = 60;
    public static final int NEUTRAL_RATING_SCORE = 60;
    public static final int NEUTRAL_COMPLETION_RATE = 100;
    public static final int CREDIT_GOOD_MIN = 60;
    public static final int CREDIT_EXCELLENT_MIN = 80;

    private CreditConstant() {
    }
}
```

- [ ] **Step 2: 创建 `CreditMetrics.java`**

```java
package com.lz.credit.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 信用分计算输入指标
 *
 * @author lz
 */
@Data
@Builder
public class CreditMetrics {

    /** 已确认接单数 */
    private Long acceptedCount;
    /** 已完成接单数 */
    private Long completedCount;
    /** 作为接收方收到的平均评分，可为 null */
    private Double ratingAvg;
}
```

- [ ] **Step 3: 创建 `CreditCalculator.java`**

```java
package com.lz.credit.strategy;

import com.lz.credit.dto.CreditMetrics;

/**
 * 信用分计算策略
 *
 * @author lz
 */
public interface CreditCalculator {

    /**
     * 输入原始指标，返回 0–100 信用分
     */
    int calculate(CreditMetrics metrics);
}
```

- [ ] **Step 4: 编译验证**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`（无编译错误）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/lz/credit/constant/CreditConstant.java src/main/java/com/lz/credit/dto/CreditMetrics.java src/main/java/com/lz/credit/strategy/CreditCalculator.java
git commit -m "feat: 新增信用分领域基础类（CreditConstant / CreditMetrics / CreditCalculator）"
```

---

### Task 2: SimpleCreditCalculator（TDD）

**Files:**
- Test: `src/test/java/com/lz/credit/SimpleCreditCalculatorTest.java`
- Create: `src/main/java/com/lz/credit/strategy/SimpleCreditCalculator.java`

- [ ] **Step 1: 写失败测试 `SimpleCreditCalculatorTest.java`**

```java
package com.lz.credit;

import com.lz.credit.dto.CreditMetrics;
import com.lz.credit.strategy.SimpleCreditCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SimpleCreditCalculator 算法单测（纯函数，无需 Spring 容器）
 *
 * @author lz
 */
class SimpleCreditCalculatorTest {

    private final SimpleCreditCalculator calculator = new SimpleCreditCalculator();

    private CreditMetrics metrics(Long accepted, Long completed, Double ratingAvg) {
        return CreditMetrics.builder()
                .acceptedCount(accepted)
                .completedCount(completed)
                .ratingAvg(ratingAvg)
                .build();
    }

    @Test
    void newUser_returnsDefaultScore() {
        assertEquals(60, calculator.calculate(metrics(0L, 0L, null)));
    }

    @Test
    void onlyRating_fullScore() {
        assertEquals(100, calculator.calculate(metrics(0L, 0L, 5.0)));
    }

    @Test
    void onlyRating_neutralCompletion() {
        assertEquals(88, calculator.calculate(metrics(0L, 0L, 4.0)));
    }

    @Test
    void acceptedButNotCompleted() {
        assertEquals(56, calculator.calculate(metrics(2L, 1L, null)));
    }

    @Test
    void fullPerformance() {
        assertEquals(100, calculator.calculate(metrics(2L, 2L, 5.0)));
    }

    @Test
    void roundingBoundary() {
        assertEquals(78, calculator.calculate(metrics(2L, 1L, 4.8)));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -q -Dtest=SimpleCreditCalculatorTest test`
Expected: `BUILD FAILURE`，报 `SimpleCreditCalculator` 不存在 / 编译失败。

- [ ] **Step 3: 实现 `SimpleCreditCalculator.java`**

```java
package com.lz.credit.strategy;

import com.lz.credit.constant.CreditConstant;
import com.lz.credit.dto.CreditMetrics;
import org.springframework.stereotype.Component;

/**
 * v1 信用分计算：均分 60% + 完成率 40%
 *
 * @author lz
 */
@Component
public class SimpleCreditCalculator implements CreditCalculator {

    @Override
    public int calculate(CreditMetrics metrics) {
        long accepted = metrics.getAcceptedCount() == null ? 0L : metrics.getAcceptedCount();
        long completed = metrics.getCompletedCount() == null ? 0L : metrics.getCompletedCount();
        Double ratingAvg = metrics.getRatingAvg();

        if (accepted == 0 && ratingAvg == null) {
            return CreditConstant.DEFAULT_USER_SCORE;
        }

        double rating = ratingAvg == null
                ? CreditConstant.NEUTRAL_RATING_SCORE
                : Math.round(ratingAvg / 5.0 * 100);
        double completion = accepted == 0
                ? CreditConstant.NEUTRAL_COMPLETION_RATE
                : Math.round((double) completed / accepted * 100);

        return (int) Math.round(CreditConstant.RATING_WEIGHT * rating
                + CreditConstant.COMPLETION_WEIGHT * completion);
    }
}
```

- [ ] **Step 4: 运行测试，确认全绿**

Run: `mvn -q -Dtest=SimpleCreditCalculatorTest test`
Expected: `BUILD SUCCESS`，6 个用例全 PASS。

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/lz/credit/SimpleCreditCalculatorTest.java src/main/java/com/lz/credit/strategy/SimpleCreditCalculator.java
git commit -m "feat: 实现 v1 信用分计算 SimpleCreditCalculator（均分60%+完成率40%），含 6 例单测"
```

---

### Task 3: CreditScoreService（TDD + Mockito）

**Files:**
- Test: `src/test/java/com/lz/credit/CreditScoreServiceTest.java`
- Create: `src/main/java/com/lz/service/CreditScoreService.java`

- [ ] **Step 1: 写失败测试 `CreditScoreServiceTest.java`**

```java
package com.lz.credit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lz.credit.dto.CreditMetrics;
import com.lz.credit.strategy.SimpleCreditCalculator;
import com.lz.mapper.ReviewsMapper;
import com.lz.mapper.TaskAcceptRecordsMapper;
import com.lz.mapper.TaskMapper;
import com.lz.service.CreditScoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * CreditScoreService 单测（Mockito，验证聚合 → 策略的接线与空值兜底）
 *
 * @author lz
 */
@ExtendWith(MockitoExtension.class)
class CreditScoreServiceTest {

    @Mock
    private ReviewsMapper reviewsMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskAcceptRecordsMapper taskAcceptRecordsMapper;

    private CreditScoreService newService() {
        return new CreditScoreService(
                reviewsMapper, taskMapper, taskAcceptRecordsMapper, new SimpleCreditCalculator());
    }

    @Test
    void getScore_delegatesThroughCalculator() {
        when(taskAcceptRecordsMapper.selectCount(any(QueryWrapper.class))).thenReturn(2L);
        when(taskMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        when(reviewsMapper.avgRatingByAcceptor(9L)).thenReturn(4.8);

        assertEquals(78, newService().getScore(9L));
    }

    @Test
    void loadMetrics_nullCounts_defaultToZeroAndNewUser() {
        when(taskAcceptRecordsMapper.selectCount(any(QueryWrapper.class))).thenReturn(null);
        when(taskMapper.selectCount(any(QueryWrapper.class))).thenReturn(null);
        when(reviewsMapper.avgRatingByAcceptor(9L)).thenReturn(null);

        CreditMetrics metrics = newService().loadMetrics(9L);
        assertEquals(0L, metrics.getAcceptedCount());
        assertEquals(0L, metrics.getCompletedCount());
        assertEquals(60, newService().getScore(9L));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -q -Dtest=CreditScoreServiceTest test`
Expected: `BUILD FAILURE`，报 `CreditScoreService` 不存在。

- [ ] **Step 3: 实现 `CreditScoreService.java`**

```java
package com.lz.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lz.credit.dto.CreditMetrics;
import com.lz.credit.strategy.CreditCalculator;
import com.lz.mapper.ReviewsMapper;
import com.lz.mapper.TaskAcceptRecordsMapper;
import com.lz.mapper.TaskMapper;
import com.lz.pojo.Enum.AcceptStatus;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.TaskAcceptRecords;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户信用分服务：聚合原始指标 → 调策略计算信用分
 *
 * @author lz
 */
@Service
public class CreditScoreService {

    private final ReviewsMapper reviewsMapper;
    private final TaskMapper taskMapper;
    private final TaskAcceptRecordsMapper taskAcceptRecordsMapper;
    private final CreditCalculator creditCalculator;

    public CreditScoreService(ReviewsMapper reviewsMapper,
                              TaskMapper taskMapper,
                              TaskAcceptRecordsMapper taskAcceptRecordsMapper,
                              CreditCalculator creditCalculator) {
        this.reviewsMapper = reviewsMapper;
        this.taskMapper = taskMapper;
        this.taskAcceptRecordsMapper = taskAcceptRecordsMapper;
        this.creditCalculator = creditCalculator;
    }

    /** 聚合用户信用指标（3 次 SQL） */
    public CreditMetrics loadMetrics(Long userId) {
        long accepted = Optional.ofNullable(taskAcceptRecordsMapper.selectCount(
                new QueryWrapper<TaskAcceptRecords>()
                        .eq("AccepterId", userId)
                        .eq("status", AcceptStatus.CHECKED.getDbValue())))
                .orElse(0L);
        long completed = Optional.ofNullable(taskMapper.selectCount(
                new QueryWrapper<Task>()
                        .eq("ReceiverID", userId)
                        .eq("STATUS", TaskStatus.COMPLETED.getDbValue())))
                .orElse(0L);
        Double ratingAvg = reviewsMapper.avgRatingByAcceptor(userId);

        return CreditMetrics.builder()
                .acceptedCount(accepted)
                .completedCount(completed)
                .ratingAvg(ratingAvg)
                .build();
    }

    /** 计算用户信用分（0–100） */
    public int getScore(Long userId) {
        return creditCalculator.calculate(loadMetrics(userId));
    }

    /** 基于已加载指标计算信用分 */
    public int getScore(CreditMetrics metrics) {
        return creditCalculator.calculate(metrics);
    }
}
```

- [ ] **Step 4: 运行测试，确认全绿**

Run: `mvn -q -Dtest=CreditScoreServiceTest,SimpleCreditCalculatorTest test`
Expected: `BUILD SUCCESS`，8 个用例全 PASS。

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/lz/credit/CreditScoreServiceTest.java src/main/java/com/lz/service/CreditScoreService.java
git commit -m "feat: 新增 CreditScoreService 聚合信用指标并调策略，含 Mockito 单测"
```

---

### Task 4: CreditProfileVO 字段 + CreditController 接线

**Files:**
- Modify: `src/main/java/com/lz/pojo/vo/CreditProfileVO.java`
- Modify: `src/main/java/com/lz/controller/CreditController.java`

- [ ] **Step 1: `CreditProfileVO.java` 增加 `creditScore` 字段**

在 `private List<CreditReviewVO> reviewList;` 之后追加：

```java
    /** 信用分（0–100，可空，前端兜底 60） */
    private Integer creditScore;
```

- [ ] **Step 2: 重写 `CreditController.java`**

整文件替换为：

```java
package com.lz.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lz.credit.dto.CreditMetrics;
import com.lz.mapper.ReviewsMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.entity.Reviews;
import com.lz.pojo.entity.Users;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.CreditProfileVO;
import com.lz.pojo.vo.CreditReviewVO;
import com.lz.service.CreditScoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户信用档案
 *
 * @author lz
 */
@RestController
@RequestMapping("/credit")
@Slf4j
@Api(tags = "用户信用档案")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class CreditController {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private ReviewsMapper reviewsMapper;

    @Autowired
    private CreditScoreService creditScoreService;

    @GetMapping
    @ApiOperation("当前用户信用档案")
    public Result<?> credit() {
        Users user = usersMapper.getByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName());
        Long userId = user.getUserId();

        CreditMetrics metrics = creditScoreService.loadMetrics(userId);

        Long reviewCount = (long) reviewsMapper.selectCount(new QueryWrapper<Reviews>()
                .eq("AcceptorID", userId));
        Long goodCount = (long) reviewsMapper.selectCount(new QueryWrapper<Reviews>()
                .eq("AcceptorID", userId).ge("Rating", 4));
        List<CreditReviewVO> list = reviewsMapper.selectReviewsByAcceptor(userId);
        double goodRate = reviewCount > 0 ? Math.round(goodCount * 10000.0 / reviewCount) / 100.0 : 100.0;

        CreditProfileVO vo = CreditProfileVO.builder()
                .acceptTotal(metrics.getAcceptedCount())
                .completedTotal(metrics.getCompletedCount())
                .ratingAvg(metrics.getRatingAvg())
                .goodRate(goodRate)
                .reviewCount(reviewCount)
                .creditScore(creditScoreService.getScore(metrics))
                .reviewList(list)
                .build();
        return Result.success(vo);
    }
}
```

> 说明：`acceptedCount / completedCount / ratingAvg` 的聚合已收拢到 `CreditScoreService`，控制器不再直接注入 `TaskMapper` / `TaskAcceptRecordsMapper`；`reviewsMapper` 仍用于好评率统计。

- [ ] **Step 3: 编译验证**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`（无编译错误，无未使用注入）

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/lz/pojo/vo/CreditProfileVO.java src/main/java/com/lz/controller/CreditController.java
git commit -m "feat: 信用档案接口返回 creditScore，聚合逻辑收拢至 CreditScoreService"
```

---

### Task 5: 前端信用档案页展示信用分

**Files:**
- Modify: `web/src/views/user/CreditProfile.vue`

- [ ] **Step 1: 模板新增信用分 Hero 卡片**

在 `<div v-loading="loading">` 内、`<el-row :gutter="20" v-if="profile">` **之前**插入：

```html
                <el-row :gutter="20" v-if="profile" class="credit-hero-row">
                    <el-col :span="24">
                        <el-card shadow="never" class="credit-hero">
                            <div class="hero-score">{{ displayScore }}</div>
                            <div class="hero-label">信用分 · {{ displayLevel }}</div>
                        </el-card>
                    </el-col>
                </el-row>
```

- [ ] **Step 2: script 增加 `computed`**

在 `data()` 之后、`created()` 之前插入：

```js
        computed: {
            displayScore() {
                return this.profile && this.profile.creditScore != null ? this.profile.creditScore : 60
            },
            displayLevel() {
                const s = this.displayScore
                if (s < 60) return '待提升'
                if (s < 80) return '良好'
                return '优秀'
            }
        },
```

- [ ] **Step 3: style 增加 Hero 样式**

在 `.stat-label { ... }` 规则之后追加：

```css
    .credit-hero-row {
        margin-bottom: 10px;
    }

    .credit-hero {
        text-align: center;
        background: linear-gradient(135deg, #409EFF, #66b1ff);
        color: #fff;
        border: none;
    }

    .hero-score {
        font-size: 44px;
        font-weight: 700;
    }

    .hero-label {
        color: rgba(255, 255, 255, 0.9);
        font-size: 14px;
        margin-top: 4px;
    }
```

- [ ] **Step 4: 前端构建验证**

Run: `cd web && npm run build`
Expected: 构建完成无报错（vue-cli 输出 `Build complete` 或类似；无 ESLint 致命错误）。

> 说明：本机种子账号密码未知，无法登录查看页面实际效果；此步仅验证编译通过。页面视觉与边界（含 60 归「良好」、null 兜底 60）待有可用账号时手工复核。

- [ ] **Step 5: Commit**

```bash
git add web/src/views/user/CreditProfile.vue
git commit -m "feat: 信用档案页展示信用分与等级文案，null 兜底 60"
```

---

## 全量回归（收尾）

- [ ] Run: `mvn -q -Dtest=SimpleCreditCalculatorTest,CreditScoreServiceTest test`
  Expected: `BUILD SUCCESS`，8 用例全绿。
- [ ] Run: `mvn -q compile`
  Expected: `BUILD SUCCESS`。
- [ ] 确认无硬编码状态字符串：`grep -rn "\"Checked\"\|\"COMPLETED\"" src/main/java/com/lz/credit src/main/java/com/lz/service/CreditScoreService.java` 无输出。
- [ ] 确认未改动任何数据库表结构、未新增 `application.yml` 配置。
