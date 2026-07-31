package com.lz.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lz.Exception.MyException;
import com.lz.mapper.SensitiveWordMapper;
import com.lz.pojo.entity.SensitiveWord;
import com.lz.pojo.result.Result;
import com.lz.service.impl.SensitiveWordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 敏感词配置（发布委托拦截）
 *
 * @author lz
 */
@RestController
@RequestMapping("/sensitive")
@Slf4j
@Api(tags = "敏感词配置")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class SensitiveWordController {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @GetMapping("/words")
    @ApiOperation("敏感词列表")
    public Result<?> list() {
        List<SensitiveWord> list = sensitiveWordMapper.selectList(
                new QueryWrapper<SensitiveWord>().orderByAsc("id"));
        return Result.success(list);
    }

    @PostMapping("/words")
    @ApiOperation("新增敏感词")
    public Result<?> add(@RequestBody Map<String, String> body) throws MyException {
        String word = body.get("word");
        if (word == null || word.trim().isEmpty()) {
            throw new MyException("敏感词不能为空");
        }
        word = word.trim();
        Integer exist = sensitiveWordMapper.selectCount(
                new QueryWrapper<SensitiveWord>().eq("word", word));
        if (exist != null && exist > 0) {
            throw new MyException("该敏感词已存在");
        }
        SensitiveWord sw = SensitiveWord.builder()
                .word(word)
                .createTime(new Date())
                .build();
        sensitiveWordMapper.insert(sw);
        sensitiveWordService.refresh();
        return Result.success("添加成功");
    }

    @DeleteMapping("/words/{id}")
    @ApiOperation("删除敏感词")
    public Result<?> delete(@PathVariable("id") Long id) {
        sensitiveWordMapper.deleteById(id);
        sensitiveWordService.refresh();
        return Result.success("删除成功");
    }

    @PostMapping("/check")
    @ApiOperation("校验文本中的敏感词")
    public Result<?> check(@RequestBody Map<String, String> body) {
        List<String> hit = sensitiveWordService.check(body.get("text"));
        return Result.success(hit);
    }
}
