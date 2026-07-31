package com.lz.pojo.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 任务履约节点打卡 DTO
 *
 * @author lz
 */
@Data
@ApiModel(value = "任务履约节点打卡对象", description = "任务履约节点打卡时传递的数据模型")
public class TaskNodeDTO {

    @ApiModelProperty(value = "任务ID", required = true)
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @ApiModelProperty(value = "节点类型（CONTACTED/PICKED_UP/DELIVERED）", required = true)
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    @ApiModelProperty(value = "打卡图片")
    private String imgUrl;

    @ApiModelProperty(value = "打卡定位坐标")
    private String location;

    @ApiModelProperty(value = "简短备注")
    private String remark;
}
