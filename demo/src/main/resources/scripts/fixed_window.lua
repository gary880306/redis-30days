-- Day 26：固定視窗限流，第一個進來的開始算，1 秒內最多放 ARGV[1] 個
-- KEYS[1]：計數的 key，例如 limit:fixed:1
-- ARGV[1]：上限，例如 100

local count = redis.call('INCR', KEYS[1]) -- 進來一個加一，key 不存在會從 0 開始加

if count == 1 then
    redis.call('EXPIRE', KEYS[1], 1) -- 第一個進來的設 1 秒過期，過期了就從頭算
end

if count > tonumber(ARGV[1]) then
    return 0 -- 超過上限，擋掉
end

return 1 -- 放進來
