// Day 25：100 件商品，50 個人同時搶，打的是先讀再寫的 /stock/buy/wrong
// 執行：./k6/run.sh oversell
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 50,          // 50 個虛擬使用者同時打
  iterations: 5000, // 總共打 5000 次
};

export default function () {
  const res = http.post('http://localhost:8080/stock/buy/wrong?id=1');
  check(res, { '搶到': (r) => r.json() >= 0 }); // 沒庫存回 -1
}
