# API 契约

## 板材纹理上传

`POST /boards/texture` 接收 `multipart/form-data` 的 `file` 字段，支持 jpg/png/webp，返回 `{ url }` 供板材保存时写入 `textureUrl`。

上传优先使用阿里云 OSS：`ALIYUN_OSS_ENDPOINT`、`ALIYUN_OSS_BUCKET`、`ALIYUN_OSS_ACCESS_KEY_ID`、`ALIYUN_OSS_ACCESS_KEY_SECRET` 配置完整时写入 OSS；本地开发未配置 OSS 时，自动保存到 `LOCAL_UPLOAD_DIR`（默认 `uploads`）下的 `board-textures/`，并返回 `/uploads/board-textures/...`。`/uploads/**` 由后端静态资源映射直接托管。

## 返回约定

业务接口默认返回：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

例外：`POST /algorithm/answer` 当前直接返回 `List<SolutionResponseDTO>`。

## 接口地图

| 模块 | 接口 | 认证 | 返回约定 |
| --- | --- | --- | --- |
| 认证 | `/auth/login`、`/auth/register`、`/auth/register-org`、`/auth/register-user`、`/auth/logout` | 不需要 JWT | `Result` |
| 客户 | `/customers`、`/customers/{id}`、`/customers/batch`、`/customers/batch/status` | 需要 `Authorization: Bearer <token>` | `Result` |
| 板材 | `/boards`、`/boards/{id}`、`/boards/options`、`/boards/texture`、`/boards/batch`、`/boards/batch/status` | 需要 `Authorization: Bearer <token>` | `Result` |
| 订单 | `/orders`、`/orders/{id}`、`/orders/{id}/status`、`/orders/{id}/layout-input`、`/orders/status-labels` | 需要 `Authorization: Bearer <token>` | `Result` |
| 订单明细 | `/order-items`、`/order-items/{id}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 余料 | `/remnants`、`/remnants/{id}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 排样结果 | `/layout-results`、`/layout-results/{id}`、`/layout-results/order/{orderId}`、`/layout-results/{id}/nc` | 需要 `Authorization: Bearer <token>` | `Result` |
| 算法求解 | `POST /algorithm/answer` | 需要 `Authorization: Bearer <token>` | `List<SolutionResponseDTO>` |
| 算法异步 | `POST /algorithm/submit`、`GET /algorithm/result/{taskId}`、`POST /algorithm/compare`、`GET /algorithm/algorithms` | 需要 `Authorization: Bearer <token>` | `Result` |
| 生产任务 | `/production-tasks`、`/production-tasks/{id}`、`/production-tasks/{id}/status`、`/production-tasks/{id}/assign`、`/production-tasks/{id}` DELETE、`/production-tasks/kanban`、`/production-tasks/order/{orderId}`、`/production-tasks/order/{orderId}/assign`、`/production-tasks/my`、`/production-tasks/my/{taskId}`、`/production-tasks/my/{id}/status` | 需要 `Authorization: Bearer <token>` | `Result` |
| 用户 | `/users`、`/users/{id}`、`/users/{id}/status`、`/users/roles`、`/users/create-in-org`、`/users/me`、`/users/me/password`、`/users/pending`、`/users/{id}/org-role` | 需要 `Authorization: Bearer <token>` | `Result` |
| 组织 | `/organizations`、`/organizations/{id}`、`/organizations/{id}/status` | 需要 `Authorization: Bearer <token>` | `Result` |
| 仪表盘 | `/dashboard/summary`、`/dashboard/order-trend`、`/dashboard/order-status-dist`、`/dashboard/utilization-trend` | 需要 `Authorization: Bearer <token>` | `Result` |
| 管理后台 | `/admin/dashboard` | 需要 `Authorization: Bearer <token>` | `Result` |
| 通知 | `/notifications`、`/notifications/{id}/read`、`/notifications/read-all`、`/notifications/unread-count` | 需要 `Authorization: Bearer <token>` | `Result` |
| 审计日志 | `/audit-logs`、`/audit-logs/export` | 需要 `Authorization: Bearer <token>` | `Result` |
| 订单拆单 | `/order-split/execute`、`/order-split/confirm` | 需要 `Authorization: Bearer <token>` | `Result` |
| 柜体模板 | `/cabinet-templates`、`/cabinet-templates/{id}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 导入 | `/customers/import`、`/boards/import`、`/customers/template`、`/boards/template` | 需要 `Authorization: Bearer <token>` | `Result` |
| 导出 | `/customers/export`、`/boards/export` | 需要 `Authorization: Bearer <token>` | Excel 文件 |

板材请求/响应字段包含可选 `textureUrl`，用于网页端 3D 柜体建模纹理贴图；`POST /boards/texture` 接收 `multipart/form-data` 的 `file`，上传到阿里云 OSS 后返回 `{ url }` 供板材保存时写入 `textureUrl`。OSS 配置从环境变量读取：`ALIYUN_OSS_ENDPOINT`、`ALIYUN_OSS_BUCKET`、`ALIYUN_OSS_ACCESS_KEY_ID`、`ALIYUN_OSS_ACCESS_KEY_SECRET`，可选 `ALIYUN_OSS_BOARD_TEXTURE_PREFIX`、`ALIYUN_OSS_PUBLIC_BASE_URL`。

`GET /boards/options` 返回板材表中 `brand`、`materialType`、`sizeType`、`length`、`width`、`thickness` 的去重候选值，按出现次数降序排列，用于网页端新增/编辑表单的可输入下拉候选。

`GET /boards/export` 可携带可选查询参数 `ids=1,2,3` 导出指定板材；未传 `ids` 时保持导出全部板材。

网页端加工数据输入页只读展示 `orderId` 作为订单号；后端 `TOrder.orderNo` 字段、`/orders` 请求/响应结构和自动编号兼容逻辑保持不变，其他页面仍可按需使用 `orderNo`。

生产分配：`PUT /production-tasks/order/{orderId}/assign` 接收 `{ "assigneeId": 1 }`，后端按用户表查询负责人姓名并写入最新生产任务；订单暂无生产任务时自动创建待生产任务并关联订单当前 `layoutResultId`。`GET /production-tasks/my` 和 `/production-tasks/my/{taskId}` 只返回当前登录用户被分配的生产任务，详情包含任务、订单与排版结果摘要；这些自助查询按 `assignee_id` 做业务过滤，不受订单创建人 `user_id` 租户条件误挡。员工自助状态变更：`PUT /production-tasks/my/{id}/status` 接收 `{ "status": 1 }`（开始）或 `{ "status": 2 }`（完成），仅允许操作自己的任务。排版历史列表的 `TLayoutResultVO` 可携带最新生产任务的 `assigneeId`、`assigneeName`，用于展示与再次分配时回填。生产看板、任务创建/编辑/删除/分配/状态变更要求 `order:write`；我的生产订单接口要求 `order:read`。

跨账号业务记录访问规则：系统启用了基于 `user_id` 的 MyBatis-Plus 租户过滤，但部分业务操作会合法涉及不同账号下的记录，例如管理员创建订单后分配给员工、员工查看被分配任务、按订单回显任务负责人。遇到这类链路时，不能只依赖当前登录用户的 `user_id` 自动条件；必须先明确业务访问关系，使用显式业务条件读取目标记录，并同时保留权限校验。

生产任务租户过滤注意：`t_production_task.user_id` 表示任务创建/订单归属用户，`assignee_id` 表示被分配生产员工。曾出现的 bug 是员工端 `/production-tasks/my` 使用普通 MyBatis-Plus 查询时被自动追加 `user_id = 当前员工ID`，导致管理端创建并分配给员工的任务虽然 `assignee_id` 正确写入，但员工端列表被租户条件过滤为空。凡是“按负责人查看任务”“按订单回显最新分配人”“员工查看自己任务详情”的链路，都必须使用显式 `assignee_id`/`order_id` 业务条件，并在必要处通过 mapper 的 `@InterceptorIgnore(tenantLine = "true")` 避开创建人 `user_id` 过滤；权限边界仍由接口权限和 `assignee_id = UserContext.userId` 校验保证。后续其他模块只要存在“记录归属账号”和“业务操作账号”不同的情况，也要按同一规则检查读取与修改链路。

NC 文件导出：`GET /layout-results/{id}/nc` 返回 G-code 文件供 CNC 设备使用。

用户自助：`GET /users/me` 返回当前登录用户信息；`PUT /users/me/password` 修改密码，接收 `{ "oldPassword", "newPassword" }`。

删除保护：客户已被订单引用时，`DELETE /customers/{id}` 和 `/customers/batch` 返回业务错误；板材已被订单明细或余料引用时，`DELETE /boards/{id}` 和 `/boards/batch` 返回业务错误。前端应提示用户改为禁用，避免破坏历史订单数据。

## 认证规则

- `WebConfig` 拦截 `/**`，排除 `/`、`/index.html`、`/assets/**`、`/favicon.ico`、`/auth/*`。
- 受保护接口必须携带 `Authorization: Bearer <token>`。
- `TokenInterceptor` 验证 JWT 后将 `userId`、`orgId`、`roles`、`permissions` 写入 `UserContext`，请求结束后清理上下文。
- `@RequirePermission` 注解强制 RBAC 权限校验；无注解的接口默认放行（仅需有效 JWT）。
- 注册流程：`/auth/register-org` 创建组织 + 管理员账号；`/auth/register-user` 注册用户加入组织（需管理员审批）。

## 修改规则

- 不要无任务修改接口路径、请求字段、响应字段或认证要求。
- 新增或修改接口时，同步检查 Controller、Service、DTO/VO、MockMvc 测试、`frontend/src/api/`、`miniprogram/services/api.js` 和相关页面。
- 保持 `Result` 结构稳定；如果必须破坏兼容性，在交付说明和 `docs/ai/change-log.md` 中明确影响范围。
