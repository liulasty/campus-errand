package com.lz.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 首页数据驾驶舱统计
 *
 * @author lz
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsVO {
    /** 今日新增委托 */
    private Long todayNewTask;
    /** 今日接单 */
    private Long todayAccepted;
    /** 各状态数量：[{name: 中文状态, value}] */
    private List<Map<String, Object>> statusCounts;
    /** 委托类型分布：[{name: 类别名, value}] */
    private List<Map<String, Object>> categoryCounts;
    /** 接单排行：[{userId, name, value}] */
    private List<Map<String, Object>> acceptRanking;
}
