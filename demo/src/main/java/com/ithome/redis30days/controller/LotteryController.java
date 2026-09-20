package com.ithome.redis30days.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 7：抽獎。SADD 報名自動去重 → SPOP 抽出即移除。
 *
 *   curl -X POST "http://localhost:8080/lottery/join?user=u1"
 *   curl -X POST "http://localhost:8080/lottery/draw?count=3"
 *   curl http://localhost:8080/lottery
 */
@RestController
@RequestMapping("/lottery")
public class LotteryController {

    private static final String KEY = "lottery:1";

    private final SetOperations<String, String> set;

    public LotteryController(StringRedisTemplate redis) {
        this.set = redis.opsForSet();
    }

    /** 報名：同一個人連按十次也只會有一筆 */
    @PostMapping("/join")
    public Map<String, Object> join(@RequestParam String user) {
        return Map.of("added", set.add(KEY, user), "total", set.size(KEY));
    }

    /** 抽獎：一次抽 count 個，抽出來的就從池子裡移除 */
    @PostMapping("/draw")
    public Map<String, Object> draw(@RequestParam long count) {
        List<String> winners = set.pop(KEY, count);
        return Map.of("winners", winners, "left", set.size(KEY));
    }

    @GetMapping
    public Map<String, Object> pool() {
        return Map.of("users", set.members(KEY), "total", set.size(KEY));
    }
}
