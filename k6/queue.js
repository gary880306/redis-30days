// Day 26：100 件商品，50 個人同時搶，打的是搶到先排隊的 /stock/buy/queue
// 執行：./k6/run.sh queue
import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

export const options = {
  vus: 50,          // 50 個虛擬使用者同時打
  iterations: 5000, // 總共打 5000 次
};

export default function () {
  const user = exec.scenario.iterationInTest; // 第幾次打就當作第幾號使用者
  const res = http.post(`http://localhost:8080/stock/buy/queue?id=1&user=${user}`);
  check(res, { '排隊中': (r) => r.json('result') === '排隊中' });
}
