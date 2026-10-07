-- Day 24：鎖還是自己的才刪，比對跟刪除一次做完
-- KEYS[1]：鎖的 key，例如 lock:job
-- ARGV[1]：搶鎖時放進去的 UUID

if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1]) -- 是自己的，刪掉，回 1
end

return 0 -- 鎖已經不是自己的了，不動
