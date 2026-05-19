# 问题记录（第一轮）

## 背景

- 来源：浏览器页面标注评论，时间 2026-05-11。
- 涉及模块：网页端加工数据输入页、排版工作台页、生产看板页。
- 涉及文件：
  - `frontend/src/views/cutting/DataInputView.vue`
  - `frontend/src/components/cutting/OrderInfoBar.vue`
  - `frontend/src/components/cutting/RawMaterialPanel.vue`
  - `frontend/src/components/cutting/CuttingTable.vue`
  - `frontend/src/composables/useCuttingTable.js`
  - `frontend/src/views/cutting/LayoutWorkbenchView.vue`
  - `frontend/src/components/cutting/LayoutHistoryPanel.vue`
  - `frontend/src/views/ProductionKanbanView.vue`
  - `frontend/src/styles/main.css`

## 问题列表

### P1-001 下料尺寸表格在宽屏下未占满父容器

- 优先级：P1
- 类型：前端/体验
- 现象：在 `/cutting/data-input` 页面，当父容器宽度较大时，右侧"下料尺寸输入"表格列宽固定，表格内容未铺满父级容器，右侧出现大片空白，视觉上显得不完整。
- 期望：表格整体应随父容器宽度自适应，常用列在宽屏下合理拉伸，保持 Excel 风格输入区占满右侧可用空间。
- 相关文件：
  - `frontend/src/components/cutting/CuttingTable.vue`
  - `frontend/src/composables/useCuttingTable.js`
  - `frontend/src/views/cutting/DataInputView.vue`
- 相关代码位置：
  - `CuttingTable.vue:50`：`el-table` 渲染下料尺寸表。
  - `CuttingTable.vue:59`：列使用 `:width="col.width"` 固定宽度。
  - `useCuttingTable.js:25`：下料表列配置。
  - `DataInputView.vue:115`：右侧表格容器。
- 复现步骤：
  1. 启动网页端并进入 `http://127.0.0.1:5173/cutting/data-input`。
  2. 使用较宽浏览器视口，例如 1719x970。
  3. 观察右侧"下料尺寸输入"表格。
- 风险影响：宽屏使用时表格区域浪费明显，数据录入页面完成度和专业感下降；后续增加列或横向滚动时也可能出现布局不一致。
- 建议验证方式：`cd frontend && npm run build`；在 1366px、1719px、1920px 视口下检查 `/cutting/data-input`，确认表格宽度、列宽和横向滚动表现。

### P1-002 下料尺寸表头字段需要居中显示

- 优先级：P2
- 类型：前端/体验
- 现象：`/cutting/data-input` 页面下料尺寸表格表头字段，例如"订单号""客户""板材类型""长(L)"等，目前偏左显示。
- 期望：表头字段统一居中显示，与表格录入场景的字段视觉对齐要求一致。
- 相关文件：
  - `frontend/src/components/cutting/CuttingTable.vue`
  - `frontend/src/styles/main.css`
- 相关代码位置：
  - `CuttingTable.vue:50`：`el-table`。
  - `CuttingTable.vue:59`：`el-table-column`。
  - `main.css:651` 之后：切割页面共享面板样式，可考虑补充表格样式。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/data-input`。
  2. 查看右侧"下料尺寸输入"的表头行。
- 风险影响：表头与单元格视觉重心不一致，影响表格录入体验。
- 建议验证方式：构建前端并在浏览器检查表头居中；同时确认表体输入框和校验红框没有发生错位。

### P1-003 顶部客户输入框点击后应弹出数据库候选列表

- 优先级：P1
- 类型：前端/接口/体验
- 现象：`/cutting/data-input` 顶部"客户名称"输入框当前是普通 `el-input`，点击后不会弹出数据库查询到的候选列表。
- 期望：点击或聚焦输入框时，下方出现可选择的候选列表；列表数据来自后端数据库查询结果，选择后回填输入框。
- 相关文件：
  - `frontend/src/components/cutting/OrderInfoBar.vue`
  - `frontend/src/views/cutting/DataInputView.vue`
  - `frontend/src/api/customers.js`
  - `frontend/src/api/users.js`
- 相关代码位置：
  - `OrderInfoBar.vue:45`：客户名称表单项。
  - `OrderInfoBar.vue:46`：客户名称普通输入框。
  - `DataInputView.vue:16`：`customer` 状态。
  - `customers.js:3`：已有客户列表接口封装。
  - `users.js:3`：已有用户列表接口封装。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/data-input`。
  2. 点击顶部"客户名称"输入框。
  3. 观察输入框下方没有候选下拉列表。
- 风险影响：用户需要手动输入名称，容易产生错别字或与数据库记录不一致，影响后续订单、客户关联和排版记录沉淀。
- 建议验证方式：使用 Element Plus 下拉/自动补全组件后，验证聚焦、输入过滤、选择回填、清空、接口异常空态；运行 `cd frontend && npm run build`。

