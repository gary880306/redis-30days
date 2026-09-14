#!/usr/bin/env bash
#
# 跑 k6 並把完整報告存到 k6-reports/
#
#   ./k6/run.sh hello
#
# k6 預設只把報告印在終端機、不存檔，所以這裡用 tee 同時存一份。
# 之後新增腳本（k6/xxx.js）也可以直接用：./k6/run.sh xxx
#
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p k6-reports

SCRIPT="${1:?用法: ./k6/run.sh <腳本名，不含 .js>}"
[ -f "k6/${SCRIPT}.js" ] || { echo "找不到 k6/${SCRIPT}.js"; exit 1; }

OUT="k6-reports/${SCRIPT}-$(date +%Y%m%d-%H%M%S).txt"
k6 run "k6/${SCRIPT}.js" 2>&1 | tee "$OUT"

echo ""
echo "報告已存到：$OUT"
