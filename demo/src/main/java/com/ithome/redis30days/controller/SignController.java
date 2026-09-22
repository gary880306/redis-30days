package com.ithome.redis30days.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 9：每日簽到。一個使用者一個 bit，SETBIT 點亮、BITCOUNT 數人頭。
 *
 *   curl -X POST http://localhost:8080/sign/1
 *   curl http://localhost:8080/sign/today
 *   curl http://localhost:8080/sign/1
 */
@RestController
@RequestMapping("/sign")
public class SignController {

    private static final String DAY = "day09";

    private static final String KEY = "sign:" + DAY;

    private final StringRedisTemplate redis;

    public SignController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 簽到：userId 就是第幾個 bit，setBit 回傳的是「原本」的值 */
    @PostMapping("/{userId}")
    public Map<String, Object> sign(@PathVariable long userId) {
        Boolean before = redis.opsForValue().setBit(KEY, userId, true);
        return Map.of("userId", userId, "firstToday", !Boolean.TRUE.equals(before), "total", count());
    }

    /** 今天幾個人簽到：BITCOUNT 數有幾個 1，不用把名單撈出來 */
    @GetMapping("/today")
    public Map<String, Object> today() {
        return Map.of("date", DAY, "total", count());
    }

    /** 某個人今天簽了沒：GETBIT 只看那一個 bit */
    @GetMapping("/{userId}")
    public Map<String, Object> check(@PathVariable long userId) {
        Boolean signed = redis.opsForValue().getBit(KEY, userId);
        return Map.of("userId", userId, "signed", Boolean.TRUE.equals(signed));
    }

    /** ValueOperations 只包了 setBit / getBit，BITCOUNT 要自己拿連線下 */
    private Long count() {
        byte[] key = KEY.getBytes(StandardCharsets.UTF_8);
        return redis.execute((RedisCallback<Long>) con -> con.stringCommands().bitCount(key));
    }
}
