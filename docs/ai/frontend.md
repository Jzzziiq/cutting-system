# 前端与小程序约定

## 网页端

- 源码目录：`frontend/`
- 技术栈：Vue 3、Vite、Pinia、Vue Router、Axios、Element Plus、Canvas 2D。
- UI 组件库：Element Plus，主题色 `#0f766e` teal，通过 `:root` CSS 变量覆盖；使用中文语言包。
- API 封装：`frontend/src/api/`，按后端模块拆分，如 `orders.js`、`boards.js`、`remnants.js`、`layout-results.js`、`algorithm.js`。
- 组合式函数：`frontend/src/composables/`，重点包括 `useCuttingTable`、`useLayoutCanvas`、`useAlgorithmSubmit`。
- 路由：`frontend/src/router/index.js`。
- 认证状态：`frontend/src/stores/auth.js`。
- 开发代理：`/api` -> `http://localhost:8080`。

## 核心页面

- 系统管理：登录、工作台、客户管理、板材管理、用户管理、审计日志。
- 生产加工：加工数据输入 `/cutting/data-input`、排版工作台 `/cutting/layout-workbench`、生产看板、我的生产订单 `/production/my-orders`。
- 板材管理可维护 `textureUrl`，也可上传 jpg/png/webp 纹理图片到阿里云 OSS 并自动回填 URL；新增/编辑表单的品牌、材质、规格类型、长度、宽度、厚度支持从历史候选值选择或手动输入；列表有勾选记录时只导出选中板材，未勾选时导出全部板材。3D 柜体建模在板材映射后优先使用目标板材纹理，缺省时回退颜色外观。
- 加工数据输入页：顶栏订单信息、左侧板材和余料选择、右侧 Excel 风格下料表格、底栏统计和确认排版；页面展示订单表主键 `orderId` 作为只读订单号，操作人员只读展示当前登录用户的 `realName` 或 `username`。
- 排版工作台页：工具栏、历史排版记录、中央 Canvas、板材切换标签和摘要栏；历史排版记录有关联订单时可分配生产任务给员工。
- 我的生产订单页：只读展示当前登录用户被分配的生产任务列表与订单详情，数据来自 `/production-tasks/my` 和 `/production-tasks/my/{taskId}`，不依赖前端筛选隐藏全量生产任务；排版工作台历史记录展示已分配员工，并在再次分配时回填原负责人。
- 3D 柜体设计页：在 `/cutting/cabinet-design?orderId=...` 下维护订单级柜体草稿；MVP4 已加入板件库拖放、画布内已有板件拖动、吸附对齐、选中板件尺寸/位置/封边微调、复制/删除/视角复位和撤销重做。新增自由板件仍使用 `materialSlot` 参与既有板材映射和拆单流程。

## 小程序端

- 源码目录：`miniprogram/`
- API 封装：`miniprogram/services/api.js`
- 统一请求：`miniprogram/utils/request.js`
- 后端地址：`miniprogram/utils/config.js`
- 页面范围：登录、客户、板材、算法输入与结果展示。

## 修改规则

- 后端接口路径、参数或返回字段变化时，网页端和小程序端必须同步检查。
- 登录成功后前端应保存 token，并在后续业务请求中携带 `Authorization: Bearer <token>`。
- 算法接口成功返回数组，业务接口成功返回 `Result`；前端错误处理要区分这两类结构。
- 网页端构建推荐命令：`cd frontend; npm run build`。
- 小程序预览需要微信开发者工具打开 `miniprogram/` 目录。
