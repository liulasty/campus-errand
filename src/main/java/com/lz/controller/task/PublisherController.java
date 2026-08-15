package com.lz.controller.task;

import com.lz.Exception.MyException;
import com.lz.pojo.Enum.AcceptStatus;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.Enum.TaskUpdateType;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.PublishDTO;
import com.lz.pojo.dto.UpdateTaskToCompletedDTO;
import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.TaskAcceptRecords;
import com.lz.pojo.entity.TaskUpdates;
import com.lz.pojo.entity.Users;
import com.lz.pojo.entity.UsersInfo;
import com.lz.pojo.result.PageResult;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.TaskAndUserInfoVO;
import com.lz.service.ITaskAcceptRecordsService;
import com.lz.service.ITaskService;
import com.lz.service.ITaskUpdatesService;
import com.lz.service.IUsersInfoService;
import com.lz.service.IUsersService;
import com.lz.service.impl.RealNameAuthenticationService;
import com.lz.utils.IdentityMaskUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 发布者接口（发布委托/确认接单/取消/完成/删除）
 */
@RestController
@RequestMapping("/tasks/publisher")
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
@Api(tags = "发布者接口", value = "发布者接口")
public class PublisherController {

    @Autowired
    private IUsersInfoService usersInfoService;

    @Autowired
    private IUsersService usersService;

    @Autowired
    private ITaskService taskService;

    @Autowired
    private ITaskUpdatesService taskUpdatesService;

    @Autowired
    private ITaskAcceptRecordsService taskAcceptRecordsService;

    @Autowired
    private RealNameAuthenticationService realNameAuthenticationService;

