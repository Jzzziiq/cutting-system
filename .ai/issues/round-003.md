# 问题记录

## 背景

- 来源：浏览器页面标注评论，累计三轮标注，最新标注时间 2026-05-11。
- 涉及模块：网页端加工数据输入页、排版工作台页、生产看板页，以及员工/用户、板材、排样结果相关接口契约。
- 涉及文件：
  - `frontend/src/views/cutting/DataInputView.vue`
  - `frontend/src/views/cutting/LayoutWorkbenchView.vue`
  - `frontend/src/views/ProductionKanbanView.vue`
  - `frontend/src/components/cutting/OrderInfoBar.vue`
  - `frontend/src/components/cutting/RawMaterialPanel.vue`
  - `frontend/src/components/cutting/CuttingTable.vue`
  - `frontend/src/components/cutting/LayoutHistoryPanel.vue`
  - `frontend/src/composables/useCuttingTable.js`
  - `frontend/src/api/users.js`
  - `frontend/src/api/boards.js`
  - `frontend/src/api/layout-results.js`
  - `frontend/src/api/production-tasks.js`
  - `frontend/src/styles/main.css`
  - `src/main/java/com/cutting/cuttingsystem/controller/UserController.java`
  - `src/main/java/com/cutting/cuttingsystem/controller/LayoutResultController.java`
  - `src/main/java/com/cutting/cuttingsystem/entitys/DTO/TLayoutResultDTO.java`
  - `src/main/java/com/cutting/cuttingsystem/entitys/VO/TLayoutResultVO.java`

## 旧问题完成状态

| 编号 | 原问题 | 当前状态 | 判定依据 |
| --- | --- | --- | --- |
| P1-001 | 下料尺寸表格在宽屏下未占满父容器 | 已完成 | `CuttingTable.vue` 已将列宽从 `width` 改为 `min-width`，截图中下料表可铺满右侧区域。 |
| P1-002 | 下料尺寸表头字段需要居中显示 | 已完成 | `CuttingTable.vue` 已新增 `.cutting-table .el-table__header-wrapper .cell { text-align: center; }`。 |
| P1-003 | 顶部客户输入框点击后应弹出数据库候选列表 | 已完成/待回归验证 | `OrderInfoBar.vue` 已改为远程 `el-select`，接入 `listCustomers` 并在聚焦时查询；最新截图中客户输入框已恢复可见。 |
| P1-004 | 原材料搜索输入框点击后应弹出数据库候选列表，失焦后可收回 | 已完成/待回归验证 | `RawMaterialPanel.vue` 已新增 `searchFocused`、`onBlur` 和条件渲染，理论上可在失焦且无关键字时收起结果。 |
| P1-005 | 生产看板页缺少与其他管理页一致的容器包裹 | 已完成 | `ProductionKanbanView.vue` 根容器已改为 `section-block`，并补齐标题说明结构。 |
| P1-006 | 排版工作台历史记录搜索框点击后应出现预选框 | 部分完成/需返工 | `LayoutHistoryPanel.vue` 已改为 `el-autocomplete`，但最新截图反馈“历史排单的搜索栏没了”，需要修复可见性和预选交互。 |
| P1-007 | 加工数据输入页需要增加外部容器 | 已完成 | `DataInputView.vue` 已增加 `cutting-shell` 外层容器；`main.css` 已新增 `.cutting-shell` 样式。 |
| P1-008 | 排版工作台需要增加外部容器 | 已完成 | `LayoutWorkbenchView.vue` 已增加 `cutting-shell` 外层容器；`main.css` 已新增对应样式。 |

## 问题列表

### P1-006 排版工作台历史搜索预选框需返工

- 优先级：P1
- 类型：前端/体验/接口
- 状态：部分完成/需返工
- 现象：`/cutting/layout-workbench` 的 `LayoutHistoryPanel.vue` 代码中已有 `el-autocomplete` 搜索框，但最新截图中左侧“历史排单记录”面板只显示历史卡片，搜索栏不可见。
- 期望：历史排单面板顶部应稳定显示搜索栏；点击搜索栏时出现历史排版记录或客户/订单候选；候选可筛选、可选择，并且不影响常驻历史列表展示。
- 相关文件：
  - `frontend/src/components/cutting/LayoutHistoryPanel.vue`
  - `frontend/src/views/cutting/LayoutWorkbenchView.vue`
  - `frontend/src/api/layout-results.js`
