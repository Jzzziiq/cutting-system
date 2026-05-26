# 前端与小程序约定

## 网页端

- 源码目录：`frontend/`
- 技术栈：Vue 3、Vite 7、Pinia 3、Vue Router 4、Axios、Element Plus、Canvas 2D、Three.js、ECharts。
- UI 组件库：Element Plus，主题色 `#0f766e` teal，通过 `:root` CSS 变量覆盖；使用中文语言包。
- API 封装：`frontend/src/api/`，按后端模块拆分，共 17 个文件。`http.js` 为 Axios 实例，自动解包 `Result` 信封。详见下方"API 模块清单"。
- 组合式函数：`frontend/src/composables/`，共 10 个。详见下方"组合式函数清单"。
- Store：`frontend/src/stores/auth.js`（认证状态）、`frontend/src/stores/cabinetDesign.js`（3D 柜体设计草稿，含撤销重做）。
- 路由：`frontend/src/router/index.js`。
- 开发代理：`/api` -> `http://localhost:8080`。

## 核心页面

- 系统管理：登录、工作台、客户管理、板材管理、用户管理、审计日志。
- 生产加工：加工数据输入 `/cutting/data-input`、排版工作台 `/cutting/layout-workbench`、生产看板 `/production-board`。
- 板材管理可维护 `textureUrl`，也可上传 jpg/png/webp 纹理图片到阿里云 OSS 并自动回填 URL；新增/编辑表单的品牌、材质、规格类型、长度、宽度、厚度支持从历史候选值选择或手动输入；列表有勾选记录时只导出选中板材，未勾选时导出全部板材。3D 柜体建模在板材映射后优先使用目标板材纹理，缺省时回退颜色外观。
- 加工数据输入页：顶栏订单信息、左侧板材和余料选择、右侧 Excel 风格下料表格、底栏统计和确认排版；页面展示订单表主键 `orderId` 作为只读订单号，操作人员只读展示当前登录用户的 `realName` 或 `username`。
- 排版工作台页：工具栏、历史排版记录、中央 Canvas、板材切换标签和摘要栏；历史排版记录有关联订单时可分配生产任务给员工。
- 我的生产订单页：只读展示当前登录用户被分配的生产任务列表与订单详情，数据来自 `/production-tasks/my` 和 `/production-tasks/my/{taskId}`，不依赖前端筛选隐藏全量生产任务；排版工作台历史记录展示已分配员工，并在再次分配时回填原负责人。
- 3D 柜体设计页：在 `/cutting/cabinet-design?orderId=...` 下维护订单级柜体草稿；MVP4 已加入板件库拖放、画布内已有板件拖动、吸附对齐、选中板件尺寸/位置/封边微调、复制/删除/视角复位和撤销重做。新增自由板件仍使用 `materialSlot` 参与既有板材映射和拆单流程。

## 小程序端

- 源码目录：`miniprogram/`
- API 封装：`miniprogram/services/api.js`（10 个函数，全部被页面调用）
- 统一请求：`miniprogram/utils/request.js`
- 后端地址：`miniprogram/utils/config.js`
- 页面范围：登录、我的任务（列表+详情）、个人设置、通知。

| 页面 | 调用的 API |
| --- | --- |
| `pages/login/login` | `login` |
| `pages/tasks/index` | `listMyTasks`、`getUnreadCount` |
| `pages/tasks/detail` | `getMyTaskDetail`、`transitionTask` |
| `pages/notifications/index` | `listNotifications`、`markNotificationRead`、`markAllNotificationsRead` |
| `pages/profile/index` | `getProfile`、`changePassword` |

## 修改规则

- 后端接口路径、参数或返回字段变化时，网页端和小程序端必须同步检查。
- 登录成功后前端应保存 token，并在后续业务请求中携带 `Authorization: Bearer <token>`。
- 算法接口成功返回数组，业务接口成功返回 `Result`；前端错误处理要区分这两类结构。
- 网页端构建推荐命令：`cd frontend; npm run build`。
- 小程序预览需要微信开发者工具打开 `miniprogram/` 目录。

## API 模块清单

标记说明：✔ = 前端已调用，✘ = 已封装但前端未调用，⚠ = 前端直接 http 调用未封装。

| 文件 | 函数数 | 已调用 | 未调用 |
| --- | --- | --- | --- |
| `http.js` | Axios 实例（自动解包 Result、401 跳登录） | — | — |
| `auth.js` | 3 | ✔ login、registerOrg、registerUser | — |
| `admin.js` | 1 | ✔ getAdminSummary | — |
| `users.js` | 5 | ✔ listUsers、getUser、updateUserStatus、assignRoles、listRoles | — |
| `audit-logs.js` | 2 | ✔ listAuditLogs、exportAuditLogs | — |
| `customers.js` | 10 | ✔ 全部 | — |
| `boards.js` | 12 | ✔ 全部 | — |
| `dashboard.js` | 3 | ✔ getSummary、getOrderTrend、getOrderStatusDist | — |
| `cabinet-templates.js` | 5 | ✔ listCabinetTemplates、createCabinetTemplate、updateCabinetTemplate、deleteCabinetTemplate | ✘ getCabinetTemplate |
| `order-split.js` | 2 | ✔ executeSplit、confirmSplit | — |
| `orders.js` | 9 | ✔ listOrders、getOrder、createOrder、getLayoutInput、saveLayoutInput | ✘ updateOrder、deleteOrder、transitionOrderStatus、getOrderStatusLabels |
| `order-items.js` | 5 | — | ✘ 全部 5 个 |
| `remnants.js` | 5 | ✔ listRemnants | ✘ getRemnant、createRemnant、updateRemnant、deleteRemnant |
| `layout-results.js` | 6 | ✔ listLayoutResults、getLayoutResult、createLayoutResult、deleteLayoutResult | ✘ getLayoutResultsByOrder、updateLayoutResult |
| `algorithm.js` | 4 | ✔ submitAlgorithm、getAlgorithmResult | ✘ getAlgorithms、compareAlgorithms |
| `production-tasks.js` | 10 | ✔ deleteTask、assignTask、assignOrderTask、kanbanData | ✘ listTasks、getTask、createTask、updateTask、transitionTask、listTasksByOrder |

直接 http 调用（未封装为 API 函数）：
- `ProfileView.vue`：`PUT /users/me/password`
- `OrgUsersView.vue`：`GET /users/pending`、`PUT /users/{id}/org-role`

## 组合式函数清单

| 文件 | 用途 |
| --- | --- |
| `useAlgorithmSubmit.js` | 算法提交与结果轮询 |
| `useCuttingTable.js` | 下料表格数据管理 |
| `useLayoutCanvas.js` | 排版 Canvas 绘制 |
| `useLayoutDataLoader.js` | 排版数据加载与保存 |
| `useLayoutRunner.js` | 排版执行流程控制 |
| `useBoardWorkpieceGroups.js` | 板材工件分组 |
| `useSlotMapping.js` | 材质槽位映射 |
| `useThreeScene.js` | Three.js 3D 场景管理 |
| `useCabinetGeometry.js` | 柜体几何体生成 |
| `useSceneInteraction.js` | 3D 场景交互（拖动、吸附） |
