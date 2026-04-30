# 项目说明

agent_instruction: true  
read_before_work: true  
scope: entire_repository  

## Codex 启动约定

- 每次 Codex 在本仓库开始分析、修改、测试或文档维护前，应先读取本文件。
- 本文件是项目级上下文入口；当用户请求与代码、接口、数据模型、算法、依赖或文档有关时，应优先遵循本文档中的约定。
- 如果本文件与用户当前明确指令冲突，以用户当前指令为准；如果本文件与局部代码事实冲突，应先读取代码并在回复中说明差异。
- 修改项目后，如涉及新增 API、修改数据模型、调整算法逻辑或引入新依赖，应同步更新本文档的相关章节和“变更记录”。

project_name: cutting-system  
project_type: 柜门板材切割排版系统  
primary_language: Java  
backend_framework: Spring Boot 3.x  
current_backend_version: Spring Boot 3.5.11  
java_version: 17  
database: MySQL 8.0+  
orm: MyBatis-Plus 3.5.x  
auth: JWT + Spring MVC Interceptor  
frontend_web: Vue 3 + Vite + Pinia + Vue Router + Axios  
frontend_miniprogram: 微信小程序原生框架  
algorithm_entry: `POST /algorithm/answer`  
algorithm_strategy: 禁忌搜索 + 天际线放置算法  
last_updated: 2026-04-30  

本项目是基于 Spring Boot 的柜门板材切割排版系统。后端负责用户认证、客户管理、板材管理、订单/余料/排样结果管理、算法接口与静态资源托管。网页端用于后台操作和排样结果可视化，小程序端用于现场录入、客户管理、板材管理、算法输入和排样结果展示。

## 开发要求

1. 不要一次性重构大范围代码，优先做小步、明确、可验证的修改。
2. 每次只完成一个明确任务；如果任务跨后端、网页端和小程序端，先确认接口契约再分层修改。
3. 修改后必须补充或运行相关测试；无法运行时，在交付说明中写明原因和风险。
4. 保持接口返回结构稳定。除 `POST /algorithm/answer` 当前直接返回数组外，业务接口默认使用 `Result` 结构：`code`、`msg`、`data`。
5. 中文注释保持简洁，只解释业务约束、算法意图或非显然逻辑，避免无意义注释。
6. 代码修改后说明改了哪些文件、为什么改、如何测试。
7. 不要随意修改已有包名、表字段名、接口路径和 DTO/VO 字段名；确需调整时同步修改后端、网页端、小程序端、测试和本文档。
8. 新增 API、修改数据模型、调整算法逻辑或引入新依赖时，必须及时更新本文档，并在“变更记录”中标注变更日期、变更类型、原因、影响范围和验证方式。
9. 不要提交本地运行产物、日志和敏感配置；数据库密码、JWT 密钥等生产配置应迁移到环境变量或外部配置。
10. 修改算法时优先补充 `src/test/java/com/cutting/cuttingsystem/model/AlgorithmUnitTest.java`；修改接口时优先补充 MockMvc 测试。

## 常用命令

后端测试：

```powershell
mvn test
```

如需隔离 Maven 本地仓库缓存：

```powershell
mvn "-Dmaven.repo.local=target\.m2" test
```

后端启动：

```powershell
mvn spring-boot:run
```

本地同时启动后端和网页端：

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1
```

停止本地开发服务：

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\stop-dev.ps1
```

网页端开发：

```powershell
cd frontend
npm install
npm run dev
```

网页端构建：

```powershell
cd frontend
npm run build
```

小程序端构建与预览：

```text
使用微信开发者工具打开 miniprogram/ 目录，点击“预览”并扫码。
```

环境依赖说明：

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Node.js 16+（网页端开发和构建需要；小程序若引入 npm 包时也需要）
- 微信开发者工具（小程序预览和真机调试需要）

## 代码导航

