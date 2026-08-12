package com.lz.controller.system;

import com.lz.pojo.result.Result;
import com.lz.service.impl.SensitiveWordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 敏感词接口（用户侧：发布文本校验）
 */
@RestController
@RequestMapping("/sensitive-words")
@Slf4j
@Api(tags = "敏感词校验接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class SensitiveWordController {

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @PostMapping("/check")
    @ApiOperation("校验文本中的敏感词")
    public Result<?> check(@RequestBody Map<String, String> body) {
        List<String> hit = sensitiveWordService.check(body.get("text"));
        return Result.success(hit);
    }
}
