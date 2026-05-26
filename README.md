# 柜门板材切割排版系统

基于 Spring Boot + Vue 3 + 微信小程序的柜门板材切割排版与生产管理平台。核心能力包括禁忌搜索 / 遗传算法双策略排样优化、订单生命周期状态机、RBAC 权限体系、操作审计日志，支持 Docker 一键部署。

## 功能概览

- **排样优化** — 禁忌搜索 + 天际线放置 / 遗传算法双策略，支持旋转、间隙、安全边距
- **排版可视化** — Canvas 2D 实时渲染排样结果，支持缩放、件料标注、利用率统计
- **订单管理** — 订单录入、拆单、状态流转（待处理 → 已排版 → 生产中 → 已完成）
- **生产看板** — 任务分配、拖拽流转、生产进度追踪
- **RBAC 权限** — 角色 + 权限矩阵，支持多组织隔离（租户级数据隔离）
- **审计日志** — @AuditLog 注解自动记录关键操作
- **NC 刀路导出** — 排样结果导出为 NC 加工文件
- **柜体设计** — 3D 柜体模板管理与板件拆解
- **微信小程序** — 现场录入、扫码查询、结果展示

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.5.11, Java 17, MyBatis-Plus 3.5.15, JWT (jjwt 0.12.6), EasyExcel 4.0.3 |
| 前端 | Vue 3.5, Vite 7, Pinia 3, Vue Router 4, Element Plus 2.14, ECharts 6, Three.js 0.184 |
| 小程序 | 微信小程序原生框架, @vant/weapp 1.10.5 |
| 算法 | 禁忌搜索 + 天际线放置 + 遗传算法 (AlgorithmRegistry 工厂模式) |
| 数据库 | MySQL 8.0 (`board_cutting_db`) |
| 部署 | Docker Compose (MySQL + Spring Boot + Nginx) |

## 项目结构

```
cutting-system/
├── src/                          # Spring Boot 后端
│   ├── main/java/com/cutting/cuttingsystem/
│   │   ├── controller/           # REST 控制器（认证、客户、板材、订单、算法、排版结果等）
│   │   ├── entitys/              # 实体、DTO、VO（包名 entitys 为历史约定）
│   │   ├── model/                # 算法实现：TabuSearch, GeneticAlgorithm, AlgorithmRegistry
│   │   ├── service/              # 业务逻辑接口与实现
│   │   ├── mapper/               # MyBatis-Plus Mapper；XML 在 resources/mapper/
│   │   ├── interceptor/          # TokenInterceptor → UserContext (ThreadLocal)
│   │   ├── aop/                  # @AuditLog 切面
│   │   ├── config/               # WebConfig, JwtConfig, MybatisPlusConfig（租户 + 分页）
│   │   └── util/                 # JwtUtil, ReadDataUtil（算法编排）
│   └── main/resources/
│       ├── application.yml       # 主配置（端口 8080, 数据库, JWT）
│       ├── db/migration/         # SQL 迁移脚本（无 Flyway，手动执行）
│       └── mapper/               # MyBatis XML 映射文件
├── frontend/                     # Vue 3 网页端（Vite + Element Plus）
│   ├── src/api/                  # Axios 接口封装
│   ├── src/components/           # 通用组件（AppShell, LayoutCanvas 等）
│   ├── src/composables/          # 组合式函数（useLayoutRunner, useLayoutDataLoader 等）
│   ├── src/views/                # 页面视图
│   └── src/stores/               # Pinia 状态管理
├── miniprogram/                  # 微信小程序端
│   ├── pages/                    # 小程序页面
│   └── services/api.js           # 接口封装
├── docs/                         # 项目文档
│   ├── ai/                       # AI 辅助开发专题文档
│   └── user-manual/              # 用户手册
├── docker/                       # Docker 初始化脚本
├── nginx/                        # Nginx 配置
├── scripts/                      # 本地开发脚本（start-dev.ps1, stop-dev.ps1）
├── pom.xml                       # Maven 项目配置
├── Dockerfile                    # 后端容器构建
└── docker-compose.yml            # 编排：MySQL + Backend + Nginx
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+（项目自带 Maven Wrapper）
- MySQL 8.0+
- Node.js 20.19+ 或 22.12+，npm 10+
- 微信开发者工具（小程序开发）

### 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE board_cutting_db DEFAULT CHARACTER SET utf8mb4;"

# 执行迁移脚本（按文件名顺序）
mysql -u root -p board_cutting_db < src/main/resources/db/migration/20260430_align_bishe_schema.sql
mysql -u root -p board_cutting_db < src/main/resources/db/migration/20260507_algorithm_task.sql
# ... 按顺序执行 db/migration/ 下所有 .sql 文件
```

