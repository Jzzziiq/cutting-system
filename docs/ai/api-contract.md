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
| 认证 | `/auth/login`、`/auth/register`、`/auth/logout` | 不需要 JWT | `Result` |
| 客户 | `/customers`、`/customers/{id}`、`/customers/batch`、`/customers/batch/status` | 需要 `Authorization: Bearer <token>` | `Result` |
| 板材 | `/boards`、`/boards/{id}`、`/boards/texture`、`/boards/batch`、`/boards/batch/status` | 需要 `Authorization: Bearer <token>` | `Result` |
| 订单 | `/orders`、`/orders/{id}`、`/orders/{id}/status` | 需要 `Authorization: Bearer <token>` | `Result` |
| 订单明细 | `/order-items`、`/order-items/{id}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 余料 | `/remnants`、`/remnants/{id}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 排样结果 | `/layout-results`、`/layout-results/{id}`、`/layout-results/order/{orderId}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 算法求解 | `POST /algorithm/answer` | 需要 `Authorization: Bearer <token>` | `List<SolutionResponseDTO>` |
| 算法异步 | `POST /algorithm/submit`、`GET /algorithm/result/{taskId}`、`POST /algorithm/compare`、`GET /algorithm/algorithms` | 需要 `Authorization: Bearer <token>` | `Result` |
| 生产任务 | `/production-tasks`、`/production-tasks/{id}`、`/production-tasks/kanban`、`/production-tasks/order/{orderId}` | 需要 `Authorization: Bearer <token>` | `Result` |

板材请求/响应字段包含可选 `textureUrl`，用于网页端 3D 柜体建模纹理贴图；`POST /boards/texture` 接收 `multipart/form-data` 的 `file`，上传到阿里云 OSS 后返回 `{ url }` 供板材保存时写入 `textureUrl`。OSS 配置从环境变量读取：`ALIYUN_OSS_ENDPOINT`、`ALIYUN_OSS_BUCKET`、`ALIYUN_OSS_ACCESS_KEY_ID`、`ALIYUN_OSS_ACCESS_KEY_SECRET`，可选 `ALIYUN_OSS_BOARD_TEXTURE_PREFIX`、`ALIYUN_OSS_PUBLIC_BASE_URL`。

删除保护：客户已被订单引用时，`DELETE /customers/{id}` 和 `/customers/batch` 返回业务错误；板材已被订单明细或余料引用时，`DELETE /boards/{id}` 和 `/boards/batch` 返回业务错误。前端应提示用户改为禁用，避免破坏历史订单数据。

## 认证规则

- `WebConfig` 拦截 `/**`，排除 `/`、`/index.html`、`/assets/**`、`/favicon.ico`、`/auth/*`。
- 受保护接口必须携带 `Authorization: Bearer <token>`。
- `TokenInterceptor` 验证 JWT 后将 `userId` 写入 `UserContext`，请求结束后清理上下文。

## 修改规则

- 不要无任务修改接口路径、请求字段、响应字段或认证要求。
- 新增或修改接口时，同步检查 Controller、Service、DTO/VO、MockMvc 测试、`frontend/src/api/`、`miniprogram/services/api.js` 和相关页面。
- 保持 `Result` 结构稳定；如果必须破坏兼容性，在交付说明和 `docs/ai/change-log.md` 中明确影响范围。
