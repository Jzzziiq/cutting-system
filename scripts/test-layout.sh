#!/bin/bash
# 模拟一次尺寸排版 — 约用掉4~5张板
# 用法: bash scripts/test-layout.sh

BASE="http://localhost:8080"

echo "=== 1. 登录 ==="
LOGIN=$(curl -s -X POST "$BASE/auth/login" -d 'username=admin&password=123456')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
echo "Token: ${TOKEN:0:30}..."

echo ""
echo "=== 2. 提交排版（38个柜门工件，2440×1220mm板材） ==="
RESULT=$(curl -s -X POST "$BASE/algorithm/answer" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  --max-time 300 \
  -d '{
    "L": 2440, "W": 1220,
    "isRotateEnable": true,
    "gapDistance": 3,
    "squareList": [
      {"id":"门板-01","l":720,"w":450},{"id":"门板-02","l":720,"w":450},
      {"id":"门板-03","l":700,"w":400},{"id":"门板-04","l":700,"w":400},
      {"id":"门板-05","l":680,"w":420},{"id":"门板-06","l":680,"w":420},
      {"id":"门板-07","l":650,"w":380},{"id":"门板-08","l":650,"w":380},
      {"id":"门板-09","l":600,"w":350},{"id":"门板-10","l":600,"w":350},
      {"id":"门板-11","l":600,"w":350},{"id":"门板-12","l":580,"w":400},
      {"id":"门板-13","l":580,"w":400},{"id":"门板-14","l":550,"w":360},
      {"id":"门板-15","l":550,"w":360},{"id":"门板-16","l":500,"w":300},
      {"id":"门板-17","l":500,"w":300},{"id":"门板-18","l":500,"w":300},
      {"id":"门板-19","l":480,"w":320},{"id":"门板-20","l":480,"w":320},
      {"id":"门板-21","l":450,"w":350},{"id":"门板-22","l":450,"w":350},
      {"id":"门板-23","l":400,"w":300},{"id":"门板-24","l":400,"w":300},
      {"id":"门板-25","l":400,"w":300},{"id":"门板-26","l":380,"w":250},
      {"id":"门板-27","l":380,"w":250},{"id":"门板-28","l":350,"w":280},
      {"id":"门板-29","l":350,"w":280},{"id":"门板-30","l":300,"w":200},
      {"id":"门板-31","l":300,"w":200},{"id":"门板-32","l":300,"w":200},
      {"id":"门板-33","l":250,"w":180},{"id":"门板-34","l":250,"w":180},
      {"id":"门板-35","l":250,"w":180},{"id":"门板-36","l":700,"w":500},
      {"id":"门板-37","l":650,"w":450},{"id":"门板-38","l":600,"w":450}
    ]}')

echo ""
echo "=== 3. 排版结果 ==="
echo "$RESULT" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f'板材数量: {len(data)} 张')
total_pieces = 0
total_rate = 0
for i, s in enumerate(data):
    pieces = len(s.get('placeSquareList', []))
    total_pieces += pieces
    rate = s.get('rate', 0) * 100
    total_rate += rate
    L = s.get('containerLength', '?')
    W = s.get('containerWidth', '?')
    print(f'  板{i+1}: {L}×{W}mm  利用率 {rate:.1f}%  工件 {pieces} 个')
avg_rate = total_rate / len(data) if data else 0
print(f'\n总计: {len(data)} 张板, {total_pieces} 个工件, 平均利用率 {avg_rate:.1f}%')
"

echo ""
echo "=== 4. 结果已保存到 /tmp/layout_result.json ==="
echo "$RESULT" > /tmp/layout_result.json
