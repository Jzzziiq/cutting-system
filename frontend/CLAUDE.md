---
paths: ["frontend/**"]
---

# 前端开发规则

## 常用命令

```powershell
cd frontend; npm install; npm run dev
cd frontend; npm run build
```

## 硬约束

- 生产加工核心页面是 `/cutting/data-input` 和 `/cutting/layout-workbench`；不要恢复旧 `/algorithm` 页面入口，除非用户明确要求。
- 后端接口路径、参数或返回字段变化时，必须同步检查 `frontend/src/api/`。
