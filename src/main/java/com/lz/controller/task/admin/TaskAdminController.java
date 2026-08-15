package com.lz.controller.task.admin;

import com.lz.Exception.MyException;
import com.lz.pojo.Enum.TaskPhase;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.Enum.TaskUpdateType;
import com.lz.pojo.Page.DraftConfig;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.TaskPageDTO;
import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.TaskUpdates;
import com.lz.pojo.entity.Users;
import com.lz.pojo.entity.UsersInfo;
import com.lz.pojo.result.PageResult;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.TaskExportVO;
import com.lz.service.IDelegateAuditRecordsService;
import com.lz.service.INotificationReadStatusService;
import com.lz.service.INotificationsService;
import com.lz.service.ITaskService;
import com.lz.service.ITaskUpdatesService;
import com.lz.service.IUsersInfoService;
import com.lz.service.IUsersService;
import com.lz.utils.excelutil.EasyExcelUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 委托管理接口（管理员）
 */
@RestController
@RequestMapping("/admin/tasks")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
@Api(tags = "管理员管理委托相关接口")
@Slf4j
public class TaskAdminController {

    private final ITaskService taskService;
    private final IDelegateAuditRecordsService delegateAuditRecordsService;
    private final IUsersService usersService;
    private final IUsersInfoService usersInfoService;
    private final ITaskUpdatesService taskUpdatesService;
    private final INotificationReadStatusService notificationReadStatusService;
    private final INotificationsService notificationsService;

    /**
     * 获取当前登录管理员的信息。
     */
    @Autowired
    public TaskAdminController(ITaskService taskService,
            IDelegateAuditRecordsService delegateAuditRecordsService,
            IUsersService usersService,
            IUsersInfoService usersInfoService,
            ITaskUpdatesService taskUpdatesService,
            INotificationReadStatusService notificationReadStatusService,
            INotificationsService notificationsService) {
        this.taskService = taskService;
        this.delegateAuditRecordsService = delegateAuditRecordsService;
        this.usersService = usersService;
        this.usersInfoService = usersInfoService;
        this.taskUpdatesService = taskUpdatesService;
        this.notificationReadStatusService = notificationReadStatusService;
        this.notificationsService = notificationsService;
    }

    /**
     * 获取当前用户信息
     */
    public Users getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminName = authentication.getName();
        log.info("管理员: {}", adminName);

