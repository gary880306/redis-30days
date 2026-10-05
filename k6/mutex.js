// Day 22：跟 breakdown.js 一樣，改打有互斥鎖的 /product/lock
// 執行：./k6/run.sh mutex
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 50,          // 50 個虛擬使用者同時打
  iterations: 5000, // 總共打 5000 次
};

export default function () {
  const res = http.get('http://localhost:8080/product/lock/1');
  check(res, { '查了 DB': (r) => r.json('source') === 'MISS' });
}
