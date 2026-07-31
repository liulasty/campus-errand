package com.lz.pojo.vo;

/*
 * Created with IntelliJ IDEA.
 * @Author: lz
 * @Date: 2024/04/14/11:37
 * @Description:
 */

import com.lz.pojo.Enum.AuthenticationStatus;
import lombok.Data;

import java.util.Date;

/**
 * 用户页面 vo
 *
 * @author lz
 * @date 2024/04/14
 */
@Data
public class UserPageVO {
    private Long userId;
    private String username;
    private String email;
    private String role;
    private Boolean isActive;
    private Date createTime;
    private Date activeTime;
    private AuthenticationStatus authStatus;
    private Boolean IsEnabled;
    /** 认证等级（0=未认证,1=L1实名,2=L2校园卡） */
    private Integer authLevel;
    /** 学号/工号/其他校内编号 */
    private String identityNo;
    /** 信用分（0-100） */
    private Integer creditScore;
    /** 姓名 */
    private String name;
    /** 用户角色（student/teacher/other） */
    private String userRole;
    /** 认证照片 */
    private String roleImgSrc;
    /** 认证申请时间 */
    private Date certifieTime;

}