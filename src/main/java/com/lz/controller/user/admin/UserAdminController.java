package com.lz.controller.user.admin;

import com.lz.Annotation.NoReturnHandle;
import com.lz.Exception.MyException;
import com.lz.pojo.Page.UsersConfig;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.entity.Users;
import com.lz.pojo.result.PageResult;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.UserExportVO;
import com.lz.service.IUsersService;
import com.lz.utils.excelutil.EasyExcelUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理接口（管理员）
 */
@RestController
@RequestMapping("/admin/users")
@CrossOrigin(origins = "*", allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Api(tags = "用户管理接口")
@Slf4j
public class UserAdminController {

    @Autowired
    private IUsersService usersService;

    @GetMapping("/export")
    @ApiOperation("导出用户列表 Excel")
    @NoReturnHandle
    public void exportExcel(HttpServletResponse response) throws MyException {
        List<Users> users = usersService.list();
        List<UserExportVO> rows = users.stream().map(u -> UserExportVO.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .email(u.getEmail())
                .role(u.getRole())
                .isActive(u.getIsActive())
                .isEnabled(u.getIsEnabled())
                .createTime(u.getCreateTime())
                .build()).collect(Collectors.toList());
        EasyExcelUtil.exportExcel(response, "用户列表", "用户列表", rows, UserExportVO.class);
    }

    /**
     * 按页面获取用户信息
     */
    @GetMapping
    @ApiOperation("分页查询用户信息")
    public Result<PageResult> getUserInfoByPage(@RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String isActive,
            @RequestParam(required = false)
            @ApiParam(value = "认证状态（英文枚举名）：AUTHENTICATING / AUTHENTICATED / AUTHENTICATION_FAILED / UNAUTHORIZED")
            String authStatus,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("分页查询用户信息:{}", isActive);
        Boolean is = null;
        if (isActive != null && !"".equals(isActive)) {
            is = "TRUE".equals(isActive);
        }
        UsersConfig config = UsersConfig.builder().username(username).email(email).isActive(is)
                .authStatus(authStatus).pageNum(pageNum).pageSize(pageSize).build();
        log.info("分页查询用户信息:{}", config);
        return Result.success(usersService.getUserByPage(config));
    }

    @PutMapping
    @ApiOperation("管理员更新用户信息")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<String> updateUserInfoByAdmin(@RequestBody Users users) {
        log.info("管理员更新用户信息:{}", users);
        usersService.updateById(users);
        return Result.success(MessageConstants.USER_UPDATE_SUCCESS);
    }

    /**
     * 按 ID 删除用户信息
     */
    @DeleteMapping("/{id}")
    @ApiOperation("根据用户id删除用户信息")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<String> deleteUserInfoById(@PathVariable Long id) {
        log.info("根据用户id删除用户信息:{}", id);
        usersService.removeById(id);
        return Result.success(MessageConstants.USER_DELETE_SUCCESS);
    }

    @PutMapping("/{id}/activate")
    @ApiOperation("管理员激活用户")
    public Result<String> adminActivation(@PathVariable Long id) throws MyException {
        log.info("管理员激活用户");
        Users user = usersService.getById(id);
        if (user == null) {
            log.error("用户认证信息不存在");
            return Result.error(MessageConstants.USER_AUTHENTICATION_INFO_NOT_EXIST);
        }

        if (!usersService.adminActivation(id)) {
            throw new MyException(MessageConstants.USER_ACTIVE_FAIL);
        }
        log.info("管理员激活用户成功");
        return Result.success(MessageConstants.ADMIN_ACTIVE_USER_SUCCESS);
    }

    /**
     * 管理员禁用用户
     */
    @PutMapping("/{id}/disable")
    @ApiOperation("管理员禁用用户")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<String> disableUserByAdmin(@PathVariable Long id) throws MyException {
        log.info("管理员禁用用户:{}", id);
        usersService.disableUser(id);
        return Result.success(MessageConstants.USER_DISABLE_SUCCESS);
    }

    /**
     * 管理员取消禁用用户
     */
    @PutMapping("/{id}/enable")
    @ApiOperation("管理员取消禁用用户")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<String> cancelDisableUserByAdmin(@PathVariable Long id) throws MyException {
        log.info("管理员取消禁用用户:{}", id);
        usersService.cancelDisableUser(id);
        return Result.success(MessageConstants.USER_ABLE_SUCCESS);
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    @ApiOperation("重置密码")
    public Result<String> resetPassword(@RequestBody Users users) throws MyException {
        log.info("重置密码:{}", users);
        usersService.resetPassword(users);
        return Result.success(MessageConstants.USER_UPDATE_SUCCESS);
    }

    @DeleteMapping
    @ApiOperation("批量删除用户")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<String> deleteAccounts(@RequestBody int[] deleteUsers) throws MyException {
        log.info("删除用户:{}", deleteUsers);
        usersService.deleteUsers(deleteUsers);
        return Result.success(MessageConstants.USER_DELETE_SUCCESS);
    }
}
