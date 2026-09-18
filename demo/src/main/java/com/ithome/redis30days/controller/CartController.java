package com.ithome.redis30days.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Day 5：同一個購物車，用 String 存整包 JSON 和用 Hash 存，併發下的差別。
 *
 *   curl http://localhost:8080/cart/reset
 *   ./k6/run.sh cart
 *   curl http://localhost:8080/cart/result
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    private static final String JSON_KEY = "cart:json:1001";
    private static final String HASH_KEY = "cart:hash:1001";
    private static final String ITEM = "product:2001";

    private static final TypeReference<Map<String, Integer>> CART = new TypeReference<>() {};

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final HashOperations<String, String, String> hash;

    public CartController(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
        this.hash = redis.opsForHash();
    }

    // ---------- 併發對照 ----------

    /** String 版：讀整包 → 反序列化 → 改 → 序列化 → 寫回，中間任何一步都可能被插隊 */
    @GetMapping("/json/add")
    public Map<String, Integer> jsonAdd() throws JsonProcessingException {
        String json = redis.opsForValue().get(JSON_KEY);
        Map<String, Integer> cart = json == null ? new HashMap<>() : mapper.readValue(json, CART);

        cart.merge(ITEM, 1, Integer::sum);

        redis.opsForValue().set(JSON_KEY, mapper.writeValueAsString(cart));
        return cart;
    }

    /** Hash 版：一個指令做完，不用讀回來 */
    @GetMapping("/hash/add")
    public Long hashAdd() {
        return hash.increment(HASH_KEY, ITEM, 1);
    }

    @GetMapping("/result")
    public Map<String, Object> result() throws JsonProcessingException {
        String json = redis.opsForValue().get(JSON_KEY);
        Map<String, Integer> cart = json == null ? Map.of() : mapper.readValue(json, CART);
        return Map.of(
                "json", cart.getOrDefault(ITEM, 0),
                "hash", Integer.parseInt(hash.get(HASH_KEY, ITEM)));
    }

    @GetMapping("/reset")
    public Map<String, Object> reset() {
        redis.delete(JSON_KEY);
        redis.delete(HASH_KEY);
        hash.put(HASH_KEY, ITEM, "0");
        return Map.of("ok", true);
    }

    // ---------- 購物車該有的操作 ----------

    /** 加入商品：已經在車上就累加數量 */
    @PostMapping("/{userId}/items")
    public Long add(@PathVariable Long userId, @RequestParam String productId, @RequestParam long qty) {
        return hash.increment(key(userId), productId, qty);
    }

    /** 直接指定數量（HSET 是覆蓋，不是累加） */
    @PostMapping("/{userId}/items/{productId}")
    public Map<String, Object> update(
            @PathVariable Long userId, @PathVariable String productId, @RequestParam long qty) {
        hash.put(key(userId), productId, String.valueOf(qty));
        return Map.of("ok", true);
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public Map<String, Object> remove(@PathVariable Long userId, @PathVariable String productId) {
        return Map.of("removed", hash.delete(key(userId), productId));
    }

    /** 整台車撈出來，順便回傳件數 */
    @GetMapping("/{userId}")
    public Map<String, Object> all(@PathVariable Long userId) {
        return Map.of(
                "items", hash.entries(key(userId)),
                "size", hash.size(key(userId)));
    }

    private String key(Long userId) {
        return "cart:" + userId;
    }
}
