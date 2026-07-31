package com.lz.verifier;

import com.lz.pojo.Enum.VerifyResult;

/**
 * 身份核验接口（教务系统对接预留）
 *
 * @author lz
 */
public interface IIdentityVerifier {

    /**
     * 校验身份标识与姓名是否匹配
     *
     * @param identityNo 学号/工号/其他校内编号
     * @param name       姓名
     * @param role       student/teacher/other
     *
     * @return 核验结果
     */
    VerifyResult verify(String identityNo, String name, String role);
}