配置数据库连接：复制 `src/main/resources/application-local.yml.example` 为 `application-local.yml`，填入本地 MySQL 密码。

### 后端启动

```bash
# 使用 Maven Wrapper
./mvnw spring-boot:run

# 或指定本地仓库
mvn "-Dmaven.repo.local=target/.m2" spring-boot:run
```

后端运行在 `http://localhost:8080`。

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，自动代理 `/api` 到后端 `:8080`。

### 一键启动脚本

```powershell
# 启动后端 + 前端
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1

# 重启
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -Restart

# 自定义端口
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -BackendPort 8090 -FrontendPort 5174

# 停止
powershell.exe -ExecutionPolicy Bypass -File scripts\stop-dev.ps1
```

日志输出到 `logs/dev/backend.log` 和 `logs/dev/frontend.log`。

### Docker 部署

```bash
docker-compose up -d --build
```

启动 MySQL (:3307)、后端 (:8080)、Nginx (:80)。需设置环境变量 `MYSQL_ROOT_PASSWORD` 和 `JWT_SECRET`。

### 小程序

在微信开发者工具中打开 `miniprogram/` 目录，配置 `services/api.js` 中的后端地址。

## 测试

```bash
# 后端单元测试
mvn test

# 指定测试类
mvn test -Dtest="AlgorithmUnitTest"

# 前端构建检查
cd frontend && npm run build
```

## 认证与权限

- JWT Bearer Token 认证，`Authorization: Bearer <token>`
- `UserContext` (ThreadLocal) 存储 userId / orgId / roles / permissions
- `@RequirePermission` 注解实现接口级 RBAC 控制
- 多租户隔离：`TenantLineInnerInterceptor` 自动追加 `WHERE org_id = ?`

## API 概览

业务接口统一返回 `{ "code": 200, "msg": "success", "data": {} }`。

| 模块 | 接口前缀 | 说明 |
|------|----------|------|
| 认证 | `/auth/*` | 登录、注册、注销 |
| 客户 | `/customers/*` | 客户 CRUD |
| 板材 | `/boards/*` | 板材 CRUD、纹理上传 |
| 订单 | `/orders/*` | 订单管理、排样输入、状态流转 |
| 排样 | `/algorithm/*` | 算法提交、结果查询、算法对比、算法列表 |
| 排版结果 | `/layout-results/*` | 排版结果保存、查询、删除 |
| 生产任务 | `/production-tasks/*` | 任务分配、状态更新 |
| 通知 | `/notifications/*` | 系统通知 |
| 审计日志 | `/audit-logs/*` | 操作日志查询 |
| 用户 | `/users/*` | 用户信息、角色管理 |

完整接口文档见 `docs/ai/api-contract.md`。

## 文档导航

| 文档 | 说明 |
|------|------|
| `AGENTS.md` | AI 工具约定、硬约束、文档入口 |
| `docs/ai/code-navigation.md` | 代码目录与模块职责 |
| `docs/ai/api-contract.md` | REST 接口地图与认证说明 |
| `docs/ai/data-model.md` | 实体、DTO/VO、数据库表结构 |
| `docs/ai/algorithm.md` | 禁忌搜索、天际线放置、遗传算法详解 |
| `docs/ai/frontend.md` | Vue 网页端与小程序端开发规范 |
| `docs/ai/testing.md` | 测试策略与运行方式 |
| `docs/ai/change-log.md` | 文档变更记录 |
| `docs/user-manual/操作手册.md` | 用户操作指南 |

## 许可证

私有项目，未公开授权。
