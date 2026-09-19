package com.ithome.redis30days.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 6：拿 List 當 Queue。左邊推、右邊拿，就是先進先出（FIFO）。
 *
 *   curl -X POST "http://localhost:8080/queue/push?order=order:1"
 *   curl http://localhost:8080/queue/pop
 *   curl http://localhost:8080/queue/take
 */
@RestController
@RequestMapping("/queue")
public class QueueController {

    private static final String KEY = "queue:order";

    private final ListOperations<String, String> list;

    public QueueController(StringRedisTemplate redis) {
        this.list = redis.opsForList();
    }

    /** 生產者：LPUSH 從左邊推進去 */
    @PostMapping("/push")
    public Map<String, Object> push(@RequestParam String order) {
        return Map.of("length", list.leftPush(KEY, order));
    }

    /** 消費者：RPOP 從右邊拿，空的就回 null，要自己隔一陣子再來問一次 */
    @GetMapping("/pop")
    public Map<String, Object> pop() {
        String order = list.rightPop(KEY);
        return Map.of("order", order == null ? "(空的)" : order);
    }

    /** 換成 BRPOP：空的就掛在這裡等，有訂單進來馬上回，最多等 10 秒 */
    @GetMapping("/take")
    public Map<String, Object> take() {
        long start = System.currentTimeMillis();
        String order = list.rightPop(KEY, Duration.ofSeconds(10)); // 空的話這行會卡住，最多 10 秒
        return Map.of("order", order == null ? "(等了 10 秒還是沒有)" : order, "waitedMs", System.currentTimeMillis() - start);
    }
}
