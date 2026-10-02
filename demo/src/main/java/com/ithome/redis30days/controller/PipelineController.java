package com.ithome.redis30days.controller;

import java.util.Map;

import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 19：寫 n 筆，一筆一筆送 vs Pipeline 打包送。
 *
 *   curl -X POST "http://localhost:8080/pipeline/one?n=10000"
 *   curl -X POST "http://localhost:8080/pipeline/batch?n=10000"
 */
@RestController
@RequestMapping("/pipeline")
public class PipelineController {

    private final StringRedisTemplate redis;

    public PipelineController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 一筆一筆送：送出去、等回覆，才送下一筆 */
    @PostMapping("/one")
    public Map<String, Object> one(@RequestParam int n) {
        long start = System.currentTimeMillis();
        for (int i = 1; i <= n; i++) {
            redis.opsForValue().set("pipe:" + i, "1");
        }
        return Map.of("n", n, "ms", System.currentTimeMillis() - start);
    }

    /** Pipeline：n 筆先全部送出去，最後再一次收回覆 */
    @PostMapping("/batch")
    public Map<String, Object> batch(@RequestParam int n) {
        long start = System.currentTimeMillis();
        redis.executePipelined((RedisCallback<Object>) con -> {
            StringRedisConnection str = (StringRedisConnection) con;
            for (int i = 1; i <= n; i++) {
                str.set("pipe:" + i, "1");
            }
            return null; // 一定要回 null，回覆 Spring 會自己收
        });
        return Map.of("n", n, "ms", System.currentTimeMillis() - start);
    }
}
