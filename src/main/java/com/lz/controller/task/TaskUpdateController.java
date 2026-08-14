package com.lz.controller.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lz.Exception.MyException;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.TaskNodeDTO;
import com.lz.pojo.dto.TaskUpdateDTO;
import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.TaskUpdates;
import com.lz.pojo.entity.Users;
import com.lz.pojo.result.PageResult;
import com.lz.pojo.result.Result;
import com.lz.service.ITaskService;
import com.lz.service.ITaskUpdatesService;
import com.lz.service.IUsersService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 履约动态接口（用户侧：添加/节点打卡/单条查询）
 */
@RestController
@RequestMapping("/tasks/updates")
@Slf4j
@Api(tags = "履约动态接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class TaskUpdateController {

    @Autowired
    private ITaskUpdatesService taskUpdateService;

    @Autowired
    private ITaskService taskService;

    @Autowired
    private IUsersService usersService;

    @GetMapping
    @ApiOperation("查询任务履约动态列表（用户侧共享读，参与人可见）")
    public Result<?> list(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "taskId", required = false) Long taskId) throws MyException {
        if (taskId == null) {
            throw new MyException("taskId 不能为空");
        }
        Task task = taskService.getById(taskId);
        if (task == null) {
            throw new MyException(MessageConstants.TASK_NOT_EXIST);
        }
        Users current = getCurrentUser();
        boolean participant = current != null
                && (current.getUserId().equals(task.getOwnerId())
                        || (task.getReceiverId() != null && current.getUserId().equals(task.getReceiverId())));
        if (!participant) {
            throw new MyException(MessageConstants.PERMISSION_DENIED);
        }
        Page<TaskUpdates> page = new Page<>(pageNum, pageSize);
        IPage<TaskUpdates> taskUpdatesPage = taskUpdateService.page(page, null, null, null, taskId);
        return Result.success(new PageResult<>(taskUpdatesPage.getTotal(), taskUpdatesPage.getRecords()));
    }

    private Users getCurrentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersService.getByUsername(name);
    }

    @GetMapping("/{id}")
    public Result<?> getTask(@PathVariable("id") Long id) {
        TaskUpdates taskUpdates = taskUpdateService.getById(id);
        return Result.success(taskUpdates);
    }

    @PostMapping
    @ApiOperation("添加任务进度更新")
    public Result<TaskUpdates> addUpdate(@RequestBody TaskUpdateDTO taskUpdateDTO)
            throws MyException {
        TaskUpdates taskUpdates = taskUpdateService.addUpdate(taskUpdateDTO);
        return Result.success(taskUpdates, MessageConstants.TASK_UPDATE_SUCCESS);
    }

    @PostMapping("/node")
    @ApiOperation("添加任务履约节点打卡")
    public Result<TaskUpdates> addNodeUpdate(@RequestBody TaskNodeDTO taskNodeDTO)
            throws MyException {
        TaskUpdates taskUpdates = taskUpdateService.addNodeUpdate(taskNodeDTO);
        return Result.success(taskUpdates, MessageConstants.TASK_UPDATE_SUCCESS);
    }
}
