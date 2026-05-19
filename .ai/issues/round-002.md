# 问题记录（第二轮）

## 背景

- 来源：浏览器页面标注评论，两轮标注时间均为 2026-05-11。
- 涉及模块：网页端加工数据输入页、排版工作台页、生产看板页。
- 涉及文件：
  - `frontend/src/views/cutting/DataInputView.vue`
  - `frontend/src/views/cutting/LayoutWorkbenchView.vue`
  - `frontend/src/views/ProductionKanbanView.vue`
  - `frontend/src/components/cutting/OrderInfoBar.vue`
  - `frontend/src/components/cutting/RawMaterialPanel.vue`
  - `frontend/src/components/cutting/CuttingTable.vue`
  - `frontend/src/components/cutting/LayoutHistoryPanel.vue`
  - `frontend/src/composables/useCuttingTable.js`
  - `frontend/src/styles/main.css`

## 旧问题完成状态

| 编号 | 原问题 | 当前状态 | 判定依据 |
| --- | --- | --- | --- |
| P1-001 | 下料尺寸表格在宽屏下未占满父容器 | 已完成 | `CuttingTable.vue` 已将列宽从 `width` 改为 `min-width`，当前截图中下料表可铺满右侧区域。 |
| P1-002 | 下料尺寸表头字段需要居中显示 | 已完成 | `CuttingTable.vue` 已新增 `.cutting-table .el-table__header-wrapper .cell { text-align: center; }`。 |
| P1-003 | 顶部客户输入框点击后应弹出数据库候选列表 | 部分完成/需返工 | 已改为 `el-autocomplete` 并接入 `listCustomers`，但第二轮标注反馈"输入框没了，无法测试原问题是否还存在"。 |
| P1-004 | 原材料搜索输入框点击后应弹出数据库候选列表 | 部分完成/需返工 | `RawMaterialPanel.vue` 已在 `@focus` 时预加载板材列表，但第二轮标注反馈失焦后列表无法收回。 |
| P1-005 | 生产看板页缺少与其他管理页一致的容器包裹 | 已完成 | `ProductionKanbanView.vue` 根容器已改为 `section-block`，并补齐标题说明结构。 |
| P1-006 | 排版工作台历史记录搜索框点击后应出现预选框 | 未完成 | `LayoutHistoryPanel.vue` 仅在 `@focus` 时调用 `loadRecords`，未形成真正的预选框/下拉候选；第二轮标注反馈预加载功能存在问题。 |

## 问题列表

### P1-003 顶部客户输入框候选列表功能需返工

- 优先级：P1
- 类型：前端/接口/体验
- 状态：部分完成/需返工
- 现象：`/cutting/data-input` 顶部"客户名称"已由普通输入框改为 `el-autocomplete`，但第二轮标注反馈"输入框没了，无法测试原问题是否还存在"。截图中客户区域视觉尺寸异常，用户无法确认点击后是否能弹出数据库候选列表。
- 期望：客户名称输入框应清晰可见，点击或聚焦后显示数据库客户候选列表，选择后回填输入框，不影响订单号、排单日期、操作人员、备注等同排字段布局。
- 相关文件：
  - `frontend/src/components/cutting/OrderInfoBar.vue`
  - `frontend/src/views/cutting/DataInputView.vue`
  - `frontend/src/api/customers.js`
- 相关代码位置：
  - `OrderInfoBar.vue:42`：客户候选 loading 状态。
  - `OrderInfoBar.vue:44`：`queryCustomerSuggestions` 查询逻辑。
  - `OrderInfoBar.vue:61`：客户名称表单项。
  - `OrderInfoBar.vue:62`：`el-autocomplete`。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/data-input`。
  2. 查看顶部"客户名称"表单项。
  3. 点击客户输入区域，确认输入框是否正常可见，以及是否弹出客户候选列表。
- 风险影响：客户选择入口不可用会阻断原问题验证，并影响加工数据录入的客户关联。
- 建议验证方式：运行 `cd frontend && npm run build`；在浏览器检查输入框可见性、聚焦下拉、输入筛选、选择回填、清空和空态。

### P1-004 原材料搜索预加载列表失焦后无法收回

- 优先级：P1
- 类型：前端/体验
- 状态：部分完成/需返工
- 现象：`/cutting/data-input` 左侧"原材料选择"搜索框聚焦后会预加载板材并显示"搜索结果"，但输入框失去焦点后列表仍然停留，无法自动收回。
- 期望：预加载候选应表现为可控的候选框/下拉区；失焦或点击页面其他区域后应收起，选择或添加板材时仍应保持必要的交互可用性。
- 相关文件：
  - `frontend/src/components/cutting/RawMaterialPanel.vue`
  - `frontend/src/api/boards.js`
- 相关代码位置：
  - `RawMaterialPanel.vue:37`：`onFocus` 预加载板材。
  - `RawMaterialPanel.vue:78`：板材搜索输入框。
  - `RawMaterialPanel.vue:93`：搜索结果区。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/data-input`。
  2. 点击"搜索板材（品牌/材质/颜色）"输入框，观察预加载结果出现。
  3. 点击右侧下料表或页面其他空白区域。
  4. 观察搜索结果仍然显示，无法收回。
- 风险影响：候选列表长期占用左侧面板空间，干扰已选板材、余料选择和下料录入。
- 建议验证方式：验证聚焦显示、失焦收起、点击候选/添加按钮不误关、清空输入、接口异常空态；运行 `cd frontend && npm run build`。

