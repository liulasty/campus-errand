package com.lz.controller.user;

import com.lz.Exception.MyException;
import com.lz.pojo.Enum.AuthenticationStatus;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.UserInfoDTO;
import com.lz.pojo.entity.Users;
import com.lz.pojo.entity.UsersInfo;
import com.lz.pojo.result.Result;
import com.lz.service.IUsersInfoService;
import com.lz.service.IUsersService;
import com.lz.utils.IdentityMaskUtils;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 实名认证接口（提交/取消/本人查询）
 */
@RestController
@RequestMapping("/authentications")
@Slf4j
@Api(tags = "实名认证接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class AuthenticationController {
    @Autowired
    private IUsersInfoService usersInfoService;

    @Autowired
    private IUsersService usersService;

    /**
     * 查询实名资料
     *
     * @param id 同上
     *
     * @return {@code Result<UsersInfo>}
     */
    @GetMapping("/{id}")
    public Result<UsersInfo> userInfo(@PathVariable Long id) {
        UsersInfo usersInfo = usersInfoService.getById(id);
        if (usersInfo == null) {
            log.error("用户信息不存在");
            return Result.error("用户信息不存在");
        }
        // D-16 凭证脱敏：非本人查看他人实名资料时 identityNo 掩码（前4+掩码+末2）；本人查回明文（TC-018 契约）
        Users current = getCurrentUser();
        if (current == null || !current.getUserId().equals(usersInfo.getUserId())) {
            usersInfo.setIdentityNo(IdentityMaskUtils.mask(usersInfo.getIdentityNo()));
        }

        return Result.success(usersInfo);
    }

    /**
     * 获取当前登录用户
     */
    private Users getCurrentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersService.getByUsername(name);
    }

    /**
     * 提交认证申请
     *
     * @param dto DTO
     *
     * @return {@code Result<String>}
     *
     * @throws MyException 我的异常
     */
    @PostMapping
    public Result<String> save(@RequestBody UserInfoDTO dto) throws MyException {
        log.info("用户认证信息提交");
        UsersInfo usersInfo = usersInfoService.getById(dto.getId());
        if (usersInfo != null) {
            if (usersInfo.getAuthStatus() == AuthenticationStatus.AUTHENTICATION_FAILED) {
                log.info("重新申请认证");
                UsersInfo info = UsersInfo.builder()
                        .userId(dto.getId())
                        .name(dto.getName())
                        .authStatus(AuthenticationStatus.AUTHENTICATING)
                        .userRole(dto.getRole())
                        .qqNumber(dto.getQq())
                        .certifieTime(new Date(System.currentTimeMillis()))
                        .phoneNumber(dto.getPhone()).build();
                usersInfoService.updateById(info);
                return Result.success("重新认证信息已提交审核");
            } else {
                log.error("用户认证信息已存在");
                return Result.error("用户认证信息已存在");
            }
        } else {
            usersInfoService.submitCertificationInformation(dto);
            log.info("用户认证信息提交成功");
            return Result.success(MessageConstants.USER_UPDATE_SUCCESS);
        }
    }

    /**
     * 取消用户信息身份验证
     *
     * @param id 同上
     *
     * @return {@code Result<String>}
     *
     * @throws MyException 我的异常
     */
    @PutMapping("/{id}/cancel")
    public Result<String> cancelUserInfoAuthentication(@PathVariable Long id) throws MyException {
        UsersInfo usersInfo = usersInfoService.getById(id);
        if (usersInfo == null) {
            log.error("用户认证信息不存在");
            return Result.error(MessageConstants.USER_AUTHENTICATION_INFO_NOT_EXIST);
        }
        log.info("取消用户认证");
        if (!usersInfoService.cancelUserInfoAuthentication(id)) {
            throw new MyException(MessageConstants.USER_CANCEL_FAIL);
        }

        log.info("取消用户认证成功");
        return Result.success(MessageConstants.USER_CANCEL_SUCCESS);
    }
}
