package com.lz.common.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端 JWT 登出黑名单（内存实现）。
 * <p>
 * JWT 无状态、签发后即不可撤回，登出若仅清除本地安全上下文则同 token 仍可用。
 * 登出时将该 token 加入黑名单，鉴权过滤器对黑名单 token 不再恢复登录态，
 * 从而让登出真正失效。token 有效期 12 小时（与 {@code JwtUtil.genToken} 一致），
 * 写入时惰性清理过期条目，避免无限增长。
 */
@Component
public class JwtTokenBlacklist {

    private static final long TOKEN_TTL_MS = 1000L * 60 * 60 * 12;

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void add(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
        blacklist.put(token, now + TOKEN_TTL_MS);
    }

    public boolean contains(String token) {
        return token != null && blacklist.containsKey(token);
    }
}