- 相关代码位置：
  - `LayoutHistoryPanel.vue:39`：`querySearchSuggestions` 候选查询。
  - `LayoutHistoryPanel.vue:80`：搜索栏容器。
  - `LayoutHistoryPanel.vue:81`：`el-autocomplete` 搜索框。
  - `LayoutHistoryPanel.vue:98`：历史记录列表。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/layout-workbench`。
  2. 查看左侧“历史排单记录”面板顶部。
  3. 观察搜索栏没有显示，无法测试预加载和筛选。
- 风险影响：历史排单无法搜索，用户只能浏览固定列表，历史记录较多时难以定位订单或客户。
- 建议验证方式：运行 `cd frontend && npm run build`；浏览器检查搜索栏可见性、聚焦候选、输入筛选、选择记录、清空恢复列表。

### P1-009 操作人员也需要员工候选列表，并全局排查选择型字段

- 优先级：P1
- 类型：前端/接口/数据模型/体验
- 状态：待处理
- 现象：`/cutting/data-input` 顶部“操作人员”仍是普通 `el-input`，没有像“客户名称”一样加载数据库中的员工/用户列表。最新标注要求类似需要选择的地方都提供列表预选框；如果数据库没有员工表，则需要评估调整相关表结构或复用现有用户表。
- 期望：操作人员字段应聚焦后加载员工候选，支持输入筛选和选择回填；同时全局排查类似选择型字段，例如生产看板的分配工人、订单 ID、排样结果 ID、导入排单等，优先使用下拉/自动完成而不是手填 ID 或自由文本。
- 相关文件：
  - `frontend/src/components/cutting/OrderInfoBar.vue`
  - `frontend/src/views/ProductionKanbanView.vue`
  - `frontend/src/api/users.js`
  - `src/main/java/com/cutting/cuttingsystem/controller/UserController.java`
  - 可能涉及员工/用户相关实体和数据库表。
- 相关代码位置：
  - `OrderInfoBar.vue:94`：操作人员表单项。
  - `OrderInfoBar.vue:95`：当前普通 `el-input`。
  - `ProductionKanbanView.vue:243`：分配工人表单。
  - `ProductionKanbanView.vue:247`：新建/编辑任务中的订单 ID、排样结果 ID。
  - `UserController.java:34`：已有用户分页接口。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/data-input`。
  2. 点击“操作人员”输入框。
  3. 观察没有员工候选列表，只能手动输入。
- 风险影响：操作人员、工人、订单、排样结果等字段手填易错，后续数据关联不稳定，生产任务分配体验也显得粗糙。
- 建议验证方式：确认员工数据来源后，验证候选加载、筛选、回填、清空、无数据提示；全局用关键词 `input`、`assignee`、`operator`、`orderId`、`layoutResultId` 排查需预选的字段；运行 `cd frontend && npm run build`。

### P1-010 下料表板材类型无法从已选板材中填入

- 优先级：P1
- 类型：前端/体验/Bug
- 状态：待处理
- 现象：`/cutting/data-input` 下料尺寸表中的“板材类型”列使用 `el-select`，但最新标注反馈点击板材列表中的表格后无法填入。当前表格选项只来自左侧 `selectedBoards`，且选择板材后不会自动回填该行的材质、颜色、长宽等字段。
- 期望：用户在左侧板材列表中添加板材后，右侧“板材类型”下拉应立即可选；选择某块板材后，应根据板材数据回填或联动当前行的材质、颜色等相关字段；如果没有已选板材，应给出清晰提示。
- 相关文件：
  - `frontend/src/components/cutting/RawMaterialPanel.vue`
  - `frontend/src/components/cutting/CuttingTable.vue`
  - `frontend/src/views/cutting/DataInputView.vue`
  - `frontend/src/composables/useCuttingTable.js`
