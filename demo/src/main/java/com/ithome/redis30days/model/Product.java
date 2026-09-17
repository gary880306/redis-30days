package com.ithome.redis30days.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Day 4：示範「把物件存進 Redis」的商品。
 *
 * createdAt 用 LocalDateTime。自己 new 一個 ObjectMapper 是序列化不了它的，
 * 但 Spring Boot 準備好的那個 ObjectMapper 已經裝好 JavaTimeModule，注入來直接用就行。
 */
public record Product(Long id, String name, BigDecimal price, LocalDateTime createdAt) {}
