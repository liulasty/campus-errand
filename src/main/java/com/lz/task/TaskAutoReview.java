package com.lz.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lz.mapper.ReviewsMapper;
import com.lz.mapper.TaskMapper;
import com.lz.mapper.TaskUpdatesMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.Enum.TaskUpdateType;
import com.lz.pojo.entity.Reviews;
import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.TaskUpdates;
import com.lz.pojo.entity.Users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 已完成委托自动评价归档：扫描 COMPLETED 且完成超过指定时长、且无任何评价记录的委托单，
 * 自动插入一条系统中性好评，避免订单无评价悬挂。
 *
 * @author lz
 */
@Component
@Slf4j
public class TaskAutoReview {

    private static final long HOUR_MILLIS = 3600 * 1000L;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ReviewsMapper reviewsMapper;

    @Autowired
    private TaskUpdatesMapper taskUpdatesMapper;

    @Autowired
    private UsersMapper usersMapper;

    @Value("${app.auto-review.enabled:true}")
    private boolean enabled;

    @Value("${app.auto-review.delay-hours:24}")
    private long delayHours;

    @Scheduled(cron = "${app.auto-review.cron:0 13 * * * ?}")
    public void autoReviewCompletedTasks() {
        if (!enabled) {
            return;
        }
        log.info("已完成委托自动评价检查开始...");
        long deadline = System.currentTimeMillis() - delayHours * HOUR_MILLIS;

        List<Task> completedTasks = taskMapper.selectList(
                new QueryWrapper<Task>().eq("STATUS", TaskStatus.COMPLETED));

        int reviewed = 0;
        for (Task task : completedTasks) {
            Integer count = reviewsMapper.selectCount(
                    new QueryWrapper<Reviews>().eq("TaskID", task.getTaskId()));
            if (count != null && count > 0) {
                continue;
            }

            TaskUpdates lastResult = taskUpdatesMapper.selectOne(
                    new QueryWrapper<TaskUpdates>()
                            .eq("TaskID", task.getTaskId())
                            .eq("UpdateType", TaskUpdateType.RESULT.getDbValue())
                            .orderByDesc("UpdateTime")
                            .last("LIMIT 1"));
            if (lastResult == null || lastResult.getUpdateTime() == null
                    || lastResult.getUpdateTime().getTime() > deadline) {
                continue;
            }

            Users reviewer = usersMapper.selectOne(
                    new QueryWrapper<Users>().eq("Role", "ADMIN").last("LIMIT 1"));

            Reviews review = Reviews.builder()
                    .taskId(task.getTaskId())
                    .publisherId(task.getOwnerId())
                    .acceptorId(task.getReceiverId())
                    .reviewerId(reviewer != null ? reviewer.getUserId() : 0L)
                    .rating(3L)
                    .comment("系统自动评价")
                    .isApproved(true)
                    .build();
            reviewsMapper.insert(review);
            reviewed++;
            log.info("委托 {} 已完成超过 {} 小时且无评价，系统自动评价成功", task.getTaskId(), delayHours);
        }
        log.info("已完成委托自动评价检查结束，本次自动评价 {} 单", reviewed);
    }
}
