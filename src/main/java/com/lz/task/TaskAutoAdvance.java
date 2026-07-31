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
