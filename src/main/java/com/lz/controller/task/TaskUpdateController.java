package com.lz.controller.task;

import com.lz.Exception.MyException;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.TaskNodeDTO;
import com.lz.pojo.dto.TaskUpdateDTO;
import com.lz.pojo.entity.TaskUpdates;
import com.lz.pojo.result.Result;
import com.lz.service.ITaskUpdatesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
