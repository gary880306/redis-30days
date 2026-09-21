package com.ithome.redis30days.controller;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 8：延遲關單。score 放「該關單的時間」，時間到了才撈得出來。
 *
 *   curl -X POST "http://localhost:8080/delay/place?orderId=order:1&seconds=-60"
 *   curl http://localhost:8080/delay/due
 *   curl http://localhost:8080/delay
 */
@RestController
@RequestMapping("/delay")
public class DelayOrderController {

    private static final String KEY = "order:delay";

    private final ZSetOperations<String, String> zset;

    public DelayOrderController(StringRedisTemplate redis) {
        this.zset = redis.opsForZSet();
    }

    /** 下單：score 放「幾秒後該關單」的時間戳 */
    @PostMapping("/place")
    public Map<String, Object> place(@RequestParam String orderId, @RequestParam long seconds) {
        long closeAt = Instant.now().getEpochSecond() + seconds;
        zset.add(KEY, orderId, closeAt);
        return Map.of("orderId", orderId, "closeAt", closeAt);
    }

    /** 掃到期的：撈出 score 小於現在的，關完再 ZREM 拿掉 */
    @GetMapping("/due")
    public Map<String, Object> due() {
        long now = Instant.now().getEpochSecond();
        Set<String> due = zset.rangeByScore(KEY, 0, now);
        due.forEach(orderId -> zset.remove(KEY, orderId));
        return Map.of("closed", due, "left", zset.size(KEY));
    }

    /** 還沒到期的都還躺在這裡 */
    @GetMapping
    public Map<String, Object> all() {
        return Map.of("orders", zset.rangeWithScores(KEY, 0, -1), "size", zset.size(KEY));
    }
}
