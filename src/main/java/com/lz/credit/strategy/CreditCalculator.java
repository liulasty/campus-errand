package com.lz.credit.strategy;

import com.lz.credit.dto.CreditMetrics;

/**
 * 信用分计算策略
 *
 * @author lz
 */
public interface CreditCalculator {

    /**
     * 输入原始指标，返回 0–100 信用分
     */
    int calculate(CreditMetrics metrics);
}
