package com.lz.controller.notice.admin;

import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.entity.SystemAnnouncements;
import com.lz.pojo.entity.Users;
import com.lz.pojo.result.Result;
import com.lz.service.ISystemAnnouncementsService;
import com.lz.service.IUsersService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 系统公告管理接口（管理员：新增/修改/删除）
 */
@RestController
@Slf4j
@Api(tags = "系统公告管理接口")
@RequestMapping("/admin/announcements")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class AnnouncementAdminController {

    @Autowired
    private IUsersService usersService;

    @Autowired
    private ISystemAnnouncementsService systemAnnouncementsService;

    @PostMapping
    @ApiOperation("新增系统公告")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<Long> create(@RequestBody SystemAnnouncements systemAnnouncements) {
        Users admin = getCurrentAdmin();
        Date now = new Date(System.currentTimeMillis());
        systemAnnouncements.setPublisherId(admin.getUserId());
        systemAnnouncements.setPublishTime(now);
        systemAnnouncements.setCreatedAt(now);
        systemAnnouncements.setUpdatedAt(now);
        systemAnnouncements.setUpdatedBy(admin.getUserId());
        if (systemAnnouncements.getStatus() == null || systemAnnouncements.getStatus().trim().isEmpty()) {
            systemAnnouncements.setStatus("DRAFT");
        }
        systemAnnouncementsService.save(systemAnnouncements);
        return Result.success(systemAnnouncements.getAnnouncementId(),
                MessageConstants.SYSTEM_ANNOUNCEMENTS_ADD_SUCCESS);
    }

    @PutMapping
    public Result<String> update(@RequestBody SystemAnnouncements systemAnnouncements) {
        systemAnnouncements.setUpdatedAt(new Date(System.currentTimeMillis()))
                .setUpdatedBy(getCurrentAdmin().getUserId());
        log.info("更新系统公告 {}", systemAnnouncements);
        systemAnnouncementsService.updateById(systemAnnouncements);
        return Result.success(MessageConstants.TASK_SYSTEMANNOUNCEMENT_UPDATE_SUCCESS);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable("id") Integer id) {
        systemAnnouncementsService.removeById(id);
        return Result.success(MessageConstants.SYSTEM_ANNOUNCEMENTS_DELETE_SUCCESS);
    }

    public Users getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminName = authentication.getName();
        return usersService.getByUsername(adminName);
    }
}
