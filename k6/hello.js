// Day 2：第一支壓測腳本，測試用
// 執行：k6 run k6/hello.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 10,          // 10 個虛擬使用者
  duration: '10s',
};

export default function () {
  const res = http.get('http://localhost:8080/hello/get?key=day02');
  check(res, { 'status is 200': (r) => r.status === 200 });
}
