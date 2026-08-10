package com.lz.utils;

/**
 * 学号/工号脱敏工具（D-16 凭证脱敏统一规则）
 *
 * 规则：保留前4位+末2位，中间按实际长度补 '*'；过短（≤6）则前2位+****。
 * 示例：20260002 → 2026**02；123456 → 12****
 *
 * @author lz
 */
public final class IdentityMaskUtils {

    private IdentityMaskUtils() {
    }

    /**
     * 掩码身份标识（学号/工号/身份证号等）。null 或空原样返回。
     */
    public static String mask(String no) {
        if (no == null || no.isEmpty()) {
            return no;
        }
        if (no.length() <= 6) {
            return no.substring(0, Math.min(2, no.length())) + "****";
        }
        StringBuilder sb = new StringBuilder(no.substring(0, 4));
        for (int i = 0; i < no.length() - 6; i++) {
            sb.append('*');
        }
        return sb.append(no.substring(no.length() - 2)).toString();
    }
}
