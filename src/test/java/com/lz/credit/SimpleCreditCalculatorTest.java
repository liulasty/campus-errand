package com.lz.credit;

import com.lz.credit.dto.CreditMetrics;
import com.lz.credit.strategy.SimpleCreditCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SimpleCreditCalculator 算法单测（纯函数，无需 Spring 容器）
 *
 * @author lz
 */
class SimpleCreditCalculatorTest {

    private final SimpleCreditCalculator calculator = new SimpleCreditCalculator();

    private CreditMetrics metrics(Long accepted, Long completed, Double ratingAvg) {
        return CreditMetrics.builder()
                .acceptedCount(accepted)
                .completedCount(completed)
                .ratingAvg(ratingAvg)
                .build();
    }

    @Test
    void newUser_returnsDefaultScore() {
        assertEquals(60, calculator.calculate(metrics(0L, 0L, null)));
    }

    @Test
    void onlyRating_fullScore() {
        assertEquals(100, calculator.calculate(metrics(0L, 0L, 5.0)));
    }

    @Test
    void onlyRating_neutralCompletion() {
        assertEquals(88, calculator.calculate(metrics(0L, 0L, 4.0)));
    }

    @Test
    void acceptedButNotCompleted() {
        assertEquals(56, calculator.calculate(metrics(2L, 1L, null)));
    }

    @Test
    void fullPerformance() {
        assertEquals(100, calculator.calculate(metrics(2L, 2L, 5.0)));
    }

    @Test
    void roundingBoundary() {
        assertEquals(78, calculator.calculate(metrics(2L, 1L, 4.8)));
    }
}
