package com.lz.controller.common;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.lz.context.BaseContext;
import com.lz.pojo.entity.IpLogs;
import com.lz.pojo.result.Result;
import com.lz.service.IpLogsService;
import com.lz.utils.IpUtil;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

/**
 * 公共工具接口
 */
@RestController
@RequestMapping("/common")
@Api(tags = "公共接口")
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class CommonController {

    @Autowired
    private IpLogsService ipLogsService;

    /**
     * 获取当前登录用户的IP地址
     */
    @GetMapping("/ip")
    public Result<?> getIp(HttpServletRequest request) {
        String ipAdrress = IpUtil.getIpAdrress(request);
        Long currentId = BaseContext.getCurrentId();
        log.info("当前登录用户ID：{}", currentId);
        String clientIp = ((HttpServletRequest) request).getRemoteAddr(); // 获取客户端IP地址
        log.info("客户端IP地址：{}", clientIp);

        IpLogs ipLogs = IpLogs.builder().ip(clientIp)
                .userAgent(request.getHeader("User-Agent"))
                .visitTime(new Date(System.currentTimeMillis()))
                .build();
        ipLogsService.save(ipLogs);
        return Result.success((Object) clientIp);
    }
}
