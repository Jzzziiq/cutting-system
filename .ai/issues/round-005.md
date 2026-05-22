# 问题记录

## 背景

- 来源：Codex 复核 `.ai/reports/current.md` 后的返工分析
- 时间：2026-05-18
- 涉及模块：网页端 / 生产加工 / 加工数据输入 / 按板材分组工件录入
- 当前页面：`http://127.0.0.1:5173/cutting/data-input`
- 修复报告：`.ai/reports/current.md`
- 涉及文件：
  - `frontend/src/views/cutting/DataInputView.vue`
  - `frontend/src/components/cutting/BoardGroupTable.vue`
  - `frontend/src/composables/useBoardWorkpieceGroups.js`
  - `frontend/src/styles/main.css`

## 旧问题完成状态

| 编号 | 原问题 | 当前状态 | 判定依据 |
| --- | --- | --- | --- |
| P1-001 | 点击“添加工件行”后新增行数据存在，但输入表格不可见 | 未解决，需返工 | 修复报告仅将 `height="100%"` 改为 `max-height="400"`；浏览器运行时仍无表头、列、输入框，并出现 Vue 递归更新错误 |

## 问题列表

### P1-001 修复后仍不可输入：渲染期间校验写入响应式状态导致递归更新，Element Plus 表格列未注册

- 优先级：P1
- 类型：Bug/前端/体验
- 现象：
  - 点击“添加工件行”后，顶部统计会增加行数，但右侧板材分组下仍没有可输入的表格。
  - 运行时 DOM 中 `.group-items-table` 只有约 18px 高，表头高度为 0，`th`/`td`/`input` 数量均为 0。
  - 控制台反复报错：`Maximum recursive updates exceeded in component <DataInputView>`.
- 期望：
  - 添加板材后应显示默认 1 行可编辑工件输入表格。
  - 点击“添加工件行”后应显示新增行，且表头、单元格输入框和删除按钮正常渲染。
  - 页面不应出现 Vue 递归更新错误。
- 相关文件：
  - `frontend/src/components/cutting/BoardGroupTable.vue`
  - `frontend/src/composables/useBoardWorkpieceGroups.js`
  - `frontend/src/views/cutting/DataInputView.vue`
- 相关代码位置：
  - `frontend/src/components/cutting/BoardGroupTable.vue:98-101`：模板中多次调用 `getGroupStats(group)` 渲染工件数、面积和错误数。
  - `frontend/src/components/cutting/BoardGroupTable.vue:121`：当前已按报告改为 `max-height="400"`，但问题仍存在。
  - `frontend/src/composables/useBoardWorkpieceGroups.js:67-94`：`validateItem(item, board)` 会写入 `item._validation = errors`。
  - `frontend/src/composables/useBoardWorkpieceGroups.js:265-279`：`getGroupStats(group)` 在计算统计时调用 `validateItem(item, group.board)`，因此统计函数不是纯函数。
  - `frontend/src/views/cutting/DataInputView.vue:178`：父组件将 `getGroupStats` 作为 prop 传入 `BoardGroupTable`。
- 复现步骤：
  1. 启动前端并访问 `/cutting/data-input`。
  2. 添加任意原材料板材。
  3. 点击右侧板材组内的“添加工件行”。
  4. 观察右侧分组统计行数增加，但表格仍不可输入。
  5. 查看控制台，可见 `Maximum recursive updates exceeded in component <DataInputView>`。
- 运行时证据：
  - 页面 summary 已显示 `1 组板材 · 5 行工件`，说明数据层确实追加了工件行。
  - `.group-items-table` 根节点约 18px 高，`.el-table__header-wrapper` 高度为 0。
  - 表格 DOM 的 `colgroup` 为空，`thCount=0`、`tdCount=0`、`inputCount=0`，说明 Element Plus 表格列没有完成注册和渲染。
  - `.ai/reports/current.md` 中的 `npm run build` 只能证明编译通过，无法覆盖这个运行时响应式递归问题。
- 原因分析：
  - 上一轮修复只处理了表格高度属性：`height="100%"` → `max-height="400"`。这个改动可以规避父容器百分比高度无法解析的问题，但不是当前仍失败的根因。
  - 当前真正阻断渲染的是响应式副作用：`BoardGroupTable` 模板渲染时调用 `getGroupStats(group)`；`getGroupStats` 又调用 `validateItem`；`validateItem` 写入 `item._validation`；这会修改当前渲染依赖的 `boardGroups` 数据，触发新一轮渲染，形成递归更新。
  - 递归更新发生后，Element Plus 表格内部列注册/布局流程被打断，最终出现只有隐藏列占位 `<div>`、没有 `th/td/input` 的空表格。
- 建议解决方案：
  - 将校验逻辑拆成纯函数和写入函数，避免模板渲染期间写响应式状态。
  - 建议新增纯函数，例如 `getItemValidationErrors(item, board)`，只返回错误对象，不修改 `item`。
  - `validateItem(item, board)` 内部调用 `getItemValidationErrors` 后再写 `item._validation`，仅用于粘贴、确认提交、显式输入校验等事件流程。
  - `getGroupStats(group)` 必须改为纯统计函数：不要调用会写入状态的 `validateItem`，如需统计错误数，调用纯 `getItemValidationErrors(item, group.board)` 并统计返回对象。
  - 可选优化：减少模板中对 `getGroupStats(group)` 的重复调用，例如在父级或组合式函数中派生稳定的统计数据；但最低修复条件是先消除渲染期 mutation。
  - `max-height="400"` 可以暂时保留；递归更新修复后再用浏览器验证其高度表现。如仍有压缩，再改为自然高度或外层滚动容器。
- 风险影响：
  - 当前问题阻塞核心录入流程。用户会看到行数增加，却无法编辑任何工件尺寸。
  - 递归更新会造成页面运行时错误，可能影响同页其他组件响应式更新和 Element Plus 内部布局。
  - 如果只继续调整 CSS，不处理 `getGroupStats` 的副作用，表格仍可能无法稳定渲染。
- 建议验证方式：
  - 运行 `cd frontend && npm run build`。
  - 在浏览器打开 `/cutting/data-input`，添加板材后确认默认输入行可见。
  - 点击“添加工件行”后确认新增输入行可见，`.group-items-table input` 数量随行数增加。
  - 打开控制台确认不再出现 `Maximum recursive updates exceeded`。
  - 验证粘贴 TSV、键盘导航、删除行、确认排版前校验仍正常。
  - 在 1242×575 近似视口下验证表格表头、输入框和添加按钮不重叠、不被压缩为 0 高度。

## 待确认点

- 是否需要新增“输入时即时标红”的交互；如果需要，应通过输入事件或受控校验动作更新 `_validation`，不要通过模板统计函数隐式写入。
- 单个板材组超过多行时，是采用 Element Plus 表格内部滚动，还是采用分组外层自然高度加页面滚动，需要产品侧确认。

## 给执行者的说明

- 请先读取 `F:\Code\Java\cutting-system\AGENTS.md` 和 `F:\Code\Java\cutting-system\CLAUDE.md`。
- 请以本文档为唯一问题清单，逐项分析、修复和验证。
