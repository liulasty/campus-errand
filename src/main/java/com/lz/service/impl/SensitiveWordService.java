package com.lz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.lz.mapper.SensitiveWordMapper;
import com.lz.pojo.entity.SensitiveWord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 敏感词服务：从数据库加载词库，构建 DFA 校验器，发布委托时调用 {@link #check(String)} 拦截。
 *
 * @author lz
 */
@Service
@Slf4j
public class SensitiveWordService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    private volatile SensitiveWordBs sensitiveWordBs;

    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 从数据库加载全部敏感词
     */
    public List<String> loadWords() {
        List<SensitiveWord> all = sensitiveWordMapper.selectList(
                new QueryWrapper<SensitiveWord>().orderByAsc("id"));
        return all.stream().map(SensitiveWord::getWord)
                .filter(w -> w != null && !w.trim().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 重新构建校验器（新增/删除敏感词后调用）
     */
    public synchronized void refresh() {
        List<String> words = loadWords();
        List<String> deny = new ArrayList<>(words);
        sensitiveWordBs = SensitiveWordBs.newInstance()
                .wordDeny(new IWordDeny() {
                    @Override
                    public List<String> deny() {
                        return deny;
                    }
                })
                .init();
        log.info("敏感词库已加载，共 {} 词", deny.size());
    }

    /**
     * 校验文本，返回命中的敏感词列表（空表示无敏感词）
     */
    public List<String> check(String text) {
        if (text == null || text.trim().isEmpty() || sensitiveWordBs == null) {
            return new ArrayList<>();
        }
        return sensitiveWordBs.findAll(text);
    }
}
