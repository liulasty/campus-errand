package com.lz.verifier;

import com.lz.pojo.Enum.VerifyResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ManualIdentityVerifier 单测
 *
 * @author lz
 */
class ManualIdentityVerifierTest {

    private final ManualIdentityVerifier verifier = new ManualIdentityVerifier();

    @Test
    void verify_alwaysPendingManualAudit() {
        assertThat(verifier.verify("20240101", "张三", "student"))
                .isEqualTo(VerifyResult.PENDING_MANUAL_AUDIT);
        assertThat(verifier.verify("T1001", "李四", "teacher"))
                .isEqualTo(VerifyResult.PENDING_MANUAL_AUDIT);
    }
}
