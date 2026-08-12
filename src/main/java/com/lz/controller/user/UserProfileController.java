package com.lz.controller.user;

import com.lz.Exception.MyException;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.PassWordDTO;
import com.lz.pojo.entity.Users;
import com.lz.pojo.result.Result;
import com.lz.service.IUsersService;
import com.lz.utils.ValidateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户自助接口（基础资料/密码）
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*", allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Api(tags = "用户自助接口")
@Slf4j
public class UserProfileController {

    @Autowired
    private IUsersService usersService;

    /**
     * 获取用户信息
     */
    @GetMapping("/profile/{id}")
    @ApiOperation("获取用户信息")
    public Result<Users> getUserInfo(@PathVariable Long id) {
        log.info("获取用户信息:{}", id);
        Users users = usersService.getById(id);

        return Result.success(users);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    @ApiOperation("更新用户信息")
    public Result<String> updateUserInfo(@RequestBody Users users) {
        log.info("更新用户信息:{}", users);
        usersService.updateById(users);
        return Result.success(MessageConstants.USER_UPDATE_SUCCESS);
    }

    @ApiOperation("修改密码")
    @PutMapping("/password")
    public Result<String> editPassword(@Validated @RequestBody PassWordDTO passWordDTO,
            BindingResult result) throws MyException {
        // 校验结果
        if (ValidateUtil.validate(result) != null) {
            log.info("用户修改密码校验失败:{}", ValidateUtil.validate(result));
            return Result.error(ValidateUtil.validate(result));
        }
        log.info("修改密码:{}", passWordDTO);
        usersService.editPassword(passWordDTO);
        return Result.success(MessageConstants.PASSWORD_UPDATE_SUCCESS);
    }
}
