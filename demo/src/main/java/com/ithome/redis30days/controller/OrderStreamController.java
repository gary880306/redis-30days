package com.ithome.redis30days.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Day 10：訂單事件流。XADD 生產、XREADGROUP 消費、XACK 回報，沒 ACK 的用 XCLAIM 撿回來。
 *
 *   curl -X POST "http://localhost:8080/stream/order?orderId=1&amount=100"
 *   curl -X POST "http://localhost:8080/stream/consume?consumer=c1&count=2"
 *   curl -X POST "http://localhost:8080/stream/ack?id=時間戳記-序號"
 *   curl http://localhost:8080/stream/pending
 *   curl -X POST "http://localhost:8080/stream/claim?consumer=c1&idleSeconds=10"
 */
@RestController
@RequestMapping("/stream")
public class OrderStreamController {

    private static final String KEY = "order:stream";

    private static final String GROUP = "g1";

    private final StreamOperations<String, String, String> stream;

    public OrderStreamController(StringRedisTemplate redis) {
        this.stream = redis.opsForStream();
    }

    /** 生產：XADD，ID 交給 Redis 產，回傳的就是那筆的 ID */
    @PostMapping("/order")
    public Map<String, Object> order(@RequestParam String orderId, @RequestParam String amount) {
        RecordId id = stream.add(KEY, Map.of("orderId", orderId, "amount", amount));
        return Map.of("id", id.getValue(), "length", stream.size(KEY));
    }

    /** 消費：XREADGROUP 拿還沒發過的，同一筆不會發給兩個人。拿到只是借走，還沒算處理完 */
    @PostMapping("/consume")
    public Map<String, Object> consume(@RequestParam String consumer, @RequestParam(defaultValue = "1") long count) {
        ensureGroup();
        List<MapRecord<String, String, String>> records = stream.read(
                Consumer.from(GROUP, consumer),
                StreamReadOptions.empty().count(count),
                StreamOffset.create(KEY, ReadOffset.lastConsumed())); // lastConsumed() 就是 cli 的 '>'
        List<Map<String, Object>> got = new ArrayList<>();
        for (MapRecord<String, String, String> r : records) {
            got.add(Map.of("id", r.getId().getValue(), "value", r.getValue()));
        }
        return Map.of("consumer", consumer, "got", got);
    }

    /** 處理完了：XACK 之後這筆才從 pending 清單消失 */
    @PostMapping("/ack")
    public Map<String, Object> ack(@RequestParam String id) {
        return Map.of("acked", stream.acknowledge(KEY, GROUP, id));
    }

    /** XPENDING：誰借走了還沒還，借多久了、被發過幾次 */
    @GetMapping("/pending")
    public Map<String, Object> pending() {
        ensureGroup();
        PendingMessages messages = stream.pending(KEY, GROUP, Range.unbounded(), 100);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PendingMessage m : messages) {
            rows.add(Map.of(
                    "id", m.getIdAsString(),
                    "consumer", m.getConsumerName(),
                    "idleMs", m.getElapsedTimeSinceLastDelivery().toMillis(),
                    "deliveredTimes", m.getTotalDeliveryCount()));
        }
        return Map.of("pending", rows.size(), "rows", rows);
    }

    /** 撿回來：借超過 idleSeconds 還沒還的，XCLAIM 搶過來重做 */
    @PostMapping("/claim")
    public Map<String, Object> claim(
            @RequestParam String consumer, @RequestParam(defaultValue = "10") long idleSeconds) {
        ensureGroup();
        Duration idle = Duration.ofSeconds(idleSeconds);
        PendingMessages messages = stream.pending(KEY, GROUP, Range.unbounded(), 100);
        List<RecordId> timeout = new ArrayList<>();
        for (PendingMessage m : messages) {
            if (m.getElapsedTimeSinceLastDelivery().compareTo(idle) >= 0) {
                timeout.add(m.getId());
            }
        }
        if (timeout.isEmpty()) {
            return Map.of("consumer", consumer, "claimed", List.of());
        }
        List<MapRecord<String, String, String>> records =
                stream.claim(KEY, GROUP, consumer, idle, timeout.toArray(new RecordId[0]));
        List<Map<String, Object>> claimed = new ArrayList<>();
        for (MapRecord<String, String, String> r : records) {
            claimed.add(Map.of("id", r.getId().getValue(), "value", r.getValue()));
        }
        return Map.of("consumer", consumer, "claimed", claimed);
    }

    /** 群組不存在才建（順便把 stream 建出來），已經存在會噴 BUSYGROUP，吞掉就好 */
    private void ensureGroup() {
        try {
            stream.createGroup(KEY, ReadOffset.from("0"), GROUP);
        } catch (DataAccessException alreadyExists) {
            // BUSYGROUP Consumer Group name already exists
        }
    }
}
