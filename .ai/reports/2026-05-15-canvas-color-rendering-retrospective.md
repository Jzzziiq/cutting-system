# Canvas 颜色渲染优化卡壳复盘

## 背景

- 日期：2026-05-15
- 任务：优化排版结果 Canvas 的工件颜色分布逻辑，只调整前端渲染效果，不修改算法返回的坐标、尺寸和利用率数据。
- 主要修改文件：`frontend/src/composables/useLayoutCanvas.js`
- 最终结果：前端构建通过，浏览器验证通过；卡壳主要发生在本地浏览器验证环节，而不是颜色渲染逻辑本身。

## 本次为什么卡壳

### 1. 验证目标过早升级

实现完成后，`npm run build` 已能验证语法和打包正确性；但为了进一步确认 Canvas 像素、板材切换和 hover tooltip，我直接进入 Playwright 浏览器自动化验证。

这个方向本身合理，但没有先把验证链路拆成更小的步骤，导致工具问题、路由鉴权问题、网络 mock 问题混在一起排查。

以后应先按顺序验证：

1. `npm run build`
2. `git diff --check`
3. 如果需要浏览器验证，再用最小页面脚本只验证一个交互点
4. 最后再加像素采样、hover、tab 切换等细项

### 2. Playwright 包使用方式绕了远路

一开始尝试通过：

```powershell
npx --yes --package playwright node -
```

直接运行 Node 脚本并 `import/require('playwright')`。这在当前环境里没有稳定拿到 `playwright` 包解析路径，还先后遇到：

- CommonJS `require` 和 top-level `await` 混用导致的模块格式错误
- `npx --package playwright` 下 Node 进程仍无法解析 `playwright` 包

后来确认可用的是本仓库环境里的 `@playwright/cli`：

```powershell
npx --yes --package @playwright/cli playwright-cli --help
```

正确思路应是优先使用 `playwright-cli run-code`，并传入一个函数：

```powershell
npx --yes --package @playwright/cli playwright-cli -s <session> run-code "(page) => page.title()"
```

复杂脚本应放到临时文件，并用：

```powershell
npx --yes --package @playwright/cli playwright-cli -s <session> run-code --filename target\<script>.js
```

### 3. `run-code` 入参格式误判

`playwright-cli run-code` 需要的是一个会被调用的函数，不是普通脚本文本。

错误形式：

```js
console.log('hello')
```

会触发：

```text
TypeError: __fn__ is not a function
```

正确形式：

```js
(page) => page.title()
```

复杂异步逻辑用：

```js
(async (page) => {
  // ...
  return result
})
```

### 4. 鉴权和 sessionStorage 注入时机不对

页面有路由守卫：未登录访问 `/cutting/layout-workbench` 会跳到 `/login`。

一开始在页面已加载后用 `page.evaluate` 写入 `localStorage` 和 `sessionStorage`，容易错过 Pinia 初始化和路由守卫的读取时机，导致页面仍停在登录页或发生跳转。

正确做法是跳转目标页面前使用 `page.addInitScript`：

```js
await page.addInitScript(({ draftId, draft }) => {
  localStorage.setItem('cutting_system_token', 'visual-check-token')
  localStorage.setItem('cutting_system_user', JSON.stringify({ username: 'visual-check', permissions: [] }))
  sessionStorage.setItem(`layout-draft-${draftId}`, JSON.stringify(draft))
}, { draftId, draft })
```

### 5. 网络 mock 规则过宽，拦截了 Vite 源码模块

为了避免前端调用后端接口卡住，使用过：

```js
await page.route('**/api/**', ...)
```

这条规则过宽，在 Vite 开发服务里会误伤源码模块路径，例如：

```text
/src/api/auth.js
```

浏览器因此收到 JSON 但期望 JS module，报错：

```text
Expected a JavaScript-or-Wasm module script but the server responded with a MIME type of "application/json"
```

正确写法应限制到真实后端代理路径：

```js
await page.route('http://127.0.0.1:5176/api/**', ...)
```

不要用会匹配 `/src/api/*.js` 的泛化规则。

### 6. `networkidle` 不适合这个本地页面

曾使用：

```js
await page.goto(url, { waitUntil: 'networkidle' })
```

页面包含历史列表、接口请求或开发服务连接时，`networkidle` 容易超时。对这个任务而言，真正需要的是 Canvas 出现，而不是所有网络完全静默。

