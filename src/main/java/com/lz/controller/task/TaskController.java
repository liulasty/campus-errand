package com.lz.controller.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lz.Exception.MyException;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.constants.AuditResult;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.AuditResultDTO;
import com.lz.pojo.dto.TaskDTO;
import com.lz.pojo.dto.TaskDraftDTO;
import com.lz.pojo.entity.DelegateAuditRecords;
import com.lz.pojo.entity.DelegationCategories;
import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.Users;
import com.lz.pojo.entity.UsersInfo;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.AuditResultVO;
import com.lz.pojo.vo.NewestInfoVO;
import com.lz.pojo.vo.TaskDraftVO;
import com.lz.pojo.vo.UserDelegateDraft;
import com.lz.service.IDelegateAuditRecordsService;
import com.lz.service.IDelegationCategoriesService;
import com.lz.service.ITaskService;
import com.lz.service.IUsersInfoService;
import com.lz.service.IUsersService;
import com.lz.service.RealNameAuthenticationService;
import com.lz.utils.ValidateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 委托主流程接口（草稿/审核/发布/搜索/状态）
 */
@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Api(tags = "委托相关接口")
@Slf4j
public class TaskController {

    @Autowired
    private ITaskService taskService;

    @Autowired
    private IUsersService usersService;

    @Autowired
    private IUsersInfoService usersInfoService;

    @Autowired
    private IDelegationCategoriesService delegationCategoriesService;

    @Autowired
    private IDelegateAuditRecordsService delegateAuditRecordsService;

    @Autowired
    private RealNameAuthenticationService realNameAuthenticationService;

    //创建单个委托
    @PostMapping("/drafts")
    @ApiOperation("创建单个委托草稿")
    public Result<Long> createTask(@RequestBody @Validated TaskDTO taskDTO,
                                   BindingResult result) throws MyException {
        if (ValidateUtil.validate(result) != null) {
            return Result.error(ValidateUtil.validate(result));
        }

        Long taskId = taskService.createTask(taskDTO);

        return Result.success(taskId, MessageConstants.TASK_CREATE_SUCCESS);
    }

    @GetMapping
    @ApiOperation("按状态获取任务列表")
    public Result<List<Task>> getPendingAuditList(@RequestParam(required = false) String status)
            throws MyException {
        TaskStatus taskStatus = TaskStatus.AUDITING;
        if (status != null && !status.trim().isEmpty()) {
            taskStatus = TaskStatus.fromDbValue(status);
        }
        List<Task> taskList = taskService.list(new QueryWrapper<Task>()
                .eq("status", taskStatus)
                .orderByDesc("CreatedAt"));
        return Result.success(taskList);
    }

    //获取单个委托详细信息
    @GetMapping("/{id}")
    @ApiOperation("获取单个委托详细信息")
    public Result<TaskDraftVO> getTaskDetail(@PathVariable("id") Long id) throws MyException {
        TaskDraftVO taskDraftVO = taskService.searchTask(id);
        return Result.success(taskDraftVO);
    }

    @PutMapping("/drafts")
    @ApiOperation("更新委托草稿信息")
    public Result<String> updateTask(@RequestBody TaskDraftDTO taskDTO) throws MyException {
        try {
            Task byId = taskService.getById(taskDTO.getTaskId());
            if (byId == null) {
                throw new MyException(MessageConstants.DATA_VALIDATION_ERROR);
            }
            DelegationCategories delegationCategories = delegationCategoriesService.getTaskCategoryByCategoryName(taskDTO.getType());
            if (delegationCategories == null) {
                throw new MyException("类别不存在");
            }
            Task task = Task.builder().taskId(taskDTO.getTaskId())
                    .taskType(delegationCategories.getCategoryId())
                    .description(taskDTO.getDescription())
                    .location(taskDTO.getLocation())
                    .status(TaskStatus.DRAFT)
                    .createdAt(taskDTO.getCreatedAt()).build();

            taskService.updateById(task);
            return Result.success(MessageConstants.TASK_UPDATE_SUCCESS);
        } catch (Exception e) {
            throw new MyException(e.getMessage());
        }
    }

    @DeleteMapping("/drafts/{id}")
    @ApiOperation("删除委托草稿")
    public Result<String> deleteTaskDraft(@PathVariable("id") Long id) throws MyException {
        Task byId = taskService.getById(id);
        if (byId == null) {
            throw new MyException(MessageConstants.DATA_VALIDATION_ERROR);
        }
        Users current = getCurrentUser();
        if (current == null || !current.getUserId().equals(byId.getOwnerId())) {
            throw new MyException("无权删除该委托草稿");
        }
        taskService.removeById(id);
        return Result.success(MessageConstants.TASK_UPDATE_SUCCESS);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除委托")
    public Result<String> deleteTask(@PathVariable("id") Long taskId) throws MyException {
        Task byId = taskService.getById(taskId);
        if (byId == null) {
            throw new MyException(MessageConstants.DATA_VALIDATION_ERROR);
        }
        Users current = getCurrentUser();
        if (current == null || !current.getUserId().equals(byId.getOwnerId())) {
            throw new MyException("无权删除该委托");
        }
        taskService.removeById(taskId);
        return Result.success(MessageConstants.TASK_DELETE_SUCCESS);
    }

    /**
     * 获取当前登录用户（D-12 越权删除校验）
     */
    private Users getCurrentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersService.getByUsername(name);
    }

