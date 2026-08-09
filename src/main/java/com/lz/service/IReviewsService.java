package com.lz.service;

import com.lz.Exception.MyException;
import com.lz.pojo.dto.ReviewsDTO;
import com.lz.pojo.entity.Reviews;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * <p>
 * 存储用户对任务的评价信息 服务类
 * </p>
 *
 * @author lz
 * @since 2024-04-04
 */
public interface IReviewsService extends IService<Reviews> {

    void save(ReviewsDTO reviewsDTO) throws MyException;

    List<Reviews> exportExcel();

    /** 清空全部评价（测试支撑，仅管理员调用），返回删除行数 */
    int clearAll();
}