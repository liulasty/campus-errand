package com.lz.credit.constant;

/**
 * 信用分常量
 *
 * @author lz
 */
public final class CreditConstant {

    public static final double RATING_WEIGHT = 0.6;
    public static final double COMPLETION_WEIGHT = 0.4;
    public static final int DEFAULT_USER_SCORE = 60;
    public static final int NEUTRAL_RATING_SCORE = 60;
    public static final int NEUTRAL_COMPLETION_RATE = 100;
    public static final int CREDIT_GOOD_MIN = 60;
    public static final int CREDIT_EXCELLENT_MIN = 80;

    private CreditConstant() {
    }
}
