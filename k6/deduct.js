// Day 25：跟 oversell.js 一樣，改打 Day 20 用 Lua 扣庫存的 /stock/buy
// 執行：./k6/run.sh deduct
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 50,          // 50 個虛擬使用者同時打
  iterations: 5000, // 總共打 5000 次
};

export default function () {
  const res = http.post('http://localhost:8080/stock/buy?id=1');
  check(res, { '搶到': (r) => r.json() >= 0 }); // 沒庫存回 -1
}
