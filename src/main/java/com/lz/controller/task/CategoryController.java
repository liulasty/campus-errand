package com.lz.controller.task;

import com.lz.Exception.MyException;
import com.lz.pojo.result.NameAndDescription;
import com.lz.pojo.result.Result;
import com.lz.service.IDelegationCategoriesService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 委托分类接口（用户向选项）
 */
@RestController
@RequestMapping("/categories")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
@Slf4j
@Api(tags = "委托分类接口")
public class CategoryController {

    @Autowired
    private IDelegationCategoriesService delegationCategoriesService;

    @GetMapping("/options")
    public Result<List<NameAndDescription>> getTaskCategory() throws MyException {
        List<NameAndDescription> map = delegationCategoriesService.getTaskCategory();
        log.info("list:{}", map);
        return Result.success(map);
    }
}
