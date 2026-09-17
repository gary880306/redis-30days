package com.ithome.redis30days.controller;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ithome.redis30days.model.Product;

/** Day 4：商品快取。先查 Redis，沒有才去 DB 撈，撈完順手寫回快取。 */
@RestController
@RequestMapping("/product")
public class ProductController {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public ProductController(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
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
        redis.opsForValue().set(key, mapper.writeValueAsString(product), TTL); // 每個快取 key 都要給 TTL
        return result(product, "MISS", start);
    }

    private Product loadFromDb(Long id) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new Product(id, "商品 " + id, new BigDecimal("1299.00"), LocalDateTime.now());
    }

    private Map<String, Object> result(Product product, String source, long start) {
        return Map.of("source", source, "tookMs", System.currentTimeMillis() - start, "product", product);
    }
}
