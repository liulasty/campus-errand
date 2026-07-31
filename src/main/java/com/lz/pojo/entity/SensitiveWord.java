package com.lz.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 敏感词配置
 *
 * @author lz
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("sensitive_words")
@ApiModel(value = "敏感词配置", description = "发布委托时拦截的敏感词")
public class SensitiveWord implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "敏感词ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "敏感词")
    @TableField("word")
    private String word;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private Date createTime;
}
