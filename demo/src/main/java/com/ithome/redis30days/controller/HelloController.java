package com.ithome.redis30days.controller;

import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 2：確認 Spring Boot 真的連得上 Redis。
 *
 *   curl "http://localhost:8080/hello/set?key=day02&value=hello-redis"
 *   {"ok":true,"key":"day02","value":"hello-redis"}
 *   curl "http://localhost:8080/hello/get?key=day02"
 *   {"key":"day02","value":"hello-redis"}
 */
@RestController
public class HelloController {

    private final StringRedisTemplate redis;

    public HelloController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @GetMapping("/hello/set")
    public Map<String, Object> set(@RequestParam String key, @RequestParam String value) {
        redis.opsForValue().set(key, value);
        return Map.of("ok", true, "key", key, "value", value);
    }

    @GetMapping("/hello/get")
    public Map<String, Object> get(@RequestParam String key) {
        String value = redis.opsForValue().get(key);
        return Map.of("key", key, "value", value == null ? "(nil)" : value);
    }
}
