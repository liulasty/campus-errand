package com.lz.service;

import com.lz.Exception.UnauthorizedRealNameException;
import com.lz.mapper.UsersInfoMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.entity.UsersInfo;
import com.lz.service.impl.RealNameAuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * RealNameAuthenticationService.ensureL1 单测
 *
 * @author lz
 */
@ExtendWith(MockitoExtension.class)
class RealNameAuthenticationServiceTest {

    @Mock
    private UsersInfoMapper usersInfoMapper;

    private RealNameAuthenticationService newService() {
        RealNameAuthenticationService s = new RealNameAuthenticationService();
        ReflectionTestUtils.setField(s, "usersInfoMapper", usersInfoMapper);
        return s;
    }

    @Test
    void ensureL1_authenticated_passes() {
        UsersInfo info = UsersInfo.builder().userId(1L).authLevel(1).build();
        when(usersInfoMapper.selectById(1L)).thenReturn(info);

        assertThatCode(() -> newService().ensureL1(1L)).doesNotThrowAnyException();
    }

    @Test
    void ensureL1_unauthenticated_throws() {
        UsersInfo info = UsersInfo.builder().userId(1L).authLevel(0).build();
        when(usersInfoMapper.selectById(1L)).thenReturn(info);

        assertThatThrownBy(() -> newService().ensureL1(1L))
                .isInstanceOf(UnauthorizedRealNameException.class);
    }

    @Test
    void ensureL1_noRecord_throws() {
        when(usersInfoMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> newService().ensureL1(1L))
                .isInstanceOf(UnauthorizedRealNameException.class);
    }
}
