# 问题记录

## 背景

- 来源：Codex 浏览器标注 Comment 1，页面 `http://127.0.0.1:5173/cutting/data-input`
- 时间：2026-05-18
- 涉及模块：网页端 / 生产加工 / 加工数据输入 / 按板材分组工件录入
- 涉及文件：
  - `frontend/src/views/cutting/DataInputView.vue`
  - `frontend/src/components/cutting/BoardGroupTable.vue`
  - `frontend/src/composables/useBoardWorkpieceGroups.js`
  - `frontend/src/styles/main.css`

## 问题列表

### P1-001 点击“添加工件行”后新增行数据存在，但输入表格不可见

- 优先级：P1
- 类型：Bug/前端/体验
- 现象：在加工数据输入页选择板材后，点击板材分组内的“添加工件行”按钮，页面没有出现可输入的表格行；截图中右侧分组只显示组头和底部添加按钮。
- 期望：点击“添加工件行”后，应在当前板材分组下显示 Element Plus 表格的表头和可编辑输入行，用户可以录入工件名称、长、宽、数量、备注。
- 相关文件：
  - `frontend/src/components/cutting/BoardGroupTable.vue`
  - `frontend/src/composables/useBoardWorkpieceGroups.js`
  - `frontend/src/views/cutting/DataInputView.vue`
  - `frontend/src/styles/main.css`
- 相关代码位置：
  - `frontend/src/components/cutting/BoardGroupTable.vue:114`：分组内容容器 `.group-body`
  - `frontend/src/components/cutting/BoardGroupTable.vue:115-123`：`el-table` 使用 `height="100%"`
  - `frontend/src/components/cutting/BoardGroupTable.vue:170-174`：“添加工件行”按钮触发 `emit('add-item', group.id)`
  - `frontend/src/components/cutting/BoardGroupTable.vue:268-272`：`.group-body` 仅设置 `display:flex`、`flex-direction:column`、`min-height:0`，没有可用于百分比高度计算的明确高度
  - `frontend/src/views/cutting/DataInputView.vue:179`：父组件将 `@add-item` 直接绑定到 `addItem`
  - `frontend/src/composables/useBoardWorkpieceGroups.js:110`：新增板材组时已创建 1 条空工件
  - `frontend/src/composables/useBoardWorkpieceGroups.js:121-126`：`addItem(groupId)` 会向 `group.items` 追加空工件
- 复现步骤：
  1. 启动前端并访问 `/cutting/data-input`。
  2. 在左侧选择并添加任意原材料板材。
  3. 点击右侧该板材组底部的“添加工件行”按钮。
  4. 观察右侧区域：分组统计会显示行数/错误数变化，但可编辑表格行没有显示。
- 原因分析：
  - 当前按钮事件链是通的：`BoardGroupTable` 发出 `add-item`，`DataInputView` 调用 `useBoardWorkpieceGroups.addItem`，并将空行追加到 `group.items`。
  - 截图中顶部已显示“1 组板材 · 2 行工件”，组头显示“错误 2”，说明数据层已经有 2 条空工件并参与校验。
  - 不可见的核心原因是 `BoardGroupTable.vue` 中嵌套在分组列表里的 `el-table` 设置了 `height="100%"`，但它的直接父级 `.group-body` 是自适应高度容器，没有明确高度。Element Plus 固定高度表格在这种场景下会把表体区域压缩，导致表头/行输入区不可见或近似 0 高度。
- 建议解决方案：
  - 对板材分组内的表格不要使用 `height="100%"`。优先移除该属性，让 `el-table` 根据当前行数自然撑开。
  - 如果需要限制单个板材组过高，可改用 `max-height` 或外层滚动容器，例如给表格包一层 `.group-table-scroll { overflow-x:auto; }`，并对多行场景设置合理的最大高度。
  - 保持数据流不变：继续由 `BoardGroupTable` 通过事件上抛，`DataInputView` 调用 `useBoardWorkpieceGroups` 修改 `boardGroups`，不要在展示组件中直接创建跨组件状态。
  - 可选增强：`addItem` 后在 `nextTick` 中聚焦新增行第一个输入框，提升录入效率；该增强应在表格可见问题修复后再做。
- 风险影响：
  - 该问题阻塞加工数据输入核心流程。用户无法看到或编辑新增工件行，即使数据已被创建，也无法完成正常录入。
  - 继续保留当前布局会让用户误判按钮无效，并可能重复点击产生多条不可见空行。
- 建议验证方式：
  - 运行 `cd frontend && npm run build`。
  - 本地打开 `/cutting/data-input`，添加板材后确认默认 1 行空工件可见。
  - 点击“添加工件行”后确认新增输入行立即可见，表格表头、输入框、删除按钮布局正常。
  - 验证粘贴 TSV、键盘上下左右/Tab/Enter 导航、删除行仍可正常工作。
  - 在 1242×575 近似标注视口和移动窄屏下检查右侧表格不被压缩为 0 高度。

## 待确认点

- 是否要求单个板材组内部固定高度滚动；如果没有明确要求，建议优先采用自然高度，减少嵌套滚动。
- 修复后是否需要自动聚焦到新增行第一个输入框；这属于体验增强，不是本问题的最低修复条件。

## 给执行者的说明

- 请先读取 `F:\Code\Java\cutting-system\AGENTS.md` 和 `F:\Code\Java\cutting-system\CLAUDE.md`。
- 请以本文档为唯一问题清单，逐项分析、修复和验证。
