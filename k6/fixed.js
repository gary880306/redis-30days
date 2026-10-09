// Day 26：100 件商品，200 個人每秒按一次，打的是有固定視窗限流的 /stock/buy/fixed
// 執行：./k6/run.sh fixed
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 200,         // 200 個虛擬使用者同時打
  iterations: 1000, // 總共打 1000 次
};

export default function () {
  const res = http.post('http://localhost:8080/stock/buy/fixed?id=1');
  check(res, {
    '被擋下來': (r) => r.status === 429,
    '搶到': (r) => r.status === 200 && r.json() >= 0, // 被擋下來的沒有內容，先看 200 再看數字
  });
  sleep(1); // 按完等 1 秒再按下一次
}
