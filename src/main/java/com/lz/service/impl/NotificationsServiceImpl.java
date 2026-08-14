package com.lz.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lz.Exception.MyException;
import com.lz.mapper.NotificationsMapper;
import com.lz.mapper.SystemAnnouncementsMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.Enum.NotificationsType;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.NoticeDTO;
import com.lz.pojo.dto.NotificationDTO;
import com.lz.pojo.dto.SendDataDTO;
import com.lz.pojo.entity.NotificationReadStatus;
import com.lz.pojo.entity.Notifications;
import com.lz.pojo.entity.Users;
import com.lz.pojo.vo.NoticeItemVO;
import com.lz.pojo.vo.NoticeVO;
import com.lz.service.INotificationReadStatusService;
import com.lz.service.INotificationsService;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 存储系统通知信息 服务实现类
 * </p>
 *
 * @author lz
 * @since 2024-04-04
 */
@Service
@Slf4j
public class NotificationsServiceImpl extends ServiceImpl<NotificationsMapper, Notifications>
        implements INotificationsService {

    @Autowired
    private SystemAnnouncementsMapper systemAnnouncementsMapper;

    @Autowired
    private NotificationsMapper notificationsMapper;

    @Autowired
    private INotificationReadStatusService notificationReadStatusService;

    @Autowired
    private UsersMapper usersMapper;

    /**
     * 按 ID 和时间获取通知
     */
    @Override
    public void getNotificationsByIdANDDate() {

    }

    @Override
    public void getNewestNotifications() {

    }

    /**
     * 分页查询列表
     *
     * @param page        页
     * @param createAt    创建位置
     * @param messageType 消息类型
     * @param description 描述
     *
     * @return 页面<通知>
     */
    @Override
    public Page<Notifications> selectList(Page<Notifications> page, Date createAt, String messageType,
            String description) {
        Page<Notifications> notificationsPage = notificationsMapper.selectPageAdmin(page,
                createAt, messageType,
                description);

        return notificationsPage;
    }

    @Override
    @Transactional()
    public void add(NotificationDTO notificationDTO) throws MyException {
        Notifications notifications = Notifications.builder()
                .notificationTime(new Date(System.currentTimeMillis()))
                .notificationType(NotificationsType.fromDbValue(notificationDTO.getType()))
                .title(notificationDTO.getTitle())
                .message(notificationDTO.getDescription())
                .userId(getCurrentAdmin().getUserId())
                .build();
        boolean save = save(notifications);
        if (!save) {
            log.error(MessageConstants.ADD_MESSAGE_FAILURE);
            throw new MyException(MessageConstants.ADD_MESSAGE_FAILURE);
        }
    }

    @Override
    public void send(SendDataDTO sendData) throws MyException {
        if (sendData.getSendObject().equals("all")) {
            notificationReadStatusService.sendAllNotification(sendData.getSendId());
            return;
        }

        if (sendData.getSendObject().equals("authenticated")) {
            notificationReadStatusService.sendAuthenticatedNotification(sendData.getSendId());
            return;
        }

        if (sendData.getSendObject().equals("student")) {
            notificationReadStatusService.sendStudentNotification(sendData.getSendId());
            return;
        }

        if (sendData.getSendObject().equals("teacher")) {
            notificationReadStatusService.sendTeacherNotification(sendData.getSendId());
            return;
        }

        if (sendData.getSendObject().equals("admin")) {
            notificationReadStatusService.sendAdminNotification(sendData.getSendId());
            return;
        }

        if (sendData.getSendObject().equals("other")) {
            notificationReadStatusService.sendOtherNotification(sendData.getSendId());
            return;
        }

        throw new MyException(MessageConstants.DATA_VALIDATION_ERROR);

    }

    @Override
    public Long addTaskDeleteNotification(Long userId, String msg) {
        Notifications notifications = Notifications.builder()
                .notificationTime(new Date(System.currentTimeMillis()))
                .notificationType(NotificationsType.TASK)
                .title("任务删除通知")
                .message(msg)
                .userId(userId)
                .build();
        notificationsMapper.insert(notifications);
        return notifications.getNotificationId();
    }

    @Override
    @Transactional
    public void delNotification(Long id) {
        removeById(id);
        notificationReadStatusService.delNotification(id);
    }

    @Override
    public void getNoticeByIdANDType(NoticeDTO noticeDTO) {

    }

    @Override
    public IPage<NoticeItemVO> getNoticeType(String str, Integer pageNum, Integer pageSize) throws MyException {
        log.info("通知类型: {}", NotificationsType.fromDbValue(str).getWebValue());

        Users users = getCurrentAdmin();
        Page<NoticeItemVO> page = new Page<>(pageNum, pageSize);
        return notificationsMapper.selectListByTypePage(page, users.getUserId(),
                NotificationsType.fromDbValue(str).getDbValue());
    }

    @Override
    public NoticeVO getInfoById(Long id) {
        if (id != null) {
            NoticeVO noticeVO = notificationsMapper.getInfoById(id);
            if (noticeVO != null && !noticeVO.getIsRead()) {
                NotificationReadStatus notificationReadStatus = new NotificationReadStatus();
                notificationReadStatus.setId(id);
                notificationReadStatus.setIsRead(true);
                notificationReadStatus.setReadTime(new Date(System.currentTimeMillis()));
                notificationReadStatusService.updateById(notificationReadStatus);
            }

            return noticeVO;
        }
        return null;
    }

    @Override
    public void addTaskConfirmTheRecipient(Long userId,
            String taskAcceptanceProcessedSuccess, NotificationsType task, String s, Long taskId) throws MyException {
        Notifications notifications = Notifications.builder()
                .userId(userId)
                .notificationTime(new Date(System.currentTimeMillis()))
                .notificationType(task)
                .message(s)
                .title(taskAcceptanceProcessedSuccess)
                .build();
        int insert = notificationsMapper.insert(notifications);
        if (insert == 0) {
            throw new MyException(MessageConstants.ADD_MESSAGE_FAILURE);
        }
        // D-19 关联修复：NotificationID 用 MyBatis 回填的自增 ID，而非 insert() 返回值（行数=1），
        // 否则 read_status 错误关联到 ID=1 的通知，接收人列表查不到本通知
        notificationReadStatusService.addTaskConfirmTheRecipient(notifications.getNotificationId(),
                taskId,
                userId,
                new Date(System.currentTimeMillis()));
    }

    @Override
    public void addTaskAcceptanceSelected(Long userId, String taskAcceptanceProcessedFailed, NotificationsType task,
            String s, Long taskId) throws MyException {
        Notifications notifications = Notifications.builder()
                .userId(userId)
                .notificationTime(new Date(System.currentTimeMillis()))
                .notificationType(task)
                .message(s)
                .title(taskAcceptanceProcessedFailed)
                .build();
        int insert = notificationsMapper.insert(notifications);
        if (insert == 0) {
            throw new MyException(MessageConstants.ADD_MESSAGE_FAILURE);
        }
        // D-19 关联修复：NotificationID 用 MyBatis 回填的自增 ID，而非 insert() 返回值（行数=1），
        // 否则 read_status 错误关联到 ID=1 的通知，接收人列表查不到本通知
        notificationReadStatusService.addTaskConfirmTheRecipient(notifications.getNotificationId(),
                taskId,
                userId,
                new Date(System.currentTimeMillis()));
    }

    @Override
    public void addTaskAuditNotificationService(String reviewStatus) {

    }

    @Override
    public void sendTaskNotification(String title, String updateDescription, Long taskId, Long ownerId) {
        // D-19：notifications.UserID 列语义为「接收通知的用户ID」，ownerId 即接收人；
        // 不再覆盖为当前管理员（修复撤回/超时通知归属错位，与 T9 确认接收人路径一致）
        Notifications notifications = Notifications.builder().title(title)
                .message(updateDescription)
                .notificationTime(new Date(System.currentTimeMillis()))
                .notificationType(NotificationsType.TASK)
                .userId(ownerId)
                .build();

        notificationsMapper.insert(notifications);

        // 在 NotificationReadStatus 中建立通知与接收用户(ownerId)的关系
        notificationReadStatusService.addTaskNotification(notifications.getNotificationId(), taskId,
                ownerId);

    }

    @Override
    public IPage<NoticeItemVO> myPage(Page<NoticeItemVO> page, Long userId) {
        return notificationsMapper.selectMyPage(page, userId);
    }

    @Override
    public boolean markRead(Long id, Long userId) {
        return notificationReadStatusService.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<NotificationReadStatus>()
                        .eq("id", id).eq("UserId", userId)
                        .set("IsRead", true)
                        .set("ReadTime", new Date(System.currentTimeMillis())));
    }

    public Users getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminName = authentication.getName();
        // log.info("管理员: {}", adminName);

        return usersMapper.getByUsername(adminName);
    }

}