        return usersService.getByUsername(adminName);
    }

    /**
     * 管理端委托列表（分页）
     */
    @GetMapping
    @ApiOperation("分页查询")
    public Result<?> searchPage(@RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "5") int pageSize,
            @RequestParam(value = "Description", required = false) String description,
            @RequestParam(value = "taskType", required = false) Integer taskType,
            @RequestParam(value = "Location", required = false) String location,
            @RequestParam(value = "CreatedAt", required = false) LocalDate createdAt,
            @RequestParam(value = "TypePhase", required = false) String typePhase,
            @RequestParam(value = "Status", required = false) String status) throws MyException {
        log.info("搜索页面{}，{}，{},{},{}", taskType, createdAt, location, typePhase, description);
        // 非法 TypePhase 返回明确业务错误，而非 500（D-13；fromValue 对非法值抛 IllegalArgumentException）
        TaskPhase taskPhase = null;
        if (typePhase != null) {
            try {
                taskPhase = TaskPhase.fromValue(typePhase);
            } catch (IllegalArgumentException e) {
                throw new MyException("非法的委托阶段参数: " + typePhase);
            }
        }
        // Status 支持逗号分隔多值（如 DRAFT,AUDIT_FAILED），按 dbValue 或枚举名解析
        List<TaskStatus> statusList = parseStatusParam(status);
        DraftConfig draftConfig = new DraftConfig(createdAt, description, location, pageNum, pageSize,
                taskType,
                taskPhase, statusList);
        PageResult<Task> taskPageResult = taskService.searchPageByAdmin(draftConfig);
        return Result.success(taskPageResult);
    }

    private List<TaskStatus> parseStatusParam(String status) throws MyException {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        List<TaskStatus> result = new java.util.ArrayList<>();
        for (String item : status.split(",")) {
            String value = item.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                result.add(TaskStatus.fromDbValue(value));
            } catch (IllegalArgumentException e) {
                try {
                    result.add(TaskStatus.valueOf(value));
                } catch (IllegalArgumentException e2) {
                    throw new MyException("非法的任务状态参数: " + value);
                }
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 审核/通用分页（原 /task/searchPage）
     */
    @PostMapping("/search")
    @ApiOperation("审核/通用分页查询")
    public Result<PageResult<Task>> auditSearchPage(TaskPageDTO taskPageDTO) {
        log.info("委托信息查询taskPageDTO:{}", taskPageDTO);
        PageResult<Task> taskPageResult = taskService.searchPage(taskPageDTO);
        return Result.success(taskPageResult);
    }

    @GetMapping("/export")
    @ApiOperation("导出委托列表 Excel")
    @com.lz.Annotation.NoReturnHandle
    public void exportExcel(HttpServletResponse response) throws MyException {
        List<Task> tasks = taskService.list();
        Set<Long> ownerIds = tasks.stream().map(Task::getOwnerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Users> userMap = ownerIds.isEmpty() ? java.util.Collections.emptyMap()
                : usersService.listByIds(ownerIds).stream()
                        .collect(Collectors.toMap(Users::getUserId, u -> u, (a, b) -> a));
        Map<Long, Integer> authMap = ownerIds.isEmpty() ? java.util.Collections.emptyMap()
                : usersInfoService.listByIds(ownerIds).stream()
                        .collect(Collectors.toMap(UsersInfo::getUserId,
                                ui -> ui.getAuthLevel() == null ? 0 : ui.getAuthLevel(),
                                (a, b) -> Math.max(a, b)));

        List<TaskExportVO> rows = tasks.stream().map(t -> {
            Users owner = userMap.get(t.getOwnerId());
            return TaskExportVO.builder()
                    .taskId(t.getTaskId())
                    .ownerId(t.getOwnerId())
                    .ownerName(owner != null ? owner.getUsername() : null)
                    .ownerCredit(owner != null ? owner.getCreditScore() : null)
                    .authLevel(authMap.getOrDefault(t.getOwnerId(), 0))
                    .description(t.getDescription())
                    .location(t.getLocation())
                    .money(t.getMoney())
                    .status(t.getStatus() != null ? t.getStatus().getWebValue() : null)
                    .startTime(t.getStartTime())
                    .endTime(t.getEndTime())
                    .createdAt(t.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
        EasyExcelUtil.exportExcel(response, "委托列表", "委托列表", rows, TaskExportVO.class);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询")
    public Result<?> searchTask(@PathVariable("id") Long taskID) throws MyException {
        log.info("根据id查询{}", taskID);

        return Result.success(taskService.searchTask(taskID));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("管理员删除委托")
    public Result<?> deleteTask(@PathVariable("id") Long taskID) {
        Task task = taskService.getById(taskID);
        if (task == null || task.getStatus() == TaskStatus.ONGOING) {
            return Result.error(MessageConstants.TASK_NOT_EXIST);
        }

        log.info("管理员删除委托{}", taskID);
        taskService.removeById(taskID);
        Users users = getCurrentAdmin();

        TaskUpdates taskupdates = TaskUpdates.builder().taskId(taskID)
                .userId(users.getUserId())
                .updateType(TaskUpdateType.RESULT)
                .updateDescription(MessageConstants.TASK_DRAFT_DELETE_SUCCESS).build();
        taskUpdatesService.save(taskupdates);
        // D-19：删除通知归属发布者 owner，read_status 投递给 owner 并正确关联 taskId
        // 无发布者的任务（task.OwnerID 可空，如种子/测试数据）无通知对象，跳过投递，避免 UserID NOT NULL 插入失败
        if (task.getOwnerId() != null) {
            Long id = notificationsService.addTaskDeleteNotification(task.getOwnerId(),
                    "您的委托已被删除");
            notificationReadStatusService.addTaskNotification(id,
                    task.getTaskId(),
                    task.getOwnerId());
        }
        log.info("管理员删除委托成功{}", taskID);
        return Result.success(MessageConstants.TASK_DELETE_SUCCESS);
    }

    /**
     * 回退草稿
     */
    @PutMapping("/{id}/fallback-draft")
    @ApiOperation("管理员回退草稿")
    public Result<?> fallbackDraft(@PathVariable("id") Long taskId) throws MyException {
        log.info("管理员获取回退草稿{}", taskId);
        Task task = taskService.getById(taskId);
        if (task == null || task.getStatus() == TaskStatus.ONGOING) {
            log.error("回退草稿失败{}", taskId);
            return Result.error(MessageConstants.TASK_NOT_EXIST);
        }

        if (!taskService.fallbackDraft(taskId)) {
            log.error("回退草稿失败{}", taskId);
            return Result.error(MessageConstants.DATABASE_ERROR);
        }

        return Result.success(MessageConstants.TASK_UPDATE_SUCCESS);
    }

    /**
     * 允许发布
     */
    @PutMapping("/{id}/allow-publish")
    @ApiOperation("管理员允许发布")
    public Result<?> allowPublish(@PathVariable("id") Long taskId) throws MyException {
        log.info("管理员允许发布{}", taskId);
        Task task = taskService.getById(taskId);
        if (task == null || task.getStatus() == TaskStatus.ONGOING) {
            log.error("委托信息异常{}", taskId);
            return Result.error(MessageConstants.TASK_NOT_EXIST);
        }

        if (!taskService.allowPublish(taskId)) {
            log.error("委托审核失败{}", taskId);
            return Result.error(MessageConstants.DATABASE_ERROR);
        }

        return Result.success(MessageConstants.TASK_UPDATE_SUCCESS);
    }

    /**
     * 拒绝发布
     */
    @PutMapping("/{id}/reject-publish")
    @ApiOperation("管理员不允许发布")
    public Result<?> notAllowed(@PathVariable("id") Long taskId) throws MyException {
        log.info("管理员不允许发布{}", taskId);
        Task task = taskService.getById(taskId);
        if (task == null || task.getStatus() == TaskStatus.ONGOING) {
            log.error("委托信息异常{}", taskId);
            return Result.error(MessageConstants.TASK_NOT_EXIST);
        }

        if (!taskService.notAllowed(taskId)) {
            log.error("委托审核失败{}", taskId);
            return Result.error(MessageConstants.DATABASE_ERROR);
        }

        return Result.success(MessageConstants.TASK_UPDATE_SUCCESS);
    }

    @PutMapping("/{id}/enable")
    @ApiOperation("管理员启用")
    public Result<?> handleEnableAdmin(@PathVariable("id") Long id) throws MyException {
        log.info("管理员启用{}", id);
        usersService.cancelDisableUser(id);
        return Result.success(MessageConstants.USER_ABLE_SUCCESS);
    }

    @PutMapping("/{id}/disable")
    @ApiOperation("管理员禁用")
    public Result<?> handleDisableAdmin(@PathVariable("id") Long id) throws MyException {
        log.info("管理员禁用{}", id);
        usersService.disableUser(id);
        return Result.success(MessageConstants.USER_DISABLE_SUCCESS);
    }

    @PutMapping("/{id}/withdraw-release")
    @ApiOperation("管理员撤回发布")
    public Result<?> withdrawReleaseByTaskID(@PathVariable("id") Long id) throws MyException {
        log.info("管理员撤回发布{}", id);
        taskService.withdrawReleaseByTaskID(id);
        return Result.success(MessageConstants.TASK_WITHDRAW_SUCCESS);
    }
}
