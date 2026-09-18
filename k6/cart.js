// Day 5：同一個商品狂加 1，String 存整包 JSON 會少加，Hash 的 HINCRBY 不會
// 執行：./k6/run.sh cart
import http from 'k6/http';

export const options = {
  vus: 50,          // 50 個虛擬使用者同時加同一件商品
  iterations: 5000, // 總共各加 5000 次
};

export default function () {
  http.get('http://localhost:8080/cart/json/add');
  http.get('http://localhost:8080/cart/hash/add');
}
