// Day 4：同時打 Java 的 ++ 和 Redis 的 INCR，看誰會少加
// 執行：./k6/run.sh counter
import http from 'k6/http';

export const options = {
  vus: 50,          // 50 個虛擬使用者同時打
  iterations: 5000, // 總共各打 5000 次
};

export default function () {
  http.get('http://localhost:8080/counter/java');
  http.get('http://localhost:8080/counter/redis');
}
