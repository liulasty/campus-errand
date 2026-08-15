package com.lz.service;

import com.lz.Exception.UnauthorizedRealNameException;
import com.lz.mapper.UsersInfoMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.entity.Users;
import com.lz.pojo.entity.UsersInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 实名认证门禁服务
 *
 * @author lz
 */
@Service
public class RealNameAuthenticationService {

    @Autowired
    private UsersInfoMapper usersInfoMapper;

    @Autowired
    private UsersMapper usersMapper;

    /** 校验指定用户是否已 L1 实名认证（auth_level ≥ 1），否则抛门禁异常 */
    public void ensureL1(Long userId) {
        UsersInfo info = usersInfoMapper.selectById(userId);
        int level = info != null && info.getAuthLevel() != null ? info.getAuthLevel() : 0;
        if (level < 1) {
            throw new UnauthorizedRealNameException();
        }
    }

    /** 校验当前登录用户是否已 L1 实名认证 */
    public void ensureCurrentUserL1() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = usersMapper.getByUsername(username);
        ensureL1(user.getUserId());
    }
}