更稳妥的等待条件：

```js
await page.goto(url, { waitUntil: 'domcontentloaded' })
await page.waitForSelector('canvas.layout-canvas', { state: 'visible', timeout: 10000 })
```

### 7. Hover 验证点选错了

第一次 hover 使用 Canvas 中心点。切换到第二张板后，中心点不一定落在工件上，所以 tooltip 不出现。

正确做法是按板材坐标和当前缩放/平移换算一个确定落在工件内部的屏幕坐标：

```js
const hoverPoint = await page.evaluate(() => {
  const canvas = document.querySelector('canvas.layout-canvas')
  const rect = canvas.getBoundingClientRect()
  const L = 1200
  const W = 800
  const padding = 60
  const zoom = Math.min((rect.width - padding * 2) / L, (rect.height - padding * 2) / W)
  const pan = {
    x: (rect.width - L * zoom) / 2,
    y: (rect.height - W * zoom) / 2
  }
  return {
    x: rect.left + 80 * zoom + pan.x,
    y: rect.top + (W - 70) * zoom + pan.y
  }
})
await page.mouse.move(hoverPoint.x, hoverPoint.y)
```

## 后续避免卡壳的执行准则

### 前端 Canvas 类任务

1. 先定位绘制入口，不改数据源：本项目当前入口是 `frontend/src/composables/useLayoutCanvas.js`。
2. 保留交互状态链路：`zoom`、`panOffset`、`hoveredPiece`、`activeBoardIndex` 不要为视觉改动重写。
3. 视觉映射应是纯函数：输入工件列表和尺寸，输出颜色/文字色，不回写算法数据。
4. Canvas 和 SVG 导出如果展示同一份结果，应复用同一套视觉映射。
5. 文本可读性用亮度/对比度计算，不手写“深色背景白字”的猜测分支。

### 浏览器验证

1. 能用构建验证的先构建，不要一开始就上复杂浏览器脚本。
2. 使用 `playwright-cli run-code` 时，脚本必须是函数。
3. 注入鉴权和草稿数据用 `addInitScript`，不要等 SPA 初始化后再写 storage。
4. mock 接口只匹配真实 API 前缀，避免拦截 Vite 的 `/src/api/*.js` 模块。
5. 等待具体 UI 元素，不默认使用 `networkidle`。
6. hover、像素采样、拖拽等验证点必须落在已知坐标，不用“页面中心”这种不稳定位置。
7. 临时脚本放 `target/` 或 `output/`，验证后删除。
8. 开发服务启动后记录 PID，结束时清理子进程，避免残留端口。

## 推荐验证模板

```powershell
# 1. 构建验证
cd frontend
npm run build

# 2. 空白/格式检查
cd ..
git diff --check -- frontend/src/composables/useLayoutCanvas.js

# 3. 启动临时前端服务
$proc = Start-Process -FilePath 'npm.cmd' `
  -ArgumentList @('run','dev','--','--host','127.0.0.1','--port','5176') `
  -WorkingDirectory 'F:\Code\Java\cutting-system\frontend' `
  -PassThru `
  -WindowStyle Hidden
$proc.Id | Set-Content -LiteralPath 'F:\Code\Java\cutting-system\target\vite-check.pid'

# 4. Playwright CLI 使用函数脚本验证
npx --yes --package @playwright/cli playwright-cli -s canvas-check open http://127.0.0.1:5176/login
npx --yes --package @playwright/cli playwright-cli -s canvas-check run-code --filename target\<check-script>.js
npx --yes --package @playwright/cli playwright-cli -s canvas-check close
```

## 本次有效验证结果

最终浏览器验证返回的关键信息：

```json
{
  "boardTabCount": 2,
  "sameSizeDistance": 0,
  "adjacentDistance": 56.40035460881429,
  "activeTabText": "第 2 张板\n63.0%",
  "tooltipVisible": true
}
```

说明：

- 同尺寸工件颜色一致。
- 相邻不同尺寸工件颜色有可见差异。
- 板材切换仍可用。
- hover tooltip 仍可用。

## 一句话结论

这次慢在验证工具链和 SPA 环境准备，而不是业务实现。以后遇到类似任务，应先用构建兜底，再用最小 Playwright 函数脚本逐项增加验证，不要把包解析、鉴权注入、网络 mock、Canvas 像素采样一次性揉在一起。
