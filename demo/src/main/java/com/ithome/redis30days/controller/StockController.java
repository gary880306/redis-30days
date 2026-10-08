package com.ithome.redis30days.controller;

import java.util.List;

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
 * Day 20：Lua 扣庫存，「看有沒有庫存、有才扣」在 Redis 裡一次做完。
 * Day 25：秒殺。先讀再寫的版本，很多人同時搶會超賣；用鎖包起來不會超賣，但比 Lua 慢。
 *
 *   curl -X POST "http://localhost:8080/stock/buy?id=1"
 *   curl -X POST "http://localhost:8080/stock/buy/wrong?id=1"
 *   curl -X POST "http://localhost:8080/stock/buy/lock?id=1"
 */
@RestController
@RequestMapping("/stock")
public class StockController {

    /** 腳本放在 resources/scripts/，Lua 回的整數對到 Long */
    private static final RedisScript<Long> DEDUCT =
            RedisScript.of(new ClassPathResource("scripts/deduct.lua"), Long.class);

    private final StringRedisTemplate redis;
    private final RedissonClient redisson;

    public StockController(StringRedisTemplate redis, RedissonClient redisson) {
        this.redis = redis;
        this.redisson = redisson;
    }

    /** 買一個：回扣完剩幾個，沒庫存回 -1 */
    @PostMapping("/buy")
    public Long buy(@RequestParam long id) {
        return redis.execute(DEDUCT, List.of("stock:" + id)); // List 裡的就是 KEYS
    }

    /** 錯誤示範：先 GET 庫存，有庫存再 SET 回去減一，回傳值跟 /buy 一樣 */
    @PostMapping("/buy/wrong")
    public Long buyWrong(@RequestParam long id) {
        String key = "stock:" + id;
        String value = redis.opsForValue().get(key);
        long stock = value == null ? 0 : Long.parseLong(value); // key 不存在當 0

        if (stock <= 0) {
            return -1L; // 沒庫存，不扣
        }

        redis.opsForValue().set(key, String.valueOf(stock - 1)); // GET 跟 SET 中間，別人也可能讀到同一個數字
        return stock - 1;
    }

    /** 用 Day 24 的鎖把先讀再寫包起來，同一時間只有一個人在扣 */
    @PostMapping("/buy/lock")
    public Long buyLock(@RequestParam long id) {
        RLock lock = redisson.getLock("lock:stock:" + id);
        lock.lock(); // 跟 tryLock() 不一樣，沒搶到會一直等到搶到
        try {
            return buyWrong(id); // 裡面跟剛剛的先讀再寫一樣，只是外面多了鎖
        } finally {
            lock.unlock();
        }
    }
}
