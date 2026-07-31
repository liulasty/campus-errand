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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

        // 置 COMPLETED 分支：claim + 置状态共两次 update，且不插入 AUTO_ADVANCE
        assertThat(acted).isTrue();
        verify(taskMapper, times(2)).update(any(), any());
        verify(taskUpdatesMapper, never()).insert(any(TaskUpdates.class));
    }
}
