package com.lz.credit.strategy;

import com.lz.credit.constant.CreditConstant;
import com.lz.credit.dto.CreditMetrics;
import org.springframework.stereotype.Component;

/**
 * v1 信用分计算：均分 60% + 完成率 40%
 *
 * @author lz
 */
@Component
public class SimpleCreditCalculator implements CreditCalculator {

    @Override
    public int calculate(CreditMetrics metrics) {
        long accepted = metrics.getAcceptedCount() == null ? 0L : metrics.getAcceptedCount();
        long completed = metrics.getCompletedCount() == null ? 0L : metrics.getCompletedCount();
        Double ratingAvg = metrics.getRatingAvg();

        if (accepted == 0 && ratingAvg == null) {
            return CreditConstant.DEFAULT_USER_SCORE;
        }

        double rating = ratingAvg == null
                ? CreditConstant.NEUTRAL_RATING_SCORE
                : Math.round(ratingAvg / 5.0 * 100);
        double completion = accepted == 0
                ? CreditConstant.NEUTRAL_COMPLETION_RATE
                : Math.round((double) completed / accepted * 100);

        return (int) Math.round(CreditConstant.RATING_WEIGHT * rating
                + CreditConstant.COMPLETION_WEIGHT * completion);
    }
}
