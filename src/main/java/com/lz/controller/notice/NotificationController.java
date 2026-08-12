package com.lz.controller.notice;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lz.Exception.MyException;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.Enum.NotificationsType;
import com.lz.pojo.entity.Notifications;
import com.lz.pojo.entity.Users;
import com.lz.pojo.result.PageResult;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.NoticeItemVO;
import com.lz.pojo.vo.NoticeVO;
import com.lz.service.INotificationsService;
import com.lz.utils.EnumUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

/**
 * 通知接口（用户侧：列表/我的/已读；读接口共享）
 */
@RestController
@RequestMapping("/notifications")
@Slf4j
@Api(tags = "通知接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class NotificationController {

    @Autowired
    private INotificationsService notificationsService;

    @Autowired
    private UsersMapper usersMapper;

    @GetMapping
    public Result<?> list(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
            @RequestParam(value = "createdAt", required = false) @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd") Date createAt,
            @RequestParam(value = "messageType", required = false) String messageType,
            @RequestParam(value = "description", required = false) String description) {
        Page<Notifications> page = new Page<>(pageNum, pageSize);

        // messageType 兼容 dbValue / webValue / 枚举名，统一翻译为 dbValue 再过滤
        String dbMessageType = messageType;
        if (messageType != null && !messageType.trim().isEmpty()) {
            try {
                NotificationsType type = NotificationsType.fromValue(messageType.trim());
                dbMessageType = type == null ? null : type.getDbValue();
            } catch (IllegalArgumentException e) {
                dbMessageType = messageType;
            }
        }

        Page<Notifications> notificationsPage = notificationsService.selectList(page, createAt, dbMessageType,
                description);
        log.info("分页查询结果：{}", notificationsPage);
        return Result
                .success(new PageResult<Notifications>(notificationsPage.getTotal(), notificationsPage.getRecords()));
    }

    /**
     * 获取类型
     */
    @GetMapping("/types")
    public Result<?> getType() {
        Map<String, String> map = EnumUtils.generateKeyValues(NotificationsType.values());
        return Result.success(map);
    }

    /**
     * 按 ID 获取
     */
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable("id") Long id) {
        log.info("根据id查询通知信息：{}", id);
        Notifications notifications = notificationsService.getById(id);
        return Result.success(notifications);
    }

    @GetMapping("/by-type/{str}")
    public Result<?> getNotificationsByIdANDType(@PathVariable("str") String str) throws MyException {
        log.info("获取通知信息{}", str);

        List<NoticeItemVO> list = notificationsService.getNoticeType(str);
        return Result.success(list);
    }

    /**
     * 按 ID 获取通知详情
     */
    @GetMapping("/info/{id}")
    public Result<?> getNotificationsById(@PathVariable("id") Long id) {
        log.info("根据id查询通知信息：{}", id);
        NoticeVO noticeVO = notificationsService.getInfoById(id);
        return Result.success(noticeVO);
    }

    /**
     * 当前登录用户的消息中心（分页）
     */
    @GetMapping("/my")
    public Result<?> my(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Users user = usersMapper.getByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName());
        Page<NoticeItemVO> page = new Page<>(pageNum, pageSize);
        IPage<NoticeItemVO> result = notificationsService.myPage(page, user.getUserId());
        return Result.success(new PageResult<>(result.getTotal(), result.getRecords()));
    }

    /**
     * 标记某条通知为已读
     */
    @PutMapping("/{id}/read")
    public Result<?> markRead(@PathVariable("id") Long id) {
        Users user = usersMapper.getByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName());
        boolean ok = notificationsService.markRead(id, user.getUserId());
        return ok ? Result.success("已标记为已读") : Result.error("标记失败，通知不存在");
    }
}
