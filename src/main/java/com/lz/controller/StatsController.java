package com.lz.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lz.mapper.DelegationCategoriesMapper;
import com.lz.mapper.TaskAcceptRecordsMapper;
import com.lz.mapper.TaskMapper;
import com.lz.mapper.UsersMapper;
import com.lz.pojo.Enum.TaskStatus;
import com.lz.pojo.entity.DelegationCategories;
import com.lz.pojo.entity.Users;
import com.lz.pojo.result.Result;
import com.lz.pojo.vo.DashboardStatsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据驾驶舱统计
 *
 * @author lz
 */
@RestController
@RequestMapping("/stats")
@Slf4j
@Api(tags = "数据统计")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class StatsController {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskAcceptRecordsMapper taskAcceptRecordsMapper;

    @Autowired
    private DelegationCategoriesMapper delegationCategoriesMapper;

    @Autowired
    private UsersMapper usersMapper;

    @GetMapping
    @ApiOperation("首页数据驾驶舱统计")
    public Result<?> stats() {
        Long todayNewTask = taskMapper.getTasksTodayCount(TaskStatus.ONGOING.getDbValue()).longValue();
        Long todayAccepted = taskAcceptRecordsMapper.countCheckedToday();

        List<Map<String, Object>> statusCounts = taskMapper.countGroupByStatus().stream()
                .map(m -> {
                    Map<String, Object> mm = new HashMap<>();
                    String db = String.valueOf(m.get("name"));
                    TaskStatus st = TaskStatus.fromDbValue(db);
                    mm.put("name", st != null ? st.getWebValue() : db);
                    mm.put("value", m.get("value"));
                    return mm;
                })
                .collect(Collectors.toList());

        Map<Long, String> catNames = delegationCategoriesMapper.selectList(null).stream()
                .collect(Collectors.toMap(DelegationCategories::getCategoryId,
                        DelegationCategories::getCategoryName, (a, b) -> a));
        List<Map<String, Object>> categoryCounts = taskMapper.countGroupByType().stream()
                .map(m -> {
                    Map<String, Object> mm = new HashMap<>();
                    Long typeId = Long.valueOf(String.valueOf(m.get("typeId")));
                    mm.put("name", catNames.getOrDefault(typeId, "未知"));
                    mm.put("value", m.get("value"));
                    return mm;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> acceptRanking = taskAcceptRecordsMapper.acceptRanking(5).stream()
                .map(m -> {
                    Map<String, Object> mm = new HashMap<>();
                    Long userId = Long.valueOf(String.valueOf(m.get("userId")));
                    Users u = usersMapper.selectById(userId);
                    mm.put("userId", userId);
                    mm.put("name", u != null ? u.getUsername() : "用户" + userId);
                    mm.put("value", m.get("value"));
                    return mm;
                })
                .collect(Collectors.toList());

        DashboardStatsVO vo = DashboardStatsVO.builder()
                .todayNewTask(todayNewTask)
                .todayAccepted(todayAccepted)
                .statusCounts(statusCounts)
                .categoryCounts(categoryCounts)
                .acceptRanking(acceptRanking)
                .build();
        return Result.success(vo);
    }
}
