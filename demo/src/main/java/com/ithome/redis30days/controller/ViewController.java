package com.ithome.redis30days.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 6：最近瀏覽的 10 件商品。
 *
 * LREM 去重 → LPUSH 推到最前面 → LTRIM 只留 10 筆。
 *
 *   curl -X POST http://localhost:8080/view/1/p10
 *   curl http://localhost:8080/view/1
 */
@RestController
@RequestMapping("/view")
public class ViewController {

    /** 只留最近 10 件 */
    private static final int KEEP = 10;

    private final ListOperations<String, String> list;

    public ViewController(StringRedisTemplate redis) {
        this.list = redis.opsForList();
    }

    /** 看了一件商品：看過就先拿掉，再推到最前面，最後砍掉第 11 筆以後 */
    @PostMapping("/{userId}/{productId}")
    public List<String> view(@PathVariable Long userId, @PathVariable String productId) {
        String key = key(userId);
        list.remove(key, 0, productId); // count 0 = 整條列表裡的都刪掉
        list.leftPush(key, productId);
        list.trim(key, 0, KEEP - 1); // 只留 index 0~9
        return list.range(key, 0, -1);
    }

    @GetMapping("/{userId}")
    public Map<String, Object> recent(@PathVariable Long userId) {
        String key = key(userId);
        return Map.of("items", list.range(key, 0, -1), "size", list.size(key));
    }

    private String key(Long userId) {
        return "view:" + userId;
    }
}
