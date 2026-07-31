package com.lz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lz.Exception.MyException;
import com.lz.mapper.TaskMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.dto.TaskNodeDTO;
import com.lz.pojo.dto.TaskUpdateDTO;
import com.lz.pojo.Enum.TaskUpdateType;
import com.lz.pojo.entity.Task;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.entity.TaskUpdates;
import com.lz.mapper.TaskUpdatesMapper;
import com.lz.pojo.entity.Users;
import com.lz.service.ITaskUpdatesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;
import java.util.Objects;

/**
 * <p>
 * 记录任务的更新情况 服务实现类
 * </p>
 *
 * @author lz
 * @since 2024-04-04
 */
@Service
@Slf4j
public class TaskUpdatesServiceImpl extends ServiceImpl<TaskUpdatesMapper, TaskUpdates> implements ITaskUpdatesService {
    

    @Autowired
    private UsersMapper usersMapper;
    
    @Autowired
    private TaskMapper taskMapper;
    
    @Autowired
    private TaskUpdatesMapper taskUpdatesMapper;

    /**
     * 获取当前登录用户信息
     *
     * @return {@code Users}
     */
    public Users getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminName = authentication.getName();
        log.info("管理员: {}", adminName);

        return usersMapper.getByUsername(adminName);
    }

    /**
     * 完成审核操作，并返回对应的状态结果。
     *
     * @return 审核完成状态（YourEnumClass.AUDITING）
     */
    @Override
    public void completeAudit() {
        
    }

    /**
     * 发布委托，并返回对应的状态结果。
     *
     * @return 发布委托状态（YourEnumClass.PUBLISHED）
     */
    @Override
    public void publishAssignment() {

    }

    /**
     * 创建新任务，并返回对应的状态结果。
     *
     * @return 新任务创建状态（YourEnumClass.CREATED）
     */
    @Override
    public void createNewTask() {

    }

    /**
     * 获取委托结果，并返回对应的状态结果。
     *
     * @return 委托结果状态（YourEnumClass.RESULT）
     */
    @Override
    public void getAssignmentResult() {
        

    }

    /**
     * 获取审核中状态，并返回对应的状态结果。
     */
    @Override
    public void getAuditingStatus() {

    }

    /**
     * 将委托回退为草稿
     *
     * @param taskId
     *
     * @return {@code Boolean}
     */
    @Override
    public Boolean fallbackDraft(Long taskId) throws MyException {
        Users currentAdmin = getCurrentAdmin();
        if (currentAdmin != null) {
            TaskUpdates taskupdates = new TaskUpdates();
            taskupdates.setTaskId(taskId);
            taskupdates.setUserId(currentAdmin.getUserId());
            taskupdates.setUpdateType(TaskUpdateType.FALLBACK_DRAFT);
            taskupdates.setUpdateDescription("回退为草稿");
            taskupdates.setUpdateTime(new java.util.Date());
            save(taskupdates);
            return true;
        }
        log.error("权限异常，信息不存在");
        return false;
    }

    @Override
    public void allowPublish(Long taskId) {
        
    }

    @Override
    public void notAllowed(Long taskId) {
        

    }

    @Override
    public Boolean createNewRecord(Long taskId, TaskUpdateType auditing,
                             String dataAuditFail) {
        Users currentAdmin = getCurrentAdmin();
        if (currentAdmin != null) {
            TaskUpdates taskupdates = new TaskUpdates();
            taskupdates.setTaskId(taskId);
            taskupdates.setUserId(currentAdmin.getUserId());
            taskupdates.setUpdateType(auditing);
            taskupdates.setUpdateDescription("回退为草稿");
            taskupdates.setUpdateTime(new java.util.Date());
            save(taskupdates);
            return true;
        }
        log.error("权限异常，信息不存在");
        return false;
        
    }

    @Override
    public IPage<TaskUpdates> page(Page<TaskUpdates> page, String delegateComment, String reviewStatus, Date reviewTime, Long taskId) {
        log.info("delegateComment: {}, reviewStatus: {}, reviewTime: {}, taskId: {}", delegateComment, reviewStatus, reviewTime, taskId);
        IPage<TaskUpdates> list = taskUpdatesMapper.page(page, delegateComment,
                                                     reviewStatus, reviewTime, taskId);
        return list;
    }

    @Override
    public TaskUpdates cancelPublish(Long id) {
        Users users = getCurrentAdmin();
        TaskUpdates taskUpdates = TaskUpdates.builder().taskId(id)
                .userId(users.getUserId())
                .updateType(TaskUpdateType.RESULT)
                .updateDescription("管理员取消发布委托编号为"+id+"的任务")
                .updateTime(new Date(System.currentTimeMillis()))
                .build();

        save(taskUpdates);
        return taskUpdates;
    }

    @Override
    public TaskUpdates addUpdate(TaskUpdateDTO taskUpdateDTO) throws MyException {
        Users user = getCurrentAdmin();
        Task task = taskMapper.selectById(taskUpdateDTO.getTaskId());
        if (task == null) {
            throw new MyException(MessageConstants.TASK_NOT_EXIST);
        }

        if (task.getReceiverId() == null || !task.getReceiverId().equals(user.getUserId())) {
            throw new MyException(MessageConstants.PERMISSION_DENIED);
        }

        if (task.getStatus() != TaskStatus.ACCEPTED) {
            throw new MyException("任务未在执行中");
        }

        TaskUpdates updates = TaskUpdates.builder()
                .taskId(task.getTaskId())
                .userId(user.getUserId())
                .updateType(TaskUpdateType.PROGRESS_UPDATE)
                .updateDescription(taskUpdateDTO.getDescription())
                .updateTime(new Date())
                .build();
        save(updates);
        return updates;
    }

    @Override
    public TaskUpdates addNodeUpdate(TaskNodeDTO taskNodeDTO) throws MyException {
        Users user = getCurrentAdmin();
        Task task = taskMapper.selectById(taskNodeDTO.getTaskId());
        if (task == null) {
            throw new MyException(MessageConstants.TASK_NOT_EXIST);
        }

        if (task.getReceiverId() == null || !task.getReceiverId().equals(user.getUserId())) {
            throw new MyException(MessageConstants.PERMISSION_DENIED);
        }

        if (task.getStatus() != TaskStatus.ACCEPTED) {
            throw new MyException("任务未在执行中");
        }

        TaskUpdateType nodeType = TaskUpdateType.fromDbValue(taskNodeDTO.getNodeType());
        if (nodeType == null) {
            nodeType = TaskUpdateType.fromWebValue(taskNodeDTO.getNodeType());
        }
        if (nodeType != TaskUpdateType.CONTACTED && nodeType != TaskUpdateType.PICKED_UP
                && nodeType != TaskUpdateType.DELIVERED) {
            throw new MyException("无效的打卡节点类型");
        }

        String description = taskNodeDTO.getRemark() != null && !taskNodeDTO.getRemark().trim().isEmpty()
                ? taskNodeDTO.getRemark().trim() : nodeType.getWebValue();

        int level = levelOf(nodeType);
        Integer currentMax = list(new QueryWrapper<TaskUpdates>()
                .eq("TaskID", task.getTaskId())
                .in("UpdateType", TaskUpdateType.CONTACTED.getDbValue(),
                        TaskUpdateType.PICKED_UP.getDbValue(),
                        TaskUpdateType.DELIVERED.getDbValue(),
                        TaskUpdateType.AUTO_ADVANCE.getDbValue()))
                .stream()
                .map(TaskUpdates::getNodeIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        int nodeIndex = Math.max(currentMax, level);

        TaskUpdates updates = TaskUpdates.builder()
                .taskId(task.getTaskId())
                .userId(user.getUserId())
                .updateType(nodeType)
                .updateDescription(description)
                .imgUrl(taskNodeDTO.getImgUrl())
                .location(taskNodeDTO.getLocation())
                .nodeIndex(nodeIndex)
                .updateTime(new Date())
                .build();
        save(updates);
        return updates;
    }

    private int levelOf(TaskUpdateType nodeType) {
        if (nodeType == TaskUpdateType.CONTACTED) {
            return 1;
        }
        if (nodeType == TaskUpdateType.PICKED_UP) {
            return 2;
        }
        if (nodeType == TaskUpdateType.DELIVERED) {
            return 3;
        }
        return 0;
    }
}