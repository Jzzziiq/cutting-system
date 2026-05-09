#!/usr/bin/env python3
"""保存模拟排版订单及结果到数据库"""
import urllib.request, json

BASE = "http://localhost:8080"

# 1. Login
req = urllib.request.Request(f"{BASE}/auth/login", data=b"username=admin&password=123456")
token = json.loads(urllib.request.urlopen(req).read())["data"]["token"]
print(f"登录成功, token: {token[:30]}...")

def api(method, path, data=None):
    url = f"{BASE}{path}"
    body = json.dumps(data).encode() if data else None
    headers = {"Content-Type": "application/json", "Authorization": f"Bearer {token}"}
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    resp = urllib.request.urlopen(req, timeout=60)
    return json.loads(resp.read())

# 2. Generate order items (80 pieces)
items = []
piece_specs = [
    ("A", 720, 450, 4), ("B", 700, 400, 4), ("C", 680, 420, 4),
    ("D", 650, 380, 4), ("E", 600, 350, 5), ("F", 580, 400, 4),
    ("G", 550, 360, 4), ("H", 500, 300, 6), ("I", 480, 320, 4),
    ("J", 450, 350, 4), ("K", 400, 300, 6), ("L", 380, 250, 4),
    ("M", 350, 280, 4), ("N", 300, 200, 6), ("O", 250, 180, 6),
    ("X", 700, 500, 3), ("Y", 650, 450, 3), ("Z", 600, 450, 3),
    ("W", 800, 500, 2),
]
for code, l, w, count in piece_specs:
    for i in range(count):
        items.append({
            "partName": f"门板-{code}{i+1:02d}",
            "length": l, "width": w, "thickness": 18,
            "quantity": 1,
            "materialName": "颗粒板", "color": "白色",
            "allowRotation": 1
        })

print(f"订单明细: {len(items)} 个工件")

# 3. Create order
order = api("POST", "/orders", {
    "orderNo": "ORD-20260509-SIM",
    "customerId": 1,
    "customerName": "张三",
    "processName": "柜门切割排版",
    "orderStatus": 3,
    "remark": "模拟排版测试-80件柜门",
    "rawMaterialJson": '{"boardId":1,"brand":"爱格","materialType":"颗粒板","color":"白色","length":2440,"width":1220,"thickness":18}',
    "items": items
})

order_id = order.get("orderId")
print(f"订单创建成功: ID={order_id}")

# 4. Save layout result
result_data = json.load(open("/tmp/big_result.json"))

total_area = sum(
    (s.get("containerLength", 0) or s.get("instance", {}).get("L", 0)) *
    (s.get("containerWidth", 0) or s.get("instance", {}).get("W", 0))
    for s in result_data
)
used_area = sum(
    sum(p["l"] * p["w"] for p in s.get("placeSquareList", []))
    for s in result_data
)
rate = round(used_area / total_area, 4) if total_area > 0 else 0

layout = api("POST", "/layout-results", {
    "orderId": order_id,
    "usageRate": rate,
    "totalArea": total_area,
    "containerCount": len(result_data),
    "resultJson": json.dumps(result_data)
})

print(f"排版结果保存成功: resultId={layout.get('resultId')}, 利用率={rate*100:.1f}%")
print(f"\n汇总: 订单 #{order_id}")
print(f"  工件: {len(items)} 个")
print(f"  板材: {len(result_data)} 张")
for i, s in enumerate(result_data):
    r = s.get("rate", 0) * 100
    pcs = len(s.get("placeSquareList", []))
    print(f"    板{i+1}: 利用率 {r:.1f}%  工件 {pcs} 个")
print(f"  平均利用率: {rate*100:.1f}%")
print(f"\n现在可以在排版工作台的历史记录中查看此订单！")
