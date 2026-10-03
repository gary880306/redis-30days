-- Day 20：庫存大於 0 才扣一個，整段跑完之前不會被插隊
-- KEYS[1]：庫存的 key，例如 stock:1

local stock = tonumber(redis.call('GET', KEYS[1])) or 0 -- key 不存在當 0

if stock <= 0 then
    return -1 -- 沒庫存，不扣
end

return redis.call('DECR', KEYS[1]) -- 扣一個，回剩幾個
