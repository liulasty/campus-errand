package com.lz.controller.user;

import com.lz.Annotation.NoReturnHandle;
import com.lz.Exception.MyException;
import com.lz.common.security.JwtTokenBlacklist;
import com.lz.config.AppConfig;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.UserDTO;
import com.lz.pojo.dto.UserLoginDTO;
import com.lz.pojo.entity.Users;
import com.lz.pojo.entity.UsersInfo;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.UserLoginVO;
import com.lz.service.IUsersInfoService;
import com.lz.service.IUsersService;
import com.lz.utils.JwtUtil;
import com.lz.utils.ValidateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * 认证接口（登录/注册/登出/登录态检查）
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@Api(tags = "认证接口")
@Slf4j
public class AuthController {

    private static final String ROLE_ADMIN = "ADMIN";

    @Autowired
    private IUsersService usersService;

    @Autowired
    private IUsersInfoService userInfoService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private JwtTokenBlacklist jwtTokenBlacklist;

    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    @ApiOperation("登录")
    @NoReturnHandle
    public Result<UserLoginVO> login(@Validated @RequestBody UserLoginDTO userLoginDTO,
            BindingResult result) throws MyException {
        log.info("用户登录:{},用户密码 {}", userLoginDTO.getUsername(), userLoginDTO.getPassword());
        // 校验结果
        if (ValidateUtil.validate(result) != null) {
            log.info("用户登录校验失败:{}", ValidateUtil.validate(result));
            return Result.error(ValidateUtil.validate(result));
        }
        {
            // 使用Spring Security进行身份验证
            Authentication authentication =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                            userLoginDTO.getUsername(), userLoginDTO.getPassword()));
            log.info("用户登录成功:{}", authentication);

            // 设置认证上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取当前用户的角色信息
            Users user = usersService.getByUsername(userLoginDTO.getUsername());

            HashMap<String, Object> claims = new HashMap<>();
            claims.put("username", user.getUsername());
            claims.put("role", user.getRole());
            String token = JwtUtil.genToken(claims, appConfig.getJwtKey());
            UsersInfo usersInfo = null;
            if (user.getRole() != ROLE_ADMIN) {
                usersInfo = userInfoService.getById(user.getUserId());
            }

            UserLoginVO loginVO = UserLoginVO.builder().userId(user.getUserId())
                    .userType(user.getRole())
                    .Authorization(
                            usersInfo == null ? null : usersInfo.getAuthStatus().getDescription())
                    .token(token).build();
            return Result.success(loginVO, MessageConstants.USER_LOGIN_SUCCESS);
        }
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    @ApiOperation("注册")
    public Result<String> register(@Validated @RequestBody UserDTO userDTO, BindingResult result) {
        log.info("用户注册:{}", userDTO);
        // 校验结果
        if (ValidateUtil.validate(result) != null) {
            return Result.error(ValidateUtil.validate(result));
        }

        boolean register = usersService.register(userDTO);

        if (!register) {
            return Result.error(MessageConstants.USER_REGISTER_FAIL);
        } else {
            return Result.success(MessageConstants.USER_REGISTER_SUCCESS);
        }
    }

    @DeleteMapping("/logout")
    @ApiOperation("登出")
    public Result<String> logout(HttpServletRequest request) {
        log.info("用户登出请求");

        // 封装登出逻辑到独立的方法
        boolean logoutSuccess = tryLogout(request);

        // 根据登出结果返回相应的消息
        return logoutSuccess ? Result.success(MessageConstants.USER_LOGOUT_SUCCESS)
                : Result.error(MessageConstants.USER_LOGOUT_FAILURE);
    }

    /**
     * 尝试注销
     */
    private boolean tryLogout(HttpServletRequest request) {
        String token = request.getHeader("JWT");
        if (StringUtils.hasText(token)) {
            jwtTokenBlacklist.add(token);
            SecurityContextHolder.clearContext();
            log.info("用户登出成功");
            return true;
        }
        log.warn("登出请求未携带 JWT");
        return false;
    }

    @GetMapping("/check")
    @ApiOperation("检查用户是否登录")
    public Result<String> check(HttpServletRequest request) {
        log.info("检查用户是否登录");
        String token = request.getHeader("JWT");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = StringUtils.hasText(token)
                && !jwtTokenBlacklist.contains(token)
                && authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        return loggedIn ? Result.success("用户已登录") : Result.error("用户未登录");
    }
}
