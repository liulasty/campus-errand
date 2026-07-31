package com.lz.Exception;

/**
 * 未完成 L1 实名认证门禁异常
 *
 * @author lz
 */
public class UnauthorizedRealNameException extends RuntimeException {

    public static final String MESSAGE = "请先完成L1实名认证后再执行该操作";

    public UnauthorizedRealNameException() {
        super(MESSAGE);
    }
}
