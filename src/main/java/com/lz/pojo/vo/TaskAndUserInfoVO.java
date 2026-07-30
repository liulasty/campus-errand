package com.lz.pojo.vo;

/*
 * Created with IntelliJ IDEA.
 * @Author: lz
 * @Date: 2024/04/29/19:03
 * @Description:
 */

import com.lz.pojo.entity.Task;
import com.lz.pojo.entity.TaskAcceptRecords;
import com.lz.pojo.entity.UsersInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * @author lz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskAndUserInfoVO {
    UsersInfo usersInfo;
    Task task;
    List<TaskAcceptRecordVO> taskAcceptRecords;
    /**
     * 委托任务发布总数
     */
    Long taskPublishedTotal;
    /**
     * 委托任务完成总数
     */
    Long taskAcceptedTotal;
    /**
     *委托任务过期数
     */
    Long taskOverdueTotal;
    /**
     * 委托任务取消总数
     */
    Long taskCanceledTotal;

    public TaskAndUserInfoVO(Task task, UsersInfo usersInfo, List<TaskAcceptRecordVO> taskAcceptRecords) {
        this.usersInfo = usersInfo;
        this.task = task;
        this.taskAcceptRecords = taskAcceptRecords;
    }

    public TaskAndUserInfoVO(Task task, UsersInfo usersInfo) {
        this.usersInfo = usersInfo;
        this.task = task;
        this.taskAcceptRecords = new ArrayList<>();
    }
}