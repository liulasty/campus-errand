package com.lz.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: 配置类
 * @author lz
 */
@Configuration
public class AppConfig {
    @Value("${jwt.KEY}")
    private String jwtKey;

    @Bean
    public String getJwtKey() {

        return jwtKey;
    }
}