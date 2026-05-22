# 前端页面逻辑优化方案

## Context

用户反馈"加工数据输入"和"排版工作台"两个页面逻辑杂糅。经过全面分析前端代码，发现以下核心问题：

1. **CabinetDesignView.vue（2124行）** 是整个前端最大的文件，单文件组件零子组件拆分，60% 的业务逻辑内联在 view 中
2. **DataInputView.vue（355行）** 的 `onConfirm` 方法（~85行）混合了验证、算法提交、结果解析、sessionStorage 存储、路由导航五种职责；且已存在 `useAlgorithmSubmit.js` composable 却未使用
3. **useCuttingTable.js 和 useBoardWorkpieceGroups.js** 存在大量重复代码（验证、键盘导航、粘贴处理、算法输入构建）
4. **LayoutWorkbenchView.vue（624行）** 三条数据加载路径和排版编排逻辑全部内联

---

## 优化方案（按优先级排序）

### 第一优先级：拆分 CabinetDesignView.vue（2124 → ~400行）

**问题**：单文件 2124 行，零子组件，所有 UI（左侧栏、3D 画布、右侧检查器、多个弹窗）和业务逻辑（柜体生成、吸附对齐、材质槽映射、模板 CRUD、拆单流程）全部内联。

**拆分策略**：

| 新组件/模块 | 来源（行范围） | 预估行数 | 职责 |
|---|---|---|---|
| `components/cabinet/CabinetLeftSidebar.vue` | template: 1215-1320, script: 预设/草稿/部件库逻辑 | ~180 | 预设列表、草稿管理、部件拖拽库 |
| `components/cabinet/BoardInspectorPanel.vue` | template: 1394-1546, script: 827-874 | ~200 | 右侧板材属性编辑面板 |
| `components/cabinet/CabinetWizardDialog.vue` | template: 1548-1600, script: 928-971 | ~150 | 新建柜体向导弹窗 |
| `components/cabinet/SlotMapDialog.vue` | template: 1602-1635, script: 973-1077 | ~180 | 材质槽映射弹窗 |
| `components/cabinet/SplitPreviewDialog.vue` | template: split preview 区域, script: 1088-1135 | ~120 | 拆单预览/确认弹窗 |
| `composables/useCabinetGeometry.js` | script: 391-552 (generateCabinetJson) + 642-682 (createFreeAssemblyBoard) | ~220 | 柜体几何计算（纯函数） |
| `composables/useSceneInteraction.js` | script: 578-630 (snap) + 707-825 (drag/drop/pointer) | ~180 | 场景交互：拖放、吸附、指针拖拽 |
| `composables/useSlotMapping.js` | script: 973-1077 | ~120 | 材质槽映射状态管理 |
| `constants/cabinet.js` | script: 57-212 | ~160 | 静态常量：部件定义、颜色板、类型标签等 |

**拆分后 CabinetDesignView.vue** 变为纯编排层：引入子组件、组合 composable、处理事件冒泡，预估 ~400 行。

---

### 第二优先级：LayoutWorkbenchView 提取数据加载与排版编排（624 → ~350行）

**问题**：624 行中有 ~540 行 script，包含三条独立的数据加载路径（order/task/draft）、排版编排逻辑（`runLayoutForGroups` 按组提交算法）、结果装饰（`decorateSolutions` 关联工件信息）、以及导出/保存功能，全部内联在 view 中。

**方案**：

| 新模块 | 提取内容 | 预估行数 | 职责 |
|---|---|---|---|
| `composables/useLayoutRunner.js` | `runLayoutForGroups` + `buildSquareList` + `decorateSolutions` + `summarizeBoardResults`（lines 54-154） | ~120 | 排版编排：按板材组提交算法、装饰结果、汇总统计 |
| `composables/useLayoutDataLoader.js` | `loadFromOrder` + `loadFromTask` + `loadFromDraft` + `loadFromRoute` + `buildOrderInfo`（lines 156-532） | ~180 | 数据加载编排：三条路径的加载逻辑、route 监听 |
| `utils/exportUtils.js` | `onExportToolpath` + `onExportFile`（lines 434-468） | ~50 | SVG/JSON 导出工具函数 |

**拆分后 LayoutWorkbenchView.vue** 变为：引入子组件和 composable、处理 UI 事件（缩放/设置/返回编辑）、模板，预估 ~350 行。

**涉及文件**：
- `frontend/src/views/cutting/LayoutWorkbenchView.vue`
- `frontend/src/composables/useAlgorithmSubmit.js`（useLayoutRunner 内部使用）

---

### 第三优先级：DataInputView 使用已有 useAlgorithmSubmit

**问题**：`onConfirm`（~85行）内联了算法提交+轮询+结果解析+存储+导航，而 `composables/useAlgorithmSubmit.js`（110行）已实现提交+轮询功能但未被使用。

**方案**：
- `DataInputView.vue` 的 `onConfirm` 改用 `useAlgorithmSubmit()` 的 `submit` 方法
- 将结果解析、draft 存储、导航逻辑拆为独立函数
- `onConfirm` 从 ~85行缩减到 ~30行

**涉及文件**：
- `frontend/src/views/cutting/DataInputView.vue`
- `frontend/src/composables/useAlgorithmSubmit.js`（只读引用）

---

### 第四优先级：消除 useCuttingTable / useBoardWorkpieceGroups 重复

**问题**：两个 composable 共 ~612 行，其中验证逻辑（长度上限3000、宽度上限1500、板材溢出检查）、键盘导航（Tab/Enter/Arrow 处理）、粘贴解析（tab 分隔数据）高度重复。

**方案**：
- 提取 `composables/useTableKeyboardNav.js` —— 通用的表格键盘导航逻辑
- 提取 `composables/useClipboardPaste.js` —— 通用的粘贴数据解析
- 提取 `utils/validation.js` —— 工件尺寸校验规则（长度/宽度/数量/溢出）
- 两个 composable 各缩减 ~80-100行

---

### 第五优先级：其他小优化

| 问题 | 方案 | 涉及文件 |
|---|---|---|
| `boardLabel`/`dimLabel` 在 RawMaterialPanel 和 BoardGroupTable 中重复 | 提取到 `utils/boardLabel.js` | 3 个文件 |
| DataInputView 中 `selectedOffcuts` 声明但未使用 | 删除或接入算法流程 | DataInputView.vue |
| useBoardWorkpieceGroups 模块级 `nextGroupId`/`nextItemId` 计数器 | 移入 composable 函数作用域 | useBoardWorkpieceGroups.js |

---

## 验证方式

1. `cd frontend && npm run build` —— 确认无编译错误
2. `npm run dev` —— 启动开发服务器
3. 逐页面手动验证：
   - `/cutting/data-input`：选择订单 → 添加板材 → 输入工件 → 保存 → 确认提交算法 → 跳转排版工作台
   - `/cutting/cabinet-design`：新建柜体向导 → 3D 场景拖放板材 → 编辑属性 → 材质槽映射 → 拆单预览 → 确认拆单
   - `/cutting/layout-workbench`：加载订单排版 → 查看结果 → 导出 → 保存
4. 功能回归：确保所有页面间的数据流（sessionStorage draft、route query）正常工作

## 执行顺序

建议按优先级分批执行，每批完成后验证再继续：
1. 第一优先级：CabinetDesignView 拆分（最大改善，2124 → ~400行）
2. 第二优先级：LayoutWorkbenchView 提取（624 → ~350行）
3. 第三优先级：DataInputView 优化（改动较小，~85行 → ~30行）
4. 第四、五优先级：composable 去重 + 小修
