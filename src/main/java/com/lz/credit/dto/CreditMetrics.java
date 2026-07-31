package com.lz.credit.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 信用分计算输入指标
 *
 * @author lz
 */
@Data
@Builder
public class CreditMetrics {

    /** 已确认接单数 */
    private Long acceptedCount;
    /** 已完成接单数 */
    private Long completedCount;
    /** 作为接收方收到的平均评分，可为 null */
    private Double ratingAvg;
}
