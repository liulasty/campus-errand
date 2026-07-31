package com.lz.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信用档案 - 单条历史评价
 *
 * @author lz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditReviewVO {
    private Long reviewId;
    private Long taskId;
    private String taskDescription;
    private Long reviewerId;
    private String reviewerName;
    private Long rating;
    private String comment;
}
