package com.lz.controller.task.admin;

import com.lz.Annotation.NoReturnHandle;
import com.lz.pojo.entity.Reviews;
import com.lz.pojo.result.Result;
import com.lz.service.IReviewsService;
import com.lz.utils.excelutil.EasyExcelUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 评价管理接口（管理员：导出/清空）
 */
@RestController
@RequestMapping("/admin/reviews")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
@Api(tags = "评价管理接口")
@Slf4j
public class ReviewAdminController {
    @Autowired
    private IReviewsService reviewsService;

    @GetMapping("/export")
    @NoReturnHandle
    public void exportExcel(HttpServletResponse response) {
        log.info("开始导出excel");

        List<Reviews> reviewsList = reviewsService.exportExcel();
        reviewsList.replaceAll(review -> review == null ? new Reviews() : review);
        EasyExcelUtil.exportExcel(response, "评论信息", "评论信息", reviewsList, Reviews.class);
        log.info("导出Excel成功");
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @ApiOperation("清空全部评价（测试支撑，仅管理员）")
    public Result<Integer> clearAll() {
        return Result.success(reviewsService.clearAll());
    }
}
