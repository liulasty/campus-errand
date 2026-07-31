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
