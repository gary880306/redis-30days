// Day 25：跟 oversell.js 一樣，改打用鎖包起來的 /stock/buy/lock
// 執行：./k6/run.sh lock
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 50,          // 50 個虛擬使用者同時打
  iterations: 5000, // 總共打 5000 次
};

export default function () {
  const res = http.post('http://localhost:8080/stock/buy/lock?id=1');
  check(res, { '搶到': (r) => r.json() >= 0 }); // 沒庫存回 -1
}
