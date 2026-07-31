package com.lz.service.impl;

import com.lz.mapper.TaskMapper;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.entity.Task;
import com.lz.pojo.result.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskServiceImpl.searchPage 委托大厅 SQL 查询（searchHallPage / countHallPage）的接线单测
 *
 * @author lz
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceImplHallSearchTest {

    @Mock
    private TaskMapper taskMapper;

    private TaskServiceImpl newService() {
        TaskServiceImpl service = new TaskServiceImpl();
        // 直接按字段名注入，避免与父类 ServiceImpl.baseMapper 同型歧义
        ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        return service;
    }

    @Test
    void searchPage_delegatesToHallQueriesWithConvertedStatus() {
        when(taskMapper.countHallPage("ONGOING", null, null, null)).thenReturn(3L);
        when(taskMapper.searchHallPage("ONGOING", null, null, null, false, 10, 0))
                .thenReturn(List.of(new Task()));

        PageResult<Task> result = newService().searchPage(1, 10, null, null, null, 0, TaskStatus.ONGOING);

        assertThat(result.getTotal()).isEqualTo(3L);
        assertThat(result.getRecords()).hasSize(1);
        verify(taskMapper).countHallPage("ONGOING", null, null, null);
        verify(taskMapper).searchHallPage("ONGOING", null, null, null, false, 10, 0);
    }

    @Test
    void searchPage_nullStatus_defaultStatesAndOffsetComputed() {
        when(taskMapper.countHallPage(null, 1L, "教学楼", "取件")).thenReturn(5L);
        when(taskMapper.searchHallPage(null, 1L, "教学楼", "取件", true, 10, 10))
                .thenReturn(List.of());

        PageResult<Task> result = newService().searchPage(2, 10, "教学楼", "取件", 1L, 1, null);

        assertThat(result.getTotal()).isEqualTo(5L);
        assertThat(result.getRecords()).isEmpty();
        verify(taskMapper).searchHallPage(null, 1L, "教学楼", "取件", true, 10, 10);
    }
}
