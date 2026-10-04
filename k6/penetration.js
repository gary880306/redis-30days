// Day 21：每次都查一個不存在的商品，而且 id 都不一樣
// 執行：./k6/run.sh penetration
import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

export const options = {
  vus: 50,          // 50 個虛擬使用者同時打
  iterations: 5000, // 總共打 5000 次
};

export default function () {
  const id = 10001 + exec.scenario.iterationInTest; // 商品只到 10000 號，從 10001 開始每次換一個
  const res = http.get(`http://localhost:8080/product/${id}`);
  check(res, { '查了 DB': (r) => r.json('source') === 'MISS' });
}
