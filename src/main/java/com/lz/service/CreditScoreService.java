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
        // MyBatis-Plus 3.4.3 的 selectCount 返回 Integer，需按 null → 0 兜底后转为 long
        long accepted = Optional.ofNullable(taskAcceptRecordsMapper.selectCount(
                new QueryWrapper<TaskAcceptRecords>()
                        .eq("AccepterId", userId)
                        .eq("status", AcceptStatus.CHECKED.getDbValue())))
                .map(Integer::longValue)
                .orElse(0L);
        long completed = Optional.ofNullable(taskMapper.selectCount(
                new QueryWrapper<Task>()
                        .eq("ReceiverID", userId)
                        .eq("STATUS", TaskStatus.COMPLETED.getDbValue())))
                .map(Integer::longValue)
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
