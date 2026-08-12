package com.lz.controller.notice;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lz.pojo.entity.SystemAnnouncements;
import com.lz.pojo.result.PageResult;
import com.lz.pojo.result.Result;
import com.lz.service.ISystemAnnouncementsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统公告接口（用户侧读）
 */
@RestController
@Slf4j
@Api(tags = "系统公告接口")
@RequestMapping("/announcements")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class AnnouncementController {

    @Autowired
    private ISystemAnnouncementsService systemAnnouncementsService;

    /**
     * 系统分页查询列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public Result<PageResult<SystemAnnouncements>> list(@RequestParam(value = "pageNum",
            defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "queryRules", required = false) Integer queryRules) {
        Page<SystemAnnouncements> page = new Page<>(pageNum, pageSize);
        IPage<SystemAnnouncements> pageResult = systemAnnouncementsService.page(page, status, description,
                queryRules);
        PageResult<SystemAnnouncements> systemAnnouncementsPageResult = new PageResult<>();
        systemAnnouncementsPageResult.setTotal(pageResult.getTotal());
        systemAnnouncementsPageResult.setRecords(pageResult.getRecords());
        return Result.success(systemAnnouncementsPageResult);
    }

    @GetMapping("/{id}")
    public Result<SystemAnnouncements> getById(@PathVariable("id") Integer id) {
        SystemAnnouncements systemAnnouncements = systemAnnouncementsService.getById(id);
        return Result.success(systemAnnouncements);
    }
}
