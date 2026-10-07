package com.ithome.redis30days.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Day 24：Redisson 只拿來用它的鎖，其他還是用 Spring Data Redis。
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redisson() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379"); // 跟 application.yml 連同一台
        config.setLazyInitialization(true); // 用到才連線，之前的 Sentinel、Cluster 沒開這台也起得來
        return Redisson.create(config);
    }
}
