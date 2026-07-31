package com.lz.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lz.mapper.ReviewsMapper;
import com.lz.mapper.TaskAcceptRecordsMapper;
import com.lz.mapper.TaskMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.Enum.AcceptStatus;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.entity.Reviews;
import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.TaskAcceptRecords;
import com.lz.pojo.entity.Users;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.CreditProfileVO;
import com.lz.pojo.vo.CreditReviewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户信用档案
 *
 * @author lz
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
    private TaskMapper taskMapper;

    @Autowired
    private TaskAcceptRecordsMapper taskAcceptRecordsMapper;

    @GetMapping
    @ApiOperation("当前用户信用档案")
    public Result<?> credit() {
        Users user = usersMapper.getByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName());
        Long userId = user.getUserId();

        Long acceptTotal = (long) taskAcceptRecordsMapper.selectCount(new QueryWrapper<TaskAcceptRecords>()
                .eq("AccepterId", userId).eq("status", AcceptStatus.CHECKED.getDbValue()));
        Long completedTotal = (long) taskMapper.selectCount(new QueryWrapper<Task>()
                .eq("ReceiverID", userId).eq("STATUS", TaskStatus.COMPLETED.getDbValue()));
        Long reviewCount = (long) reviewsMapper.selectCount(new QueryWrapper<Reviews>()
                .eq("AcceptorID", userId));
        Long goodCount = (long) reviewsMapper.selectCount(new QueryWrapper<Reviews>()
                .eq("AcceptorID", userId).ge("Rating", 4));
        Double ratingAvg = reviewsMapper.avgRatingByAcceptor(userId);
        List<CreditReviewVO> list = reviewsMapper.selectReviewsByAcceptor(userId);
        double goodRate = reviewCount > 0 ? Math.round(goodCount * 10000.0 / reviewCount) / 100.0 : 100.0;

        CreditProfileVO vo = CreditProfileVO.builder()
                .acceptTotal(acceptTotal)
                .completedTotal(completedTotal)
                .ratingAvg(ratingAvg)
                .goodRate(goodRate)
                .reviewCount(reviewCount)
                .reviewList(list)
                .build();
        return Result.success(vo);
    }
}
