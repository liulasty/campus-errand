package com.lz.pojo.dto;

/*
 * Created with IntelliJ IDEA.
 * @Author: lz
 * @Date: 2024/05/03/13:53
 * @Description:
 */

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author lz
 */
@Data
public class AcceptDTO {
    @NotNull(message = "任务ID不能为空")
    private Long task;
    @NotNull(message = "用户ID不能为空")
    private Long user;
    private String str;
}