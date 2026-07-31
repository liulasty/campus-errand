package com.lz.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户信用档案
 *
 * @author lz
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditProfileVO {
    /** 接单总数 */
    private Long acceptTotal;
    /** 已完成接单数 */
    private Long completedTotal;
    /** 平均评分 */
    private Double ratingAvg;
    /** 好评率（评分>=4 占比，百分比） */
    private Double goodRate;
    /** 评价总数 */
    private Long reviewCount;
    /** 历史评价列表 */
    private List<CreditReviewVO> reviewList;

    /** 信用分（0–100，可空，前端兜底 60） */
    private Integer creditScore;
}
