# 数据模型

## 当前主要实体

- `TUser`：用户与登录认证，关联 `org_id`。
- `TRole`：RBAC 角色定义。
- `TPermission`：RBAC 权限码。
- `TUserRole`：用户-角色关联。
- `TRolePermission`：角色-权限关联。
- `TOrganization`：组织（多租户），`org_id` 为租户隔离键。
- `TCustomer`：客户信息。
- `TBoard`：板材基础信息，包含可选 `textureUrl` 纹理图片 URL。
- `TOrder`、`TOrderItem`：排单和待切割明细。
- `TOffcut`：余料信息。
- `TLayoutResult`：排样结果持久化。
- `TProductionTask`：生产任务，含 `assignee_id`（被分配员工）和 `user_id`（任务创建人）。
- `AlgorithmTask`：算法异步任务（submit/result 模式）。
- `TNotification`：用户通知。
- `TAuditLog`：审计日志。
- `CabinetTemplate`：柜体模板。
- `CabinetOrderItem`：柜体订单明细。
- `Instance`、`Square`、`Solution`、`PlaceSquare`、`PlacePoint`：算法输入、矩形、解和放置结果。

枚举：`OrderStatus`（订单状态机）、`TaskStatus`（生产任务状态）。
统一响应：`Result { code, msg, data }`（code 200=成功，0=错误，100=失败）。

## 修改规则

- 新增字段时，同步检查实体、DTO、VO、Mapper、XML、数据库脚本、前端 API、小程序页面和测试。
- 删除或重命名字段前先评估前端兼容性，不要让已有接口静默破坏。
- 表结构变更优先放入 `src/main/resources/db/migration/`，并在相关文档中说明执行顺序。
- `src/main/java/com/cutting/cuttingsystem/entitys/` 是既有包名；不要为了拼写修正而改包名。
- 数据库密码、JWT 密钥等生产配置不要写入仓库，应迁移到环境变量或外部配置。
