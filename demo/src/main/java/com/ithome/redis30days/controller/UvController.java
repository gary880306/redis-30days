package com.ithome.redis30days.controller;

import java.util.Map;

import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 9：UV 統計。PFADD 記人、PFCOUNT 估不重複人數，塞再多人 key 都是固定大小。
 *
 *   curl -X POST "http://localhost:8080/uv/visit?user=u1"
 *   curl http://localhost:8080/uv
 */
@RestController
@RequestMapping("/uv")
public class UvController {

    private static final String DAY = "day09";

    private static final String KEY = "uv:" + DAY;

    private final HyperLogLogOperations<String, String> hll;

    public UvController(StringRedisTemplate redis) {
        this.hll = redis.opsForHyperLogLog();
    }

    /** 進來看一次就記一次，同一個人記幾次都只算一個 */
    @PostMapping("/visit")
    public Map<String, Object> visit(@RequestParam String user) {
        return Map.of("added", hll.add(KEY, user), "uv", hll.size(KEY));
    }

    /** 今天多少不重複訪客：這個數字是估的，不是精確值 */
    @GetMapping
    public Map<String, Object> uv() {
        return Map.of("date", DAY, "uv", hll.size(KEY));
    }
}
