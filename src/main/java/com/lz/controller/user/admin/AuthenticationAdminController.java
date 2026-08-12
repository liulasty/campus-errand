package com.lz.controller.user.admin;

import com.lz.Exception.MyException;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.entity.UsersInfo;
import com.lz.pojo.result.Result;
import com.lz.service.ITaskService;
import com.lz.service.IUsersInfoService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 实名审核接口（管理员）
 */
@RestController
@RequestMapping("/admin/authentications")
@Slf4j
@Api(tags = "实名审核接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class AuthenticationAdminController {
    @Autowired
    private IUsersInfoService usersInfoService;

    @Autowired
    private ITaskService taskService;

    /**
     * 确认通过审核
     */
    @PutMapping("/{id}/approve")
    public Result<String> confirmToPassTheReview(@PathVariable Long id) throws MyException {
        log.info("用户认证信息审核通过");
        usersInfoService.confirmToPassTheReview(id);
        log.info("用户认证信息审核通过成功");
        return Result.success(MessageConstants.USER_UPDATE_SUCCESS);
    }

    /**
     * 拒绝通过审核
     */
    @PutMapping("/{id}/reject")
    public Result<String> refuseToPassReview(@PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason) throws MyException {
        log.info("用户认证信息审核不通过");
        usersInfoService.refuseToPassReview(id, reason);
        log.info("用户认证信息审核不通过成功");
        return Result.success(MessageConstants.USER_UPDATE_SUCCESS);
    }

    /**
     * 删除实名记录
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        UsersInfo usersInfo = usersInfoService.getById(id);
        if (usersInfo == null) {
            log.error("用户认证信息不存在");
            return Result.error(MessageConstants.USER_AUTHENTICATION_INFO_NOT_EXIST);
        }
        if (taskService.getTasksWithUser(id).size() > 0) {
            log.error("用户存在委托任务，无法删除认证信息");
            return Result.error(MessageConstants.USER_EXIST_TASK);
        }
        log.info("删除用户认证信息");
        usersInfoService.removeById(id);
        log.info("删除用户认证信息成功");
        return Result.success(MessageConstants.USER_INFO_DELETE_SUCCESS);
    }
}
