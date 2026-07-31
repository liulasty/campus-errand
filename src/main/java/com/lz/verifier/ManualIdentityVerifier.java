package com.lz.verifier;

import com.lz.pojo.Enum.VerifyResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 手动审核实现：恒返回「待人工审核」，由管理员后台核对
 *
 * @author lz
 */
@Component
@ConditionalOnProperty(name = "app.identity-verifier.mode", havingValue = "manual", matchIfMissing = true)
public class ManualIdentityVerifier implements IIdentityVerifier {

    @Override
    public VerifyResult verify(String identityNo, String name, String role) {
        return VerifyResult.PENDING_MANUAL_AUDIT;
    }
}
