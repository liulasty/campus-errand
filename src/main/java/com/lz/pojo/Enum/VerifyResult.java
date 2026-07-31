package com.lz.pojo.Enum;

import lombok.Getter;

/**
 * 身份核验结果
 *
 * @author lz
 */
@Getter
public enum VerifyResult {
    /** 待人工审核 */
    PENDING_MANUAL_AUDIT("待人工审核"),
    /** 核验通过 */
    PASS("核验通过"),
    /** 核验驳回 */
    REJECT("核验驳回");

    private final String description;

    VerifyResult(String description) {
        this.description = description;
    }
}
