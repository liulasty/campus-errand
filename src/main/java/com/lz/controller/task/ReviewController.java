package com.lz.controller.task;

import com.lz.Exception.MyException;
import com.lz.pojo.constants.MessageConstants;
import com.lz.pojo.dto.ReviewsDTO;
import com.lz.pojo.result.Result;
import com.lz.service.IReviewsService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 委托评价接口（用户提交评价）
 */
@RestController
@RequestMapping("/reviews")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
@Api(tags = "评价接口")
@Slf4j
public class ReviewController {
    @Autowired
    private IReviewsService reviewsService;

    @PostMapping
    public Result<?> addReviews(@RequestBody ReviewsDTO reviewsDTO) throws MyException {
        reviewsService.save(reviewsDTO);
        return Result.success(MessageConstants.REVIEWS_ADD_SUCCESS);
    }
}
