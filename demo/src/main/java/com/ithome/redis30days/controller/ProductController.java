package com.ithome.redis30days.controller;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.ithome.redis30days.model.Product;

/**
 * Day 4：商品快取。先查 Redis，沒有才去 DB 撈，撈完順手寫回快取。
 * Day 21：DB 也沒有的商品（快取穿透），用空值快取跟布隆過濾器擋。
 * Day 22：熱門商品快取過期的那一刻（快取擊穿）用互斥鎖擋，TTL 加隨機秒數避免一起過期（快取雪崩）。
 *
 *   curl http://localhost:8080/product/1001
 *   curl http://localhost:8080/product/99999
 *   curl http://localhost:8080/product/bloom/99999
 *   curl http://localhost:8080/product/lock/1
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration NULL_TTL = Duration.ofMinutes(5); // 空值的 TTL 短一點
    private static final Duration LOCK_TTL = Duration.ofSeconds(10); // 搶到鎖的人掛掉，最多卡 10 秒

    /** 預計放一萬個 id，誤判率 1% */
    private final BloomFilter<Long> bloomFilter = BloomFilter.create(Funnels.longFunnel(), 10_000, 0.01);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public ProductController(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
        for (long id = 1; id <= 10_000; id++) {
            bloomFilter.put(id); // 模擬啟動時從 DB 撈出所有商品 id 放進去
        }
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) throws JsonProcessingException {
        long start = System.currentTimeMillis();
        String key = "product:" + id;

        String json = redis.opsForValue().get(key);
        if (json != null) {
            return result(mapper.readValue(json, Product.class), "HIT", start);
        }

        Product product = loadFromDb(id); // sleep 100 毫秒，模擬查詢成本
        Duration ttl = product == null ? NULL_TTL : randomTtl(); // DB 也沒有，一樣寫進去，TTL 短一點
        redis.opsForValue().set(key, mapper.writeValueAsString(product), ttl); // null 會存成字串 "null"
        return result(product, "MISS", start);
    }

    /** 先問布隆過濾器，它說沒有就一定沒有，Redis 跟 DB 都不用查 */
    @GetMapping("/bloom/{id}")
    public Map<String, Object> bloom(@PathVariable Long id) throws JsonProcessingException {
        if (!bloomFilter.mightContain(id)) {
            return result(null, "BLOOM", System.currentTimeMillis());
        }
        return get(id); // 可能有，照原本的流程走
    }

    /** 快取沒有的時候，只讓搶到鎖的人去查 DB，其他人等它寫回快取 */
    @GetMapping("/lock/{id}")
    public Map<String, Object> lock(@PathVariable Long id) throws JsonProcessingException, InterruptedException {
        long start = System.currentTimeMillis();
        String key = "product:" + id;
        String lockKey = "lock:" + key;

        while (true) {
            String json = redis.opsForValue().get(key);
            if (json != null) {
                return result(mapper.readValue(json, Product.class), "HIT", start);
            }
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL))) { // SET NX EX
                try {
                    return get(id); // 搶到鎖，照原本的流程去查 DB
                } finally {
                    redis.delete(lockKey); // 查完放掉鎖
                }
            }
            Thread.sleep(50); // 沒搶到，等 50 毫秒再看一次快取
        }
    }

    /** 10 分鐘再多 0～299 秒，同一批寫進去的 key 才不會同一秒一起過期 */
    private Duration randomTtl() {
        return TTL.plusSeconds(ThreadLocalRandom.current().nextInt(300));
    }

    private Product loadFromDb(Long id) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (id < 1 || id > 10_000) {
            return null; // 只有 1 到 10000 號商品
        }
        return new Product(id, "商品 " + id, new BigDecimal("1299.00"), LocalDateTime.now());
    }

    private Map<String, Object> result(Product product, String source, long start) {
        Map<String, Object> map = new LinkedHashMap<>(); // Map.of 不能放 null
        map.put("source", source);
        map.put("tookMs", System.currentTimeMillis() - start);
        map.put("product", product);
        return map;
    }
}
