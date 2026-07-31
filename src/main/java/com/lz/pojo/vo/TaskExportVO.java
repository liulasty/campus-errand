package com.lz.pojo.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 委托列表导出
 *
 * @author lz
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExportVO {
    @ExcelProperty("任务ID")
    private Long taskId;
    @ExcelProperty("发布者ID")
    private Long ownerId;
    @ExcelProperty("描述")
    private String description;
    @ExcelProperty("地点")
    private String location;
    @ExcelProperty("金额")
    private BigDecimal money;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("发布时间")
    private Date startTime;
    @ExcelProperty("截止时间")
    private Date endTime;
    @ExcelProperty("创建时间")
    private Date createdAt;
}