### P1-004 原材料搜索输入框点击后应弹出数据库候选列表

- 优先级：P1
- 类型：前端/接口/体验
- 现象：`/cutting/data-input` 左侧"原材料选择"的板材搜索框目前需要输入关键字或回车/点击搜索后才展示结果；点击输入框时不会直接弹出数据库候选列表。
- 期望：点击或聚焦搜索框时，下方出现可选择的板材候选列表，数据来自数据库中的板材记录，并支持继续输入筛选。
- 相关文件：
  - `frontend/src/components/cutting/RawMaterialPanel.vue`
  - `frontend/src/api/boards.js`
- 相关代码位置：
  - `RawMaterialPanel.vue:21`：`doSearch` 搜索逻辑。
  - `RawMaterialPanel.vue:65`：板材搜索输入框。
  - `RawMaterialPanel.vue:79`：搜索结果区域。
  - `boards.js:3`：已有板材列表接口封装。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/data-input`。
  2. 点击左侧"搜索板材（品牌/材质/颜色）"输入框。
  3. 观察没有出现预选候选列表。
- 风险影响：原材料选择发现性较弱，现场录入人员必须知道关键字才能查询，影响录入效率。
- 建议验证方式：验证聚焦即加载候选、输入筛选、添加板材、已选去重、空态和 loading 状态；运行 `cd frontend && npm run build`。

### P1-005 生产看板页缺少与其他管理页一致的容器包裹

- 优先级：P1
- 类型：前端/体验
- 现象：`/production-board` 页面标题"生产看板"和"+ 新建任务"直接放在内容面板里，缺少类似客户管理、板材管理、用户管理页面的白色容器包裹，视觉上显得孤立。
- 期望：生产看板页面采用与其他管理页一致的容器结构和标题区样式，例如使用 `section-block` 包住标题、操作按钮、错误提示和看板内容。
- 相关文件：
  - `frontend/src/views/ProductionKanbanView.vue`
  - `frontend/src/styles/main.css`
  - `frontend/src/views/CustomersView.vue`
  - `frontend/src/views/BoardsView.vue`
  - `frontend/src/views/UsersView.vue`
- 相关代码位置：
  - `ProductionKanbanView.vue:184`：生产看板页面根部模板。
  - `ProductionKanbanView.vue:185`：当前标题区。
  - `CustomersView.vue:206`、`BoardsView.vue:224`、`UsersView.vue:122`：其他管理页 `section-block` 参考。
  - `main.css:304`：`section-block` 共享容器样式。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/production-board`。
  2. 对比客户管理、板材管理或用户管理页面的标题和内容容器。
- 风险影响：同一后台系统内页面视觉层级不一致，生产看板显得未完成。
- 建议验证方式：调整后检查 `/production-board` 与其他管理页容器、间距、标题按钮对齐；运行 `cd frontend && npm run build`。

### P1-006 排版工作台历史记录搜索框点击后应出现预选框

- 优先级：P1
- 类型：前端/接口/体验
- 现象：`/cutting/layout-workbench` 左侧"历史排单记录"的搜索框当前是普通输入搜索，需要输入或回车触发查询，点击后没有预选框。
- 期望：点击或聚焦历史记录搜索框时，应出现可选择的预选框，展示历史排单记录或客户/订单候选；选择候选后联动加载对应排版记录。
- 相关文件：
  - `frontend/src/components/cutting/LayoutHistoryPanel.vue`
  - `frontend/src/views/cutting/LayoutWorkbenchView.vue`
  - `frontend/src/api/layout-results.js`
- 相关代码位置：
  - `LayoutHistoryPanel.vue:23`：`loadRecords` 加载历史记录。
  - `LayoutHistoryPanel.vue:59`：历史记录搜索输入框。
  - `LayoutHistoryPanel.vue:73`：历史记录列表。
  - `LayoutWorkbenchView.vue:64`：历史记录选择处理。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/layout-workbench`。
  2. 点击左侧"搜索客户/订单名"输入框。
  3. 观察没有出现预选框。
- 风险影响：历史排单记录检索依赖手动输入，用户难以快速发现已有记录；影响排版工作台回看和复用效率。
- 建议验证方式：验证聚焦预加载、输入过滤、选择记录、空态、接口异常状态；运行 `cd frontend && npm run build`。

## 待确认点

- 注释 3 标注位置是"客户名称"输入框，但评论描述为"数据库中查询到的用户"。执行前需确认候选数据源应为客户列表、用户列表，还是二者之一的表述误差。
- 注释 4 写"同注释3的问题"，但标注位置是"原材料选择"的板材搜索框。执行前需确认这里的候选列表应为板材记录，而不是用户记录。
- 注释 6 的"预选框"需确认候选内容是历史排单记录、客户/订单搜索建议，还是两者组合。
