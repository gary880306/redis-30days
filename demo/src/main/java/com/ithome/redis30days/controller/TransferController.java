package com.ithome.redis30days.controller;

import java.util.List;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 19：MULTI / EXEC 轉帳，A 扣錢、B 加錢。中間不會被插隊，但出錯也不會回滾。
 *
 *   curl -X POST "http://localhost:8080/transfer?amount=30"
 *   curl -X POST "http://localhost:8080/transfer/wrong?amount=30"
 */
@RestController
@RequestMapping("/transfer")
public class TransferController {

    private final StringRedisTemplate redis;

    public TransferController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 包在 SessionCallback 裡，MULTI 到 EXEC 才會用同一條連線 */
    @PostMapping
    public List<Object> transfer(@RequestParam long amount) {
        return redis.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<Object> execute(RedisOperations ops) {
                ops.multi();
                ops.opsForValue().decrement("balance:A", amount); // 排隊，還沒執行
                ops.opsForValue().increment("balance:B", amount);
                return ops.exec(); // 一口氣執行，回每一筆的結果
            }
        });
    }

    /** 沒包 SessionCallback：每一行各拿各的連線 */
    @PostMapping("/wrong")
    public List<Object> wrong(@RequestParam long amount) {
        redis.multi();
        redis.opsForValue().decrement("balance:A", amount);
        redis.opsForValue().increment("balance:B", amount);
        return redis.exec();
    }
}
