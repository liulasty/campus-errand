package com.lz.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lz.Exception.MyException;
import com.lz.mapper.ReviewsMapper;
import com.lz.mapper.TaskMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.dto.ReviewsDTO;
import com.lz.pojo.entity.Reviews;
import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.Users;
import com.lz.service.IReviewsService;

/**
 * <p>
 * 存储用户对任务的评价信息 服务实现类
 * </p>
 *
 * @author lz
 * @since 2024-04-04
 */
@Service
public class ReviewsServiceImpl extends ServiceImpl<ReviewsMapper, Reviews> implements IReviewsService {
    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ReviewsMapper reviewsMapper;

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private CreditScoreService creditScoreService;

    @Autowired
    private RealNameAuthenticationService realNameAuthenticationService;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Override
    public void save(ReviewsDTO reviewsDTO) throws MyException {
        realNameAuthenticationService.ensureCurrentUserL1();
        Task task = taskMapper.selectById(reviewsDTO.getTaskId());
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new RuntimeException("任务未完成，不能评价");
        }

        // 重复评价守卫：同一评价者同一任务只能评价一次
        Integer dup = reviewsMapper.selectCount(new QueryWrapper<Reviews>()
                .eq("TaskID", reviewsDTO.getTaskId())
                .eq("ReviewerID", getCurrentAdmin().getUserId()));
        if (dup != null && dup > 0) {
            throw new MyException("您已评价过该任务");
        }

        // 评分范围守卫（1-5）
        if (reviewsDTO.getRate() == null || reviewsDTO.getRate() < 1 || reviewsDTO.getRate() > 5) {
            throw new MyException("评分必须在1-5之间");
        }

        // 敏感词守卫
        if (reviewsDTO.getComment() != null
                && !sensitiveWordService.check(reviewsDTO.getComment()).isEmpty()) {
            throw new MyException("评价内容包含敏感词，请修改后重试");
        }

        Reviews reviews = Reviews.builder()
                .taskId(reviewsDTO.getTaskId())
                .reviewerId(getCurrentAdmin().getUserId())
                .acceptorId(task.getReceiverId())
                .publisherId(task.getOwnerId())
                .comment(reviewsDTO.getComment())
                .rating(reviewsDTO.getRate())
                .isApproved(false)
                .build();
        reviewsMapper.insert(reviews);
        // 被评人信用分变化，重算并持久化
        creditScoreService.recomputeAndSave(task.getReceiverId());

    }

    @Override
    public List<Reviews> exportExcel() {
        return reviewsMapper.selectList(null);
    }

    @Override
    public int clearAll() {
        return reviewsMapper.delete(null);
    }

    public Users getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminName = authentication.getName();
        // log.info("管理员: {}", adminName);

        return usersMapper.getByUsername(adminName);
    }
}