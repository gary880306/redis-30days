package com.ithome.redis30days.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 8：熱銷排行。ZINCRBY 加分 → ZRANGE ... REV 取前 N 名。
 *
 *   curl -X POST "http://localhost:8080/rank/sold?productId=p1&qty=3"
 *   curl "http://localhost:8080/rank/top?n=3"
 *   curl http://localhost:8080/rank/p1
 */
@RestController
@RequestMapping("/rank")
public class RankController {

    private static final String KEY = "rank:sales";

    private final ZSetOperations<String, String> zset;

    public RankController(StringRedisTemplate redis) {
        this.zset = redis.opsForZSet();
    }

    /** 賣出幾件就加幾分，不用先查再寫 */
    @PostMapping("/sold")
    public Map<String, Object> sold(@RequestParam String productId, @RequestParam double qty) {
        return Map.of("productId", productId, "sales", zset.incrementScore(KEY, productId, qty));
    }

    /** 前 N 名：排序是 Redis 自己維護的，直接取就好 */
    @GetMapping("/top")
    public List<Map<String, Object>> top(@RequestParam(defaultValue = "10") long n) {
        Set<TypedTuple<String>> rows = zset.reverseRangeWithScores(KEY, 0, n - 1); // 取第 0 ~ n-1 名
        List<Map<String, Object>> top = new ArrayList<>();
        for (TypedTuple<String> row : rows) {
            top.add(Map.of("rank", top.size() + 1, "productId", row.getValue(), "sales", row.getScore()));
        }
        return top;
    }

    /** 查一件商品賣幾件、排第幾 */
    @GetMapping("/{productId}")
    public Map<String, Object> one(@PathVariable String productId) {
        Double sales = zset.score(KEY, productId);
        if (sales == null) {
            return Map.of("productId", productId, "rank", "沒上榜");
        }
        Long rank = zset.reverseRank(KEY, productId); // 從 0 開始，所以要 +1
        return Map.of("productId", productId, "sales", sales, "rank", rank + 1);
    }
}