| 路径 | 职责 | 修改提示 |
| --- | --- | --- |
| `src/main/java/com/cutting/cuttingsystem/CuttingSystemApplication.java` | Spring Boot 启动入口 | 一般不需要修改 |
| `src/main/java/com/cutting/cuttingsystem/controller/` | REST 接口层 | 新增接口时同步补测试和本文档接口地图 |
| `src/main/java/com/cutting/cuttingsystem/service/` | 服务接口 | 保持业务边界清晰 |
| `src/main/java/com/cutting/cuttingsystem/service/impl/` | 服务实现 | 数据写入逻辑优先放在这里 |
| `src/main/java/com/cutting/cuttingsystem/mapper/` | MyBatis-Plus Mapper | 表结构变化时同步实体、SQL 和测试 |
| `src/main/java/com/cutting/cuttingsystem/entitys/` | 数据实体、DTO、VO、算法模型 | 保持字段命名和前端契约稳定；`entitys` 为既有包名，不要无任务改名 |
| `src/main/java/com/cutting/cuttingsystem/model/` | 排样算法核心 | 修改禁忌搜索或天际线逻辑必须补算法单测 |
| `src/main/java/com/cutting/cuttingsystem/util/` | JWT、用户上下文、算法输入解析等工具 | 修改工具类需检查调用链 |
| `src/main/resources/mapper/` | XML Mapper | 与 Mapper 接口和实体保持一致 |
| `src/main/resources/db/migration/` | 数据库增量脚本 | 新增/调整表结构时维护 |
| `src/main/resources/static/` | 后端托管的静态页面产物 | 避免与独立 `frontend/` 开发源混淆 |
| `frontend/` | Vue 3 网页端 | API 代理前缀为 `/api`，默认转发到 `http://localhost:8080` |
| `miniprogram/` | 微信小程序端 | 后端地址在 `utils/config.js` 中配置 |
| `docs/` | 项目说明、计划和数据库文档 | 重要业务变更应同步更新 |
| `scripts/` | 本地开发启动/停止脚本 | 修改脚本后在 Windows PowerShell 下验证 |

## 接口地图

