package com.lz.controller.notice.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.lz.Exception.MyException;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.NotificationDTO;
import com.lz.pojo.dto.SendDataDTO;
import com.lz.pojo.entity.Notifications;
import com.lz.pojo.result.Result;
import com.lz.service.INotificationsService;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

/**
 * 通知管理接口（管理员：新增/修改/发送/删除）
 */
@RestController
@RequestMapping("/admin/notifications")
@Slf4j
@Api(tags = "通知管理接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class NotificationAdminController {

    @Autowired
    private INotificationsService notificationsService;

    /**
     * 添加通知消息
     */
    @PostMapping
    public Result<?> add(@RequestBody NotificationDTO notificationDTO) throws MyException {
        log.info("添加通知信息：{}", notificationDTO);
        notificationsService.add(notificationDTO);

        return Result.success(MessageConstants.ADD_MESSAGE_SUCCESS);
    }

    @PutMapping
    public Result<?> update(@RequestBody Notifications notifications) throws MyException {
        log.info("更新通知信息：{}", notifications);
        notificationsService.updateById(notifications);

        return Result.success(MessageConstants.ADD_MESSAGE_SUCCESS);
    }

    /**
     * 发送通知
     */
    @PostMapping("/send")
    public Result<?> send(
            @RequestBody SendDataDTO sendData) throws MyException {
        log.info("发送通知信息：{}", sendData);
        notificationsService.send(sendData);
        return Result.success(MessageConstants.ADD_MESSAGE_SUCCESS);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") Long id) {
        log.info("根据id删除通知信息：{}", id);
        notificationsService.delNotification(id);
        return Result.success(MessageConstants.DELETE_MESSAGE_SUCCESS);
    }
}
