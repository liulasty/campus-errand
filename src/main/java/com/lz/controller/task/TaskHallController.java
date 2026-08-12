package com.lz.controller.task;

import com.lz.Exception.MyException;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.entity.Task;
import com.lz.pojo.result.NameAndDescription;
import com.lz.pojo.result.PageResult;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.TaskDetails;
import com.lz.service.IDelegationCategoriesService;
import com.lz.service.ITaskService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 委托大厅接口（分页/详情/分类）
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
@RequestMapping("/tasks/hall")
@Api(tags = "委托大厅接口", value = "委托大厅接口")
public class TaskHallController {
    @Autowired
    private ITaskService taskService;

    @Autowired
    private IDelegationCategoriesService delegationCategoriesService;

    @GetMapping
    public Result<PageResult<Task>> getTaskPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long taskType,
            @RequestParam(defaultValue = "0") Integer queryRules,
            @RequestParam(required = false) TaskStatus status) {
        PageResult<Task> taskPageResult = taskService.searchPage(pageNum, pageSize, location, description,
                taskType,
                queryRules, status);

        return Result.success(taskPageResult);
    }

    /**
     * 获取委托任务详情
     */
    @GetMapping("/{id}")
    public Result<TaskDetails> getTask(@PathVariable("id") Long id) throws MyException {
        TaskDetails taskAndUserInfo = taskService.getTaskAndPublisherInfo(id);

        return Result.success(taskAndUserInfo);
    }

    @GetMapping("/categories")
    public Result<List<NameAndDescription>> getTaskCategory() throws MyException {
        List<NameAndDescription> taskCategory = delegationCategoriesService.getTaskCategoryUser();
        return Result.success(taskCategory);
    }
}