| 模块 | 接口 | 认证 | 返回约定 |
| --- | --- | --- | --- |
| 认证 | `/auth/login`、`/auth/register`、`/auth/logout` | 不需要 JWT | `Result` |
| 客户 | `/customers`、`/customers/{id}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 板材 | `/boards`、`/boards/{id}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 订单 | `/orders`、`/orders/{id}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 订单明细 | `/order-items`、`/order-items/{id}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 余料 | `/remnants`、`/remnants/{id}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 排样结果 | `/layout-results`、`/layout-results/{id}`、`/layout-results/order/{orderId}` | 需要 `Authorization: Bearer <token>` | `Result` |
| 算法求解 | `POST /algorithm/answer` | 需要 `Authorization: Bearer <token>` | `List<SolutionResponseDTO>` |

认证规则：

- `WebConfig` 拦截 `/**`，排除 `/`、`/index.html`、`/assets/**`、`/favicon.ico`、`/auth/*`。
- 受保护接口必须携带 `Authorization: Bearer <token>`。
- `TokenInterceptor` 验证 JWT 后将 `userId` 写入 `UserContext`，请求结束后清理上下文。

## 数据模型与响应约定

通用响应结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

当前主要业务实体：

- `TUser`：用户与登录认证。
- `TCustomer`：客户信息。
- `TBoard`：板材基础信息。
- `TOrder`、`TOrderItem`：排单和待切割明细。
- `TOffcut`：余料信息。
- `TLayoutResult`：排样结果持久化。
- `Instance`、`Square`、`Solution`、`PlaceSquare`、`PlacePoint`：算法输入、矩形、解和放置结果。

数据模型修改规则：

- 新增字段时同步检查实体、DTO、VO、Mapper、数据库脚本、前端 API、小程序页面和测试。
- 删除或重命名字段前先评估前端兼容性，不要让已有接口静默破坏。
- 表结构变更优先放入 `src/main/resources/db/migration/`，并在相关文档中说明执行顺序。

## 算法模块约定

入口控制器：`src/main/java/com/cutting/cuttingsystem/controller/TestController.java`  
输入 DTO：`src/main/java/com/cutting/cuttingsystem/entitys/algorithm/DTO/InstanceDTO.java`  
输出 DTO：`src/main/java/com/cutting/cuttingsystem/entitys/algorithm/DTO/SolutionResponseDTO.java`  
核心实现：`src/main/java/com/cutting/cuttingsystem/model/TabuSearch.java`  
输入解析与多容器求解：`src/main/java/com/cutting/cuttingsystem/util/ReadDataUtil.java`  
算法测试：`src/test/java/com/cutting/cuttingsystem/model/AlgorithmUnitTest.java`

算法输入核心字段：

- `L`：容器长度。
- `W`：容器宽度。
- `rotateEnable`：是否允许旋转。
- `gapDistance`：板件间距。
- `squareList`：待排样矩形列表，矩形字段包含 `id`、`l`、`w`。

算法修改注意事项：

- `TabuSearch.evaluate(...)` 负责天际线放置评估。
- `TabuSearch.search()` 负责禁忌搜索迭代寻优。
- `ReadDataUtil.getSolution(...)` 会按多容器循环求解，直到剩余矩形清空或判定无法装入。
- 修改放置、旋转、间距或利用率计算时，必须覆盖“可放入、旋转放入、间距导致不可放入、空列表、多容器”场景。

## 前端约定

网页端：

- 源码目录：`frontend/`
- API 封装：`frontend/src/api/`
- 路由：`frontend/src/router/index.js`
- 认证状态：`frontend/src/stores/auth.js`
- 开发代理：`/api` -> `http://localhost:8080`
- 页面范围：登录、工作台、客户、板材、算法输入与结果可视化。

小程序端：

- 源码目录：`miniprogram/`
- API 封装：`miniprogram/services/api.js`
- 统一请求：`miniprogram/utils/request.js`
- 后端地址：`miniprogram/utils/config.js`
- 页面范围：登录、客户、板材、算法输入与结果展示。

前端修改规则：

- 后端接口路径、参数或返回字段变化时，网页端和小程序端必须同步检查。
- 登录成功后前端应保存 token，并在后续业务请求中携带 `Authorization: Bearer <token>`。
- 算法接口成功返回数组，业务接口成功返回 `Result`；前端错误处理要区分这两类结构。

## 测试策略

| 变更类型 | 推荐测试 |
| --- | --- |
| 算法逻辑 | `mvn test -Dtest=AlgorithmUnitTest` 或完整 `mvn test` |
| 接口路径、认证、响应结构 | MockMvc 测试，优先补充 `InterfaceSmokeTest` 和 `AuthenticationAuthorizationTest` |
| 服务和数据库写入逻辑 | Service 单测或集成测试；必要时补 SQL 初始化数据 |
| 网页端改动 | `cd frontend && npm run build`，必要时本地启动检查页面 |
| 小程序端改动 | 微信开发者工具导入 `miniprogram/` 后预览检查 |
| 脚本改动 | 在 Windows PowerShell 下运行对应脚本 |

## 变更记录

后续维护格式：

```text
- YYYY-MM-DD | 类型: API/数据模型/算法/依赖/脚本/文档 | 范围: 文件或模块 | 原因: 为什么修改 | 影响: 兼容性与调用方 | 验证: 测试或检查方式
```

当前记录：

- 2026-04-30 | 类型: 文档 | 范围: AGENTS.md | 原因: 增加 Codex 启动约定，提示后续任务优先读取本文档 | 影响: 不影响运行时行为 | 验证: 文档结构检查
- 2026-04-30 | 类型: 文档 | 范围: AGENTS.md | 原因: 为 Codex 提供项目结构、开发规则和维护约定 | 影响: 不影响运行时行为 | 验证: 文档结构检查
