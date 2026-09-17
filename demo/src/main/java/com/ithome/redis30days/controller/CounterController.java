package com.ithome.redis30days.controller;

import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 4：同樣被打 N 次，Java 的 ++ 會少加，Redis 的 INCR 不會。
 *
 *   curl http://localhost:8080/counter/reset
 *   ./k6/run.sh counter
 *   curl http://localhost:8080/counter/result
 */
@RestController
@RequestMapping("/counter")
public class CounterController {

    private static final String KEY = "counter:redis";

    private final StringRedisTemplate redis;

    /** 故意不加 volatile、不用 AtomicInteger */
    private int javaCount = 0;

    public CounterController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** ++ 其實是「讀 → 加 → 寫回」三步，兩條執行緒會互相蓋掉 */
    @GetMapping("/java")
    public int java() {
        return ++javaCount;
    }

    /** INCR 是一個指令，Redis 單執行緒跑完才輪下一個 */
    @GetMapping("/redis")
    public Long redis() {
        return redis.opsForValue().increment(KEY);
    }

    @GetMapping("/result")
    public Map<String, Object> result() {
        String value = redis.opsForValue().get(KEY);
        return Map.of("java", javaCount, "redis", value == null ? 0 : Long.parseLong(value));
    }

    @GetMapping("/reset")
    public Map<String, Object> reset() {
        javaCount = 0;
        redis.delete(KEY);
        return Map.of("ok", true);
    }
}
