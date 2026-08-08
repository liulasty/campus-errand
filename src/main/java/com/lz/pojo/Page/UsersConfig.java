package com.lz.pojo.Page;

import lombok.Builder;
import lombok.Data;

/**
 * 用户配置
 *
 * @author lz
 * @date 2024/04/15
 */
@Data
@Builder
public class UsersConfig {
    private String username;
    private String email;
    private Boolean isActive;
    private int pageNum;
    private int pageSize;
    /** 认证状态中文值（认证中/认证通过/认证失败/未认证），null=不过滤 */
    private String authStatus;
    /** 认证状态 dbValue（由 authStatus 在 Service 映射），null=不过滤 */
    private Integer authStatusDb;
    /** 未认证（无 usersinfo 记录）过滤开关 */
    private boolean unauthenticatedOnly;
    /** 分页偏移量（Service 计算，供 JOIN 查询 LIMIT 使用） */
    private int offset;
}
