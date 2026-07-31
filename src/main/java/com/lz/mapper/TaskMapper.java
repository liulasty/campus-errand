package com.lz.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.dto.TaskCountDTO;
import com.lz.pojo.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 存储任务相关信息 Mapper 接口
 * </p>
 *
 * @author lz
 * @since 2024-04-04
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
    
    List<TaskCountDTO> selectTaskTypeCountTop5();

    /**
     * 查找任务统计信息
     */
    void selectTasksStats();


    /**
     * 获取每周查询委托数量
     *
     * @param key 钥匙
     *
     * @return {@code Integer}
     */
    Integer getTasksWeeklyCount( String key);

    /**
     * 获取任务每月查询委托数量
     *
     * @param key 钥匙
     *
     * @return {@code Integer}
     */
    Integer getTasksMonthlyCount(String key);


    /**
     * 今日查询委托数量
     *
     * @param key 钥匙
     *
     * @return {@code Integer}
     */
    Integer getTasksTodayCount(String key);

    @Insert("INSERT INTO task(CreatedAt, Description, OwnerId, status, " +
            "TaskType,Location) VALUES" +
            "(#{createdAt}, #{description}, #{ownerId}, #{status}, #{taskType}, #{location})")
    @Options(useGeneratedKeys = true, keyProperty = "taskId")
    int insert(Task task);

    Long getPublishedTotal(Long id);

    Long getAcceptedTotal(Long id);

    Long getOverdueTotal(Long id);

    Long getCanceledTotal(Long id);

    List<Task> queryOngoingTasks(Date now);

    TaskStatus getTaskStatus(Long id);

    /**
     * 按状态分组统计委托数量，返回 [{name: 状态dbValue, value: 数量}]
     */
    List<Map<String, Object>> countGroupByStatus();

    /**
     * 按委托类型分组统计，返回 [{typeId, value}]
     */
    List<Map<String, Object>> countGroupByType();

    /**
     * 大厅分页查询：JOIN users 按发布者信用分排序（只取当前页，零内存压力）
     *
     * @param statusDb     状态 dbValue，null 时用默认三态
     * @param taskTypeId   委托类型（可空）
     * @param location     地点（可空）
     * @param description  描述模糊（可空）
     * @param startTimeAsc 次键 StartTime 是否升序
     * @param pageSize     页大小
     * @param offset       偏移
     */
    List<Task> searchHallPage(@Param("statusDb") String statusDb,
            @Param("taskTypeId") Long taskTypeId,
            @Param("location") String location,
            @Param("description") String description,
            @Param("startTimeAsc") boolean startTimeAsc,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    /**
     * 大厅过滤后的总条数
     */
    Long countHallPage(@Param("statusDb") String statusDb,
            @Param("taskTypeId") Long taskTypeId,
            @Param("location") String location,
            @Param("description") String description);
}