    /**
     * 获取当前登录用户
     */
    private Users getCurrentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersService.getByUsername(name);
    }

    @GetMapping("/{id}")
    public Result<?> getPublisher(@PathVariable("id") Long id) throws MyException {
        UsersInfo usersInfo = usersInfoService.getById(id);
        if (usersInfo == null) {
            throw new MyException(MessageConstants.USER_AUTHENTICATION_INFO_NOT_EXIST);
        }
        realNameAuthenticationService.ensureL1(id);
        // D-16 同源泄漏：发布者公开资料仅保留非敏感字段，identityNo 掩码，其余个人字段剔除
        usersInfo.setIdentityNo(IdentityMaskUtils.mask(usersInfo.getIdentityNo()));
        usersInfo.setPhoneNumber(null);
        usersInfo.setQqNumber(null);
        usersInfo.setRoleImgSrc("");
        usersInfo.setCertifieTime(null);
        usersInfo.setCertifiedTime(null);
        usersInfo.setRejectReason(null);

        return Result.success(usersInfo);
    }

    /**
     * 发布委托
     */
    @PutMapping("/tasks/{id}/publish")
    @ApiOperation("发布委托")
    public Result<String> confirmTask(@PathVariable("id") Long id,
            @RequestBody PublishDTO data) throws MyException {
        realNameAuthenticationService.ensureCurrentUserL1();
        // D-18 契约修复：截止时间必须为未来，拒绝过去 end（防发布即过期/当天不可接单）
        if (data == null || data.getEnd() == null) {
            throw new MyException("截止时间不能为空");
        }
        if (!data.getEnd().after(new Date())) {
            throw new MyException("截止时间必须晚于当前时间");
        }
        try {
            Task byId = taskService.getById(id);
            if (byId == null) {
                log.error("数据库错误");
                return Result.error(MessageConstants.DATABASE_ERROR);
            }
            if (byId.getStatus() != TaskStatus.PENDING_RELEASE) {
                log.error("任务状态异常");
                return Result.error(MessageConstants.UNEXPECTED_EXCEPTION);
            }
            Users current = getCurrentUser();
            if (current == null || !current.getUserId().equals(byId.getOwnerId())) {
                throw new MyException(MessageConstants.USER_INFO_ERROR);
            }
            taskService.updateById(
                    Task.builder()
                            .taskId(id)
                            .startTime(data.getStart())
                            .endTime(data.getEnd())
                            .status(TaskStatus.ONGOING)
                            .build());
            // 2.2 发布留痕：PUBLISHED（与 CREATED/AUDITING 等状态变更留痕对齐）
            taskUpdatesService.save(TaskUpdates.builder()
                    .taskId(id)
                    .userId(current.getUserId())
                    .updateType(TaskUpdateType.PUBLISHED)
                    .updateDescription(MessageConstants.TASK_PUBLISH_SUCCESS)
                    .updateTime(new Date())
                    .build());
            log.info("发布委托成功");
            return Result.success(MessageConstants.TASK_PUBLISH_SUCCESS);
        } catch (Exception e) {
            log.error("发布委托失败");
            throw new MyException(MessageConstants.TASK_PUBLISH_FAIL);
        }
    }

    /**
     * “获取任务”页（我的发布列表）
     */
    @GetMapping("/tasks")
    public Result<?> getTaskPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long taskType,
            @RequestParam(defaultValue = "0") Integer queryRules,
            @RequestParam(required = false) TaskStatus status) throws MyException {
        PageResult<Task> taskPageResult = taskService.searchPageByPublisher(pageNum,
                pageSize, location, description,
                taskType,
                queryRules, status);

        return Result.success(taskPageResult);
    }

    /**
     * 确认委托接收者
     */
    @PutMapping("/accepts/{id}/confirm")
    public Result<?> confirm(@PathVariable("id") Long id) throws MyException {
        TaskAcceptRecords acceptRecords = taskAcceptRecordsService.getById(id);
        if (acceptRecords == null || acceptRecords.getStatus() != AcceptStatus.PENDING) {
            log.error("数据库错误");
            throw new MyException(MessageConstants.DATABASE_ERROR);
        }
        Task task = taskService.getById(acceptRecords.getTaskId());
        if (task.getStatus() != TaskStatus.ONGOING) {
            log.error("任务状态异常");
            throw new MyException(MessageConstants.UNEXPECTED_EXCEPTION);
        }
        taskService.confirmTheRecipient(task.getTaskId(), acceptRecords);
        return Result.success(MessageConstants.TASK_UPDATE_SUCCESS);
    }

    @PutMapping("/tasks/{id}/cancel-publish")
    public Result<String> cancelPublish(@PathVariable("id") Long id) throws MyException {
        taskService.cancelPublishUser(id);
        return Result.success(MessageConstants.TASK_CANCEL_PUBLISH_SUCCESS);
    }

    /**
     * 获取委托任务详情
     */
    @GetMapping("/tasks/{id}")
    public Result<?> getTask(@PathVariable("id") Long id) throws MyException {
        Task byId = taskService.getById(id);
        if (byId == null) {
            throw new MyException(MessageConstants.TASK_NOT_EXIST);
        }
        // 信息安全：发布者详情含全部申请者，仅发布者本人可查看
        Users current = getCurrentUser();
        if (current == null || !current.getUserId().equals(byId.getOwnerId())) {
            throw new MyException(MessageConstants.USER_INFO_ERROR);
        }
        TaskAndUserInfoVO taskAndUserInfo = taskService.publisherSearchTaskAndPublisherInfo(id);

        return Result.success(taskAndUserInfo);
    }

    /**
     * 发布者查看自己发布的任务摘要
     */
    @GetMapping("/{taskId}/task")
    @ApiOperation("发布者查看自己发布的任务摘要")
    public Result<Task> getPublisherTaskBrief(@PathVariable("taskId") Long taskId) throws MyException {
        Task task = taskService.getById(taskId);
        if (task == null) {
            throw new MyException(MessageConstants.TASK_NOT_EXIST);
        }
        Users current = getCurrentUser();
        if (current == null || !current.getUserId().equals(task.getOwnerId())) {
            throw new MyException(MessageConstants.PERMISSION_DENIED);
        }
        return Result.success(task);
    }

    /**
     * 发布者回退草稿（已过期/已取消委托 → 草稿）
     */
    @PutMapping("/tasks/{id}/fallback-draft")
    @ApiOperation("发布者回退草稿")
    public Result<?> fallbackDraftByPublisher(@PathVariable("id") Long taskId) throws MyException {
        Task task = taskService.getById(taskId);
        if (task == null) {
            throw new MyException(MessageConstants.TASK_NOT_EXIST);
        }
        Users current = getCurrentUser();
        if (current == null || !current.getUserId().equals(task.getOwnerId())) {
            throw new MyException(MessageConstants.PERMISSION_DENIED);
        }
        if (!taskService.publisherFallbackDraft(taskId)) {
            throw new MyException(MessageConstants.DATABASE_ERROR);
        }
        return Result.success(MessageConstants.TASK_UPDATE_SUCCESS);
    }

    @PutMapping("/tasks/{id}/completed")
    public Result<?> completed(@PathVariable Long id,
            @RequestBody UpdateTaskToCompletedDTO DTO) throws MyException {
        taskService.updateToCompleted(DTO);

        return Result.success(MessageConstants.TASK_UPDATE_SUCCESS);
    }

    @DeleteMapping("/tasks/{id}")
    public Result<?> deleteTask(@PathVariable("id") Long id) throws MyException {
        taskService.deleteCancelTask(id);
        return Result.success(MessageConstants.TASK_DELETE_SUCCESS);
    }
}
