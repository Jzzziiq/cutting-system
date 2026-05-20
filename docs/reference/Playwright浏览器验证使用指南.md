# Playwright 浏览器验证使用指南

## 首次安装

```bash
pip install playwright
python -m playwright install chromium
```

## 单前端验证（无需后端/MySQL）

```bash
python "C:/Users/JZQ/.claude/plugins/cache/anthropic-agent-skills/document-skills/f458cee31a75/skills/webapp-testing/scripts/with_server.py" \
  --server "cd frontend && npm run dev" --port 5173 \
  -- python test_e2e.py
```

## 测试脚本模板

```python
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.set_viewport_size({"width": 1440, "height": 900})

    page.goto('http://localhost:5173', wait_until='networkidle')
    page.wait_for_timeout(2000)
    page.screenshot(path='/tmp/screenshot.png', full_page=True)

    # 检查元素
    has_element = '目标文本' in page.content()
    count = page.locator('canvas').count()

    browser.close()
```

## 关键要点

- `wait_until='networkidle'` — 必须等 JS 执行完毕再检查 DOM
- `headless=True` — 无头模式
- 脚本里不用管服务器启停，`with_server.py` 自动管理
- 截图输出到 `/tmp/`
