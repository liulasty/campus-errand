package com.lz.credit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lz.credit.dto.CreditMetrics;
import com.lz.credit.strategy.SimpleCreditCalculator;
import com.lz.mapper.ReviewsMapper;
import com.lz.mapper.TaskAcceptRecordsMapper;
import com.lz.mapper.TaskMapper;
import com.lz.mapper.UsersMapper;
import com.lz.service.CreditScoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
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
    @Mock
    private UsersMapper usersMapper;

    private CreditScoreService newService() {
        return new CreditScoreService(
                reviewsMapper, taskMapper, taskAcceptRecordsMapper, usersMapper, new SimpleCreditCalculator());
    }

    @Test
    void getScore_delegatesThroughCalculator() {
        when(taskAcceptRecordsMapper.selectCount(any(QueryWrapper.class))).thenReturn(2);
        when(taskMapper.selectCount(any(QueryWrapper.class))).thenReturn(1);
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

    @Test
    void recomputeAndSave_computesAndWritesScore() {
        when(taskAcceptRecordsMapper.selectCount(any(QueryWrapper.class))).thenReturn(2);
        when(taskMapper.selectCount(any(QueryWrapper.class))).thenReturn(1);
        when(reviewsMapper.avgRatingByAcceptor(9L)).thenReturn(4.8);

        newService().recomputeAndSave(9L);

        verify(usersMapper).update(isNull(), any(UpdateWrapper.class));
    }
}
