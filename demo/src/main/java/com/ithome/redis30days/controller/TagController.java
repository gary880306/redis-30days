package com.ithome.redis30days.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 7：商品標籤。SADD 打標籤 → SINTER 取交集做多條件篩選。
 *
 *   curl -X POST "http://localhost:8080/tag/p2?tags=3C,sale"
 *   curl "http://localhost:8080/tag/search?tags=3C,sale"
 *   curl http://localhost:8080/tag/3C
 */
@RestController
@RequestMapping("/tag")
public class TagController {

    private final SetOperations<String, String> set;

    public TagController(StringRedisTemplate redis) {
        this.set = redis.opsForSet();
    }

    /** 打標籤：同一件商品重複打同一個標籤，集合裡還是只有一筆 */
    @PostMapping("/{productId}")
    public Map<String, Object> tag(@PathVariable String productId, @RequestParam List<String> tags) {
        tags.forEach(tag -> set.add(key(tag), productId));
        return Map.of("productId", productId, "tags", tags);
    }

    /** 同時有這些標籤的商品，一個 SINTER 做完 */
    @GetMapping("/search")
    public Set<String> search(@RequestParam List<String> tags) {
        return set.intersect(tags.stream().map(this::key).toList());
    }

    /** 看某個標籤底下掛了哪些商品 */
    @GetMapping("/{tag}")
    public Map<String, Object> members(@PathVariable String tag) {
        return Map.of("items", set.members(key(tag)), "size", set.size(key(tag)));
    }

    private String key(String tag) {
        return "tag:" + tag;
    }
}
