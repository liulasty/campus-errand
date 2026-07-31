package com.lz.mapper;

import com.lz.pojo.entity.Reviews;
import com.lz.pojo.vo.CreditReviewVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 存储用户对任务的评价信息 Mapper 接口
 * </p>
 *
 * @author lz
 * @since 2024-04-04
 */
@Mapper
public interface ReviewsMapper extends BaseMapper<Reviews> {

    /**
     * 作为接收者收到的平均评分
     */
    Double avgRatingByAcceptor(@Param("userId") Long userId);

    /**
     * 作为接收者收到的历史评价（关联评价人昵称与任务描述）
     */
    List<CreditReviewVO> selectReviewsByAcceptor(@Param("userId") Long userId);
}