### P1-006 排版工作台历史搜索预加载功能存在问题

- 优先级：P1
- 类型：前端/体验/接口
- 状态：未完成
- 现象：`/cutting/layout-workbench` 左侧"历史排单记录"的搜索框聚焦后仅触发 `loadRecords`，页面仍展示常驻历史记录列表，没有出现明确的预选框/候选下拉；第二轮标注反馈"预加载功能存在问题"。
- 期望：点击或聚焦历史搜索框时，应显示清晰的预选候选框，可展示历史排单记录或客户/订单候选；候选可选择、可筛选，并在失焦后按预期收起。
- 相关文件：
  - `frontend/src/components/cutting/LayoutHistoryPanel.vue`
  - `frontend/src/views/cutting/LayoutWorkbenchView.vue`
  - `frontend/src/api/layout-results.js`
- 相关代码位置：
  - `LayoutHistoryPanel.vue:23`：`loadRecords`。
  - `LayoutHistoryPanel.vue:59`：历史搜索输入框。
  - `LayoutHistoryPanel.vue:65`：当前 `@focus="loadRecords"`。
  - `LayoutHistoryPanel.vue:74`：常驻历史记录列表。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/layout-workbench`。
  2. 点击左侧"搜索客户/订单名"输入框。
  3. 观察没有出现独立候选框，预加载交互不明确。
- 风险影响：用户无法高效发现和选择历史排单记录，原"点击后出现预选框"的需求未被真正满足。
- 建议验证方式：验证聚焦预加载、候选展示、输入过滤、选择记录、失焦收起、接口异常和空态；运行 `cd frontend && npm run build`。

### P1-007 加工数据输入页需要增加外部容器

- 优先级：P1
- 类型：前端/体验
- 状态：待处理
- 现象：`/cutting/data-input` 页面当前 `cutting-view` 直接放在 `content-panel` 中，整体缺少与管理页一致的外部容器。第二轮标注要求"同样增加容器"。
- 期望：加工数据输入页在现有生产型布局基础上增加统一外部容器，让顶部订单信息、左侧原材料/余料、右侧下料表和底部汇总形成完整页面块；容器样式应与其他管理页协调，同时不破坏满高录入体验。
- 相关文件：
  - `frontend/src/views/cutting/DataInputView.vue`
  - `frontend/src/styles/main.css`
- 相关代码位置：
  - `DataInputView.vue:95`：页面根节点 `cutting-view`。
  - `DataInputView.vue:106`：主工作区布局。
  - `main.css:304`：`section-block` 共享容器样式。
  - `main.css:638`：`cutting-view` 相关样式。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/data-input`。
  2. 对比客户管理、板材管理、生产看板页面的外部白色容器。
  3. 观察加工数据输入页整体缺少外部包裹。
- 风险影响：生产加工页面与管理页视觉层级不一致，页面显得散，且宽屏下内容边界不够明确。
- 建议验证方式：调整后检查 1366px、1920px 和当前 2126px 宽屏视口；确认容器、滚动、高度、底部汇总条和下料表没有被挤压；运行 `cd frontend && npm run build`。

### P1-008 排版工作台需要增加外部容器

- 优先级：P1
- 类型：前端/体验
- 状态：待处理
- 现象：`/cutting/layout-workbench` 页面当前 `cutting-view` 直接放在 `content-panel` 中，工具栏、历史记录面板和 Canvas 工作区缺少统一外部容器。第二轮标注要求"增加外部容器"。
- 期望：排版工作台应增加外部容器，包住工具栏、历史记录区域和排版 Canvas，让页面与生产看板、管理页的内容容器风格统一；同时保留工作台满高、Canvas 可视区域和左侧历史面板滚动。
- 相关文件：
  - `frontend/src/views/cutting/LayoutWorkbenchView.vue`
  - `frontend/src/styles/main.css`
- 相关代码位置：
  - `LayoutWorkbenchView.vue:256`：页面根节点 `cutting-view`。
  - `LayoutWorkbenchView.vue:257`：工具栏。
  - `LayoutWorkbenchView.vue:272`：工作台布局。
  - `main.css:304`：`section-block` 共享容器样式。
  - `main.css:645`：工作台布局样式。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/layout-workbench`。
  2. 对比生产看板或其他管理页的外部容器。
  3. 观察工具栏和工作区直接悬在内容面板中。
- 风险影响：工作台页面视觉边界弱，宽屏下不够完整；直接套容器若处理不当也可能影响 Canvas 高度和滚动。
- 建议验证方式：调整后检查当前 2126x1338、常见桌面和窄屏视口；确认 Canvas 非空、历史面板滚动、工具栏按钮和返回编辑按钮位置正常；运行 `cd frontend && npm run build`。

## 待确认点

- 客户名称候选列表应使用客户列表接口还是用户列表接口？当前代码使用 `listCustomers`，与字段名"客户名称"一致，但第一轮评论曾写"数据库中查询到的用户"。
- 历史排单记录的"预选框"候选内容应展示历史排版记录、订单记录、客户记录，还是混合搜索结果？
- 加工数据输入页和排版工作台页增加外部容器时，是否要求复用 `section-block`，还是新增更适合满高生产工作台的 `cutting-shell` 类容器？
