package com.lz.common.json;

/*
 * Created with IntelliJ IDEA.
 * @Author: lz
 * @Date: 2024/05/10/17:18
 * @Description:
 */

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 反序列化
 *
 * @author lz
 * @date 2024/05/10
 */
public  class DateDeserializer extends JsonDeserializer<Date> {

    /**
     * 解析优先级：datetime 在前、date-only 在最后，且必须整串匹配。
     * 避免旧实现先按 "yyyy-MM-dd" lenient 前缀解析，把 "2026-08-10T23:59:59Z" 吞成
     * "2026-08-10 00:00:00"（时间丢失 → EndTime 截断到本地 00:00，end=当天即过期）。
     */
    private static final String[] DATETIME_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
    };

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        String dateString = jsonParser.getText();
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        dateString = dateString.trim();

        // 1. 尝试解析为时间戳 (Long)
        try {
            long timestamp = Long.parseLong(dateString);
            return new Date(timestamp);
        } catch (NumberFormatException e) {
            // 忽略，继续尝试其他格式
        }

        // 2. 按完整串匹配解析（datetime 优先于 date）
        for (String pattern : DATETIME_PATTERNS) {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            ParsePosition pos = new ParsePosition(0);
            Date parsed = sdf.parse(dateString, pos);
            if (parsed != null && pos.getIndex() == dateString.length()) {
                return parsed;
            }
        }
        throw new IOException("Failed to deserialize Date: " + dateString);
    }
}
