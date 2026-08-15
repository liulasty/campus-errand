package com.lz.controller.credit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lz.credit.dto.CreditMetrics;
import com.lz.mapper.ReviewsMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.entity.Reviews;
import com.lz.pojo.entity.Users;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.CreditProfileVO;
import com.lz.pojo.vo.CreditReviewVO;
import com.lz.service.impl.CreditScoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户信用档案
 */
@RestController
@RequestMapping("/credit")
@Slf4j
@Api(tags = "用户信用档案")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class CreditController {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private ReviewsMapper reviewsMapper;

    @Autowired
    private CreditScoreService creditScoreService;

    @GetMapping
    @ApiOperation("当前用户信用档案")
    public Result<?> credit() {
        Users user = usersMapper.getByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName());
        Long userId = user.getUserId();

        CreditMetrics metrics = creditScoreService.loadMetrics(userId);
        int creditScore = creditScoreService.getScore(metrics);
        creditScoreService.saveScore(userId, creditScore);

        Long reviewCount = (long) reviewsMapper.selectCount(new QueryWrapper<Reviews>()
                .eq("AcceptorID", userId));
        Long goodCount = (long) reviewsMapper.selectCount(new QueryWrapper<Reviews>()
                .eq("AcceptorID", userId).ge("Rating", 4));
        List<CreditReviewVO> list = reviewsMapper.selectReviewsByAcceptor(userId);
        double goodRate = reviewCount > 0 ? Math.round(goodCount * 10000.0 / reviewCount) / 100.0 : 100.0;

        CreditProfileVO vo = CreditProfileVO.builder()
                .acceptTotal(metrics.getAcceptedCount())
                .completedTotal(metrics.getCompletedCount())
                .ratingAvg(metrics.getRatingAvg())
                .goodRate(goodRate)
                .reviewCount(reviewCount)
                .creditScore(creditScore)
                .reviewList(list)
                .build();
        return Result.success(vo);
    }
}
