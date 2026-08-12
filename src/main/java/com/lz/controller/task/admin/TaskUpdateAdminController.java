package com.lz.controller.task.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lz.Exception.MyException;
import com.lz.pojo.Enum.TaskUpdateType;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.entity.TaskUpdates;
import com.lz.pojo.result.PageResult;
import com.lz.pojo.result.Result;
import com.lz.service.ITaskUpdatesService;
import com.lz.utils.EnumUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * 履约动态管理接口（管理员：分页/类型/删除）
 */
@RestController
@RequestMapping("/admin/tasks/updates")
@Slf4j
@Api(tags = "履约动态管理接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class TaskUpdateAdminController {

    @Autowired
    private ITaskUpdatesService taskUpdateService;

    @GetMapping
    public Result<?> list(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "DelegateComment", required = false) String DelegateComment,
            @RequestParam(value = "ReviewStatus", required = false) String ReviewStatus,
            @RequestParam(value = "ReviewTime", required = false) @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd") Date ReviewTime,
            @RequestParam(value = "taskId", required = false) Long taskId

    ) {
        Page<TaskUpdates> page = new Page<>(pageNum, pageSize);
        log.info("delegateComment: {}, reviewStatus: {}, reviewTime: {}, taskId: {}", DelegateComment,
                ReviewStatus, ReviewTime, taskId);

        IPage<TaskUpdates> taskUpdatesPage = taskUpdateService.page(page, DelegateComment, ReviewStatus, ReviewTime, taskId);

        return Result.success(
                new PageResult<>(taskUpdatesPage.getTotal(), taskUpdatesPage.getRecords()));
    }

    @GetMapping("/types")
    public Result<?> getType() {
        Map<String, String> map = EnumUtils.generateKeyValues(TaskUpdateType.values());
        return Result.success(map);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteRecords(@PathVariable("id") Long id) {
        log.info("删除更新记录 {}", id);
        taskUpdateService.removeById(id);

        return Result.success(MessageConstants.TASK_RECORDS_DELETE_SUCCESS);
    }
}