    /**
     * 提交审核任务
     */
    @PutMapping("/drafts/{id}/submit")
    @ApiOperation("提交审核")
    public Result<String> auditTask(@PathVariable("id") Long id) throws MyException {
        realNameAuthenticationService.ensureCurrentUserL1();
        taskService.auditTask(id);
        return Result.success(MessageConstants.TASK_UPDATE_SUCCESS);
    }

    /**
     * 获取需要发布的委托
     */
    @GetMapping("/{id}/confirm")
    @ApiOperation("获取需要发布的委托")
    public Result<Task> confirmTask(@PathVariable("id") Long id) throws MyException {
        try {
            Task byId = taskService.getById(id);
            if (byId == null) {
                return Result.error(MessageConstants.DATABASE_ERROR);
            }
            if (byId.getStatus() != TaskStatus.PENDING_RELEASE) {
                return Result.error(MessageConstants.UNEXPECTED_EXCEPTION);
            }

            return Result.success(byId, MessageConstants.TASK_INFO_SUCCESS);
        } catch (Exception e) {
            throw new MyException(MessageConstants.TASK_PUBLISH_FAIL);
        }
    }

    /**
     * 用户取消发布
     */
    @PutMapping("/{id}/cancel")
    @ApiOperation("用户取消发布")
    public Result<String> cancelTask(@PathVariable("id") Long id) throws MyException {
        try {
            Task byId = taskService.getById(id);
            if (byId == null) {
                return Result.error(MessageConstants.DATABASE_ERROR);
            }
            if (byId.getStatus() != TaskStatus.ONGOING) {
                return Result.error(MessageConstants.UNEXPECTED_EXCEPTION);
            }
            Users current = getCurrentUser();
            if (current == null || !current.getUserId().equals(byId.getOwnerId())) {
                throw new MyException(MessageConstants.USER_INFO_ERROR);
            }
            taskService.updateById(Task.builder().taskId(id).status(TaskStatus.DRAFT).build());
            return Result.success(MessageConstants.TASK_CANCEL_SUCCESS);
        } catch (Exception e) {
            throw new MyException(e.getMessage());
        }
    }

    /**
     * 提交审核结果
     */
    @PostMapping("/audit-result")
    @ApiOperation("提交审核结果")
    public Result<String> auditResult(@RequestBody AuditResultDTO auditResultDTO) throws MyException {
        try {
            Task byId = taskService.getById(auditResultDTO.getDelegateId());
            if (byId == null) {
                return Result.error(MessageConstants.DATABASE_ERROR);
            }
            if (byId.getStatus() != TaskStatus.AUDITING) {
                return Result.error(MessageConstants.UNEXPECTED_EXCEPTION);
            }
            taskService.updateTask(auditResultDTO);

            return Result.success(MessageConstants.DATA_AUDIT_SUCCESS);
        } catch (Exception e) {
            throw new MyException(e.getMessage());
        }
    }

    /**
     * 查询审核未通过原因
     */
    @GetMapping("/{id}/audit-reason")
    @ApiOperation("获取审核未通过原因")
    public Result<AuditResultVO> getReason(@PathVariable("id") Long id) throws MyException {
        log.info("获取审核未通过原因 {}", id);
        Task byId = taskService.getById(id);
        if (byId == null) {
            throw new MyException(MessageConstants.DATABASE_ERROR);
        }
        if (byId.getStatus() != TaskStatus.AUDIT_FAILED) {
            throw new MyException(MessageConstants.UNEXPECTED_EXCEPTION);
        }
        DelegateAuditRecords failReason =
                delegateAuditRecordsService.getFailReasonById(id);
        log.info("failReason {} {}", failReason, byId.getOwnerId());
        UsersInfo usersInfo = usersInfoService.getById(byId.getOwnerId());
        log.info("userInfo {}", usersInfo);
        AuditResultVO auditResultVO = AuditResultVO.builder()
                .reviewStatus(AuditResult.REJECTED)
                .reviewComment(failReason.getReviewComment())
                .reviewTime(failReason.getReviewTime())
                .name(usersInfo.getName())
                .build();

        return Result.success(auditResultVO);
    }

    //最新发布的委托列表（简要信息）
    @GetMapping("/newest/{userId}")
    @ApiOperation("快速信息展示")
    public Result<NewestInfoVO> getNewTask(@PathVariable Long userId) {
        // 系统公告与通知
        NewestInfoVO newestInfo = taskService.getNewestInfo(userId);

        return Result.success(newestInfo);
    }

    //获取用户委托草稿
    @GetMapping("/drafts")
    @ApiOperation("获取用户委托草稿")
    public Result<List<UserDelegateDraft>> getUserDelegateDraft(@RequestParam("userId") Long userId) {
        List<UserDelegateDraft> tasksWithUser = taskService.getUserDelegateDraft(userId);
        return Result.success(tasksWithUser);
    }

    //获取委托状态
    @GetMapping("/{id}/status")
    @ApiOperation("获取委托状态")
    public Result<TaskStatus> getTaskStatus(@PathVariable Long id) {
        TaskStatus status = taskService.getTaskStatus(id);
        return Result.success(status);
    }
}
