package com.lz.pojo.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户列表导出
 *
 * @author lz
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExportVO {
    @ExcelProperty("用户ID")
    private Long userId;
    @ExcelProperty("用户名")
    private String username;
    @ExcelProperty("邮箱")
    private String email;
    @ExcelProperty("角色")
    private String role;
    @ExcelProperty("是否激活")
    private Boolean isActive;
    @ExcelProperty("是否启用")
    private Boolean isEnabled;
    @ExcelProperty("注册时间")
    private Date createTime;
}
