package com.lz.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lz.Exception.MyException;
import com.lz.mapper.UsersInfoMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.Enum.AuthenticationStatus;
import com.lz.pojo.Enum.VerifyResult;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.UserInfoDTO;
import com.lz.pojo.entity.Users;
import com.lz.pojo.entity.UsersInfo;
import com.lz.service.IUsersInfoService;
import com.lz.verifier.IIdentityVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Date;

/**
 * <p>
 * 存储系统用户详细信息 服务实现类
 * </p>
 *
 * @author lz
 * @since 2024-04-04
 */
@Service
public class UsersInfoServiceImpl extends ServiceImpl<UsersInfoMapper,
        UsersInfo> implements IUsersInfoService {
    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private IIdentityVerifier identityVerifier;

    /**
     * 提交认证信息
     *
     * @param dto DTO
     */
    @Override
    @PostMapping
    public void submitCertificationInformation(UserInfoDTO dto) throws MyException {
        Users byId = usersMapper.selectById(dto.getId());
        if (byId == null) {
            throw new MyException("用户不存在");
        }
        // 入参非空校验：身份标识、姓名、角色
        if (dto.getIdentityNo() == null || dto.getIdentityNo().trim().isEmpty()
                || dto.getName() == null || dto.getRole() == null) {
            throw new MyException("身份标识、姓名、角色不能为空");
        }
        VerifyResult result = identityVerifier.verify(dto.getIdentityNo(), dto.getName(), dto.getRole());

        UsersInfo usersInfo = UsersInfo.builder()
                .roleImgSrc(dto.getImgUrl())
                .name(dto.getName())
                .qqNumber(dto.getQq())
                .userId(dto.getId())
                .phoneNumber(dto.getPhone())
                .userRole(dto.getRole())
                .identityNo(dto.getIdentityNo())
                .authStatus(AuthenticationStatus.AUTHENTICATING)
                .certifieTime(new Date(System.currentTimeMillis()))
                .build();
        // edu 模式自动审核：PASS 直接 L1；REJECT 回认证失败（预留）
        if (result == VerifyResult.PASS) {
            usersInfo.setAuthStatus(AuthenticationStatus.AUTHENTICATED);
            usersInfo.setAuthLevel(1);
            usersInfo.setCertifiedTime(new Date(System.currentTimeMillis()));
        } else if (result == VerifyResult.REJECT) {
            usersInfo.setAuthStatus(AuthenticationStatus.AUTHENTICATION_FAILED);
        }
        save(usersInfo);
    }

    /**
     * 确认通过审核
     *
     * @param id 同上
     *
     * @return {@code Boolean}
     */
    @Override
    public Boolean confirmToPassTheReview(Long id) throws MyException {
        UsersInfo usersInfo = getById(id);
        if (usersInfo != null) {
            if (usersInfo.getAuthStatus() != AuthenticationStatus.AUTHENTICATING){
                throw new MyException(MessageConstants.USER_STATUS_ERROR);
            }
            usersInfo.setAuthStatus(AuthenticationStatus.AUTHENTICATED);
            usersInfo.setAuthLevel(1);
            usersInfo.setRejectReason(null);
            usersInfo.setCertifiedTime(new Date(System.currentTimeMillis()));
            return updateById(usersInfo);
        }else {
            throw new MyException(MessageConstants.USER_NOT_EXIST);
        }
    }

    /**
     * 拒绝通过审核
     *
     * @param id     同上
     * @param reason 驳回原因
     *
     * @return {@code Boolean}
     */
    @Override
    public Boolean refuseToPassReview(Long id, String reason) throws MyException {
        UsersInfo usersInfo = getById(id);
        if (usersInfo != null) {
            if (usersInfo.getAuthStatus() != AuthenticationStatus.AUTHENTICATING){
                throw new MyException(MessageConstants.USER_STATUS_ERROR);
            }
            usersInfo.setAuthStatus(AuthenticationStatus.AUTHENTICATION_FAILED);
            usersInfo.setAuthLevel(0);
            usersInfo.setRejectReason(reason);
            usersInfo.setCertifiedTime(new Date(System.currentTimeMillis()));
            return updateById(usersInfo);
        }else {
            throw new MyException(MessageConstants.USER_NOT_EXIST);
        }
    }

    /**
     * 取消用户信息身份验证
     *
     * @param id 同上
     *
     * @return {@code Boolean}
     */
    @Override
    public Boolean cancelUserInfoAuthentication(Long id) throws MyException {
        UsersInfo usersInfo = getById(id);
        if (usersInfo == null) {
            throw new MyException(MessageConstants.USER_NOT_EXIST);
        }
        if (usersInfo.getAuthStatus() == AuthenticationStatus.AUTHENTICATED) {
            usersInfo.setAuthStatus(AuthenticationStatus.AUTHENTICATION_FAILED);
            usersInfo.setCertifiedTime(new Date(System.currentTimeMillis()));
            return updateById(usersInfo);
        }
        return false;
    }

    
}