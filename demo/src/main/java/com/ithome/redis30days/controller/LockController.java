package com.ithome.redis30days.controller;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 24：分散式鎖。value 放自己的 UUID，用 Lua 確認是自己的才刪；Redisson 的看門狗幫鎖續期。
 *
 *   curl -X POST "http://localhost:8080/lock?seconds=3"
 *   curl -X POST "http://localhost:8080/lock/redisson?seconds=40"
 */
@RestController
@RequestMapping("/lock")
public class LockController {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    /** 比對跟刪除一次做完 */
    private static final RedisScript<Long> UNLOCK =
            RedisScript.of(new ClassPathResource("scripts/unlock.lua"), Long.class);

    private final StringRedisTemplate redis;
    private final RedissonClient redisson;

    public LockController(StringRedisTemplate redis, RedissonClient redisson) {
        this.redis = redis;
        this.redisson = redisson;
    }

    /** 搶到鎖做 seconds 秒，做完是自己的鎖才刪 */
    @PostMapping
    public Map<String, Object> lock(@RequestParam int seconds) throws InterruptedException {
        String uuid = UUID.randomUUID().toString(); // 每次搶鎖都不一樣，刪的時候才認得出是不是自己的
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("lock:job", uuid, LOCK_TTL))) {
            return Map.of("result", "沒搶到");
        }
        try {
            Thread.sleep(seconds * 1000L); // 模擬拿到鎖之後要做的事
        } finally {
            redis.execute(UNLOCK, List.of("lock:job"), uuid); // List 裡的是 KEYS，後面接的是 ARGV
        }
        return Map.of("result", "做完了");
    }

    /** 不給過期時間，Redisson 就會用看門狗：鎖 30 秒，還在做就每 10 秒續回 30 秒 */
    @PostMapping("/redisson")
    public Map<String, Object> redisson(@RequestParam int seconds) throws InterruptedException {
        RLock lock = redisson.getLock("lock:redisson");
        if (!lock.tryLock()) {
            return Map.of("result", "沒搶到");
        }
        try {
            Thread.sleep(seconds * 1000L); // 做超過 30 秒，鎖也不會過期
        } finally {
            lock.unlock(); // 一樣是自己的才刪
        }
        return Map.of("result", "做完了");
    }
}
