package com.lz.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.lz.Annotation.NoReturnHandle;
import com.lz.Exception.MyException;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.ReviewsDTO;
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
 * <p>
 * 存储用户对任务的评价信息 前端控制器
 * </p>
 *
 * @author lz
 * @since 2024-04-04
 */
@RestController
@RequestMapping("/reviews")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
@Api(tags = "评论控制器")
@Slf4j
public class ReviewsController {
    @Autowired
    private IReviewsService reviewsService;

    @PostMapping("/addReviews")
    public Result<?> addReviews(@RequestBody ReviewsDTO reviewsDTO) throws MyException {
        reviewsService.save(reviewsDTO);
        return Result.success(MessageConstants.REVIEWS_ADD_SUCCESS);
    }

    @GetMapping("/exportExcel")
    @NoReturnHandle
    public void exportExcel(HttpServletResponse response) {
        log.info("开始导出excel");

        List<Reviews> reviewsList = reviewsService.exportExcel();
        reviewsList.replaceAll(review -> review == null ? new Reviews() : review);
        EasyExcelUtil.exportExcel(response, "评论信息", "评论信息", "评论信息", reviewsList, Reviews.class);
        log.info("导出Excel成功");
    }

    @PostMapping("/clear")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ApiOperation("清空全部评价（测试支撑，仅管理员）")
    public Result<Integer> clearAll() {
        return Result.success(reviewsService.clearAll());
    }

    // 可选：保存到服务器磁盘的方法，路径应来自配置
    private void saveToDisk(List<Reviews> reviewsList) {
        // 假设路径来自配置
        String savePath = "D:\\reviews.xlsx";
        EasyExcel.write(savePath, Reviews.class).excelType(ExcelTypeEnum.XLSX).sheet("评论信息")
                .doWrite(reviewsList);
    }
}
