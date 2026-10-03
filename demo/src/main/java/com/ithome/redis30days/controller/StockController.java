package com.ithome.redis30days.controller;

import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 20：Lua 扣庫存，「看有沒有庫存、有才扣」在 Redis 裡一次做完。
 *
 *   curl -X POST "http://localhost:8080/stock/buy?id=1"
 */
@RestController
@RequestMapping("/stock")
public class StockController {

    /** 腳本放在 resources/scripts/，Lua 回的整數對到 Long */
    private static final RedisScript<Long> DEDUCT =
            RedisScript.of(new ClassPathResource("scripts/deduct.lua"), Long.class);

    private final StringRedisTemplate redis;

    public StockController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 買一個：回扣完剩幾個，沒庫存回 -1 */
    @PostMapping("/buy")
    public Long buy(@RequestParam long id) {
        return redis.execute(DEDUCT, List.of("stock:" + id)); // List 裡的就是 KEYS
    }
}