- 相关代码位置：
  - `RawMaterialPanel.vue:63`：`addBoard` 添加已选板材。
  - `CuttingTable.vue:85`：板材类型 `el-select`。
  - `CuttingTable.vue:95`：板材选项来自 `boardOptions`。
  - `DataInputView.vue:120`：传入 `selectedBoards` 作为表格板材选项。
  - `useCuttingTable.js:73`：通过 `row.boardType` 查找已选板材。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/data-input`。
  2. 在左侧原材料列表中点击添加某块板材。
  3. 点击右侧下料表“板材类型”的“选择板材”下拉。
  4. 观察无法正确填入或无法联动当前行数据。
- 风险影响：板材选择是下料计算入口之一，无法填入会阻断排版确认和校验，也会让用户误以为原材料选择未生效。
- 建议验证方式：验证添加板材后下拉选项刷新、选择后 `row.boardType` 变化、字段联动、删除已选板材后的行数据处理；运行 `cd frontend && npm run build`。

### P1-011 保存后的排版结果没有进入左侧历史排单

- 优先级：P1
- 类型：前端/后端/接口/体验
- 状态：待处理
- 现象：在 `/cutting/layout-workbench` 完成排版或保存结果后，左侧“历史排单记录”没有出现新排单。最新标注还要求历史标题显示订单号，可通过客户筛选，默认按创建日期降序排序。
- 期望：排版完成并保存后，结果应写入 `layout-results` 并刷新左侧历史；历史卡片标题应优先显示订单号；搜索支持客户筛选；列表默认按 `create_time` 降序，最新记录在最上方。
- 相关文件：
  - `frontend/src/views/cutting/LayoutWorkbenchView.vue`
  - `frontend/src/components/cutting/LayoutHistoryPanel.vue`
  - `frontend/src/api/layout-results.js`
  - `src/main/java/com/cutting/cuttingsystem/controller/LayoutResultController.java`
  - `src/main/java/com/cutting/cuttingsystem/entitys/DTO/TLayoutResultDTO.java`
  - `src/main/java/com/cutting/cuttingsystem/entitys/VO/TLayoutResultVO.java`
- 相关代码位置：
  - `LayoutWorkbenchView.vue:216`：`onSaveResult` 保存排版结果。
  - `LayoutWorkbenchView.vue:232`：调用 `createLayoutResult`。
  - `LayoutHistoryPanel.vue:23`：加载历史记录。
  - `LayoutHistoryPanel.vue:106`：历史标题使用 `rec.orderName || 排版 #id`。
  - `LayoutResultController.java:38`：分页查询未显式排序，也未关联订单号/客户。
  - `TLayoutResultDTO.java:16`：`orderId` 必填，前端当前可能传 `null`。
  - `TLayoutResultVO.java:10`：VO 未包含订单号和客户名称字段。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/cutting/layout-workbench`。
  2. 点击“开始排版”，完成排版后点击“保存结果”。
  3. 查看左侧“历史排单记录”是否新增当前排版。
  4. 检查历史标题是否为订单号、是否能按客户筛选、是否按创建日期倒序。
- 风险影响：排版结果无法沉淀到历史列表，会让保存按钮失去可见反馈；历史记录标题和筛选不满足业务查找习惯。
- 建议验证方式：补齐接口排序和 VO 字段后，用真实订单排版保存；验证保存接口成功、历史列表自动刷新、最新记录置顶、标题显示订单号、客户筛选可用；必要时补 MockMvc 测试。

### P1-012 生产看板三列卡片和任务操作样式过于随意

- 优先级：P1
- 类型：前端/体验
- 状态：待处理
- 现象：生产看板三个状态列和任务卡片仍使用较基础的样式；新建任务、开始/完成、分配、删除等操作呈现为普通小按钮或原生 `confirm`，任务创建和后续操作显得随意。
- 期望：生产看板三列应有更清晰的生产状态视觉层级；任务卡片应展示订单、负责人、工时、状态等信息的结构化布局；新建任务、分配工人、状态流转和删除应使用统一、克制的 Element Plus 交互和确认反馈，而不是零散按钮和浏览器原生确认。
- 相关文件：
  - `frontend/src/views/ProductionKanbanView.vue`
  - `frontend/src/styles/main.css`
  - `frontend/src/api/production-tasks.js`
- 相关代码位置：
  - `ProductionKanbanView.vue:184`：生产看板容器。
  - `ProductionKanbanView.vue:194`：三列看板。
  - `ProductionKanbanView.vue:206`：任务卡片。
  - `ProductionKanbanView.vue:222`：任务操作按钮组。
  - `ProductionKanbanView.vue:236`：新建/编辑/分配弹窗。
  - `main.css:546`：看板布局和卡片样式。
- 复现步骤：
  1. 进入 `http://127.0.0.1:5173/production-board`。
  2. 查看三列看板、任务卡片和按钮布局。
  3. 点击新建任务、分配、开始/完成、删除，观察表单和反馈是否仍偏随意。
- 风险影响：生产看板是生产调度主界面，视觉和操作粗糙会降低可信度，也容易造成误操作或任务状态流转不清。
- 建议验证方式：重做后检查三列视觉层级、卡片信息密度、按钮状态、弹窗表单、删除确认、拖拽状态流转；运行 `cd frontend && npm run build` 并浏览器回归。

## 待确认点

- “员工列表”的数据源应复用现有 `TUser`/`/users`，还是新增员工表或员工角色过滤？如果新增/调整数据模型，需要同步后端、前端、小程序和 AGENTS 变更记录。答复：员工角色过滤（员工角色仅登录小程序，然后查看到自己需要负责的订单类似这种）
- 下料表选择板材后是否只填入“板材类型”，还是也要自动回填材质、颜色、默认长宽或订单/客户字段？答复：这一块的使用板材逻辑需要修改，因为订单规模变大时，每个工件都需要选择板材很麻烦，我想的是在下料表中，先添加使用的板材然后在使用的板材上添加目标工件相关信息。
- 历史排单的客户筛选应由后端 `/layout-results` 支持 `customer`/`search` 参数，还是前端基于已加载数据本地过滤？答复：基于已加载数据进行过滤
- 保存排版结果时，若没有订单 ID，是否允许创建临时排版记录，还是必须先导入/绑定订单？答复：这一块先留着不动

## 给执行者的说明

- 请先读取 `F:\Code\Java\cutting-system\AGENTS.md` 和 `F:\Code\Java\cutting-system\CLAUDE.md`。
- 请以本文档为唯一问题清单，逐项分析、修复和验证。
- 本文档已保留旧问题完成状态；执行时优先处理状态为“部分完成/需返工”“待处理”的条目。
