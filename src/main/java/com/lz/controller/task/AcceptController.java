package com.lz.controller.task;

import com.lz.Exception.MyException;
import com.lz.pojo.Enum.AcceptStatus;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.AcceptDTO;
import com.lz.pojo.entity.TaskAcceptRecords;
import com.lz.pojo.result.PageResult;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.TaskAcceptRecord;
import com.lz.service.ITaskAcceptRecordsService;
import com.lz.service.ITaskService;
import com.lz.service.RealNameAuthenticationService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 接单接口（接单/我的接单/取消）
 */
@RestController
@RequestMapping("/tasks/accepts")
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
@Api(tags = "接单接口")
public class AcceptController {
    @Autowired
    private ITaskAcceptRecordsService taskAcceptRecordsService;

    @Autowired
    private ITaskService taskService;

    @Autowired
    private RealNameAuthenticationService realNameAuthenticationService;

    /**
     * 获取委托任务接收信息
     */
    @GetMapping("/{id}")
    public Result<TaskAcceptRecords> getTaskAcceptRecordByTaskId(@PathVariable Long id) throws MyException {
        log.info("查询接收委托信息 {}", id);
        TaskAcceptRecords taskAcceptRecord = taskAcceptRecordsService.getTaskAcceptRecordByTaskId(id);
        return Result.success(taskAcceptRecord);
    }

    /**
     * 添加接收委托留言
     */
    @PostMapping
    public Result<?> accept(@Validated @RequestBody AcceptDTO acceptDTO) throws MyException {
        log.info("接收委托留言 {}", acceptDTO);
        realNameAuthenticationService.ensureCurrentUserL1();
        taskAcceptRecordsService.create(acceptDTO);
        return Result.success(MessageConstants.DATA_ACCEPT_SUCCESS);
    }

    @GetMapping
    public Result<PageResult> getTaskPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long taskType,
            @RequestParam(defaultValue = "0") Integer queryRules,
            @RequestParam(required = false) TaskStatus status) throws MyException {
        PageResult<TaskAcceptRecord> taskPageResult = taskService.searchPageByAcceptor(pageNum,
                pageSize, location, description,
                taskType,
                queryRules, status);

        return Result.success(taskPageResult);
    }

    /**
     * 取消接受记录
     */
    @PutMapping("/{id}/cancel")
    public Result<?> cancelAcceptRecords(@PathVariable("id") Long id) throws MyException {
        TaskAcceptRecords taskAcceptRecord = taskAcceptRecordsService.getById(id);
        if (taskAcceptRecord == null) {
            throw new MyException(MessageConstants.TASK_NOT_EXIST);
        }
        if (!taskAcceptRecord.getStatus().equals(AcceptStatus.PENDING)) {
            throw new MyException(MessageConstants.DATABASE_ERROR);
        }
        taskAcceptRecord.setStatus(AcceptStatus.CANCEL);

        taskAcceptRecordsService.updateById(taskAcceptRecord);
        return Result.success(MessageConstants.ACCEPT_CANCEL_SUCCESS);
    }
}
