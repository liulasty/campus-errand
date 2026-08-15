package com.lz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lz.Exception.MyException;
import com.lz.pojo.Enum.TaskUpdateType;
import com.lz.pojo.dto.TaskNodeDTO;
import com.lz.pojo.dto.TaskUpdateDTO;
import com.lz.pojo.entity.TaskUpdates;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.Date;

/**
 * <p>
 * 记录任务的更新情况 服务类
 * </p>
 *
 * @author lz
 * @since 2024-04-04
 */
public interface ITaskUpdatesService extends IService<TaskUpdates> {
    /**
     * 记录：将委托回退为草稿
     * @return {@code Boolean}
     */
    Boolean fallbackDraft(Long taskID) throws MyException;

    void allowPublish(Long taskId);

    void notAllowed(Long taskId);

    Boolean createNewRecord(Long taskId, TaskUpdateType auditing,
                      String dataAuditFail);


    IPage<TaskUpdates> page(Page<TaskUpdates> page, String delegateComment, String reviewStatus, Date reviewTime, Long taskId);

    /**
     * 委托任务取消发布
     *
     * @param id 同上
     */
    TaskUpdates cancelPublish(Long id);

    /**
     * 添加进度更新
     *
     * @param taskUpdateDTO
     * @return TaskUpdates
     */
    TaskUpdates addUpdate(TaskUpdateDTO taskUpdateDTO) throws MyException;

    /**
     * 添加履约节点打卡
     *
     * @param taskNodeDTO 节点打卡数据
     * @return TaskUpdates
     */
    TaskUpdates addNodeUpdate(TaskNodeDTO taskNodeDTO) throws MyException;
}