# 板材切割系统

板材切割系统是一套面向柜门/板材加工场景的管理与排样系统。当前项目包含 Spring Boot 后端、Vue 3 网页端、微信小程序端和算法排样模块，核心目标是维护客户与板材数据、录入待切尺寸、调用排样算法并可视化结果。

## 当前能力

- 用户登录、JWT 鉴权和用户上下文。
- 客户管理接口与页面。
- 板材管理接口与页面。
- 算法求解接口 `/algorithm/answer`。
- 禁忌搜索 + 天际线放置的二维矩形排样算法。
- 网页端登录、工作台、客户、板材、算法输入和结果可视化。
- 微信小程序登录、客户、板材、算法输入和结果展示基础页面。
- 本地一键启动/停止脚本。
- 参数校验、统一异常处理、日志和测试覆盖。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17, Spring Boot 3, Spring Web, MyBatis-Plus, MySQL |
| 鉴权 | JWT, Spring MVC Interceptor |
| 校验与日志 | Jakarta Bean Validation, Slf4j, Logback |
| 网页端 | Vue 3, Vite, Pinia, Vue Router, Axios |
| 小程序 | 微信小程序原生框架 |
| 测试 | JUnit 5, MockMvc |

## 本地开发

推荐使用脚本同时启动后端和网页端：

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1
```

启动后访问：

- 后端：`http://127.0.0.1:8080`
- 网页端：`http://127.0.0.1:5173`

停止服务：

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\stop-dev.ps1
```

更多脚本说明见 [scripts/README.md](scripts/README.md)。

## 文档入口

| 文档 | 用途 |
| --- | --- |
| [docs/文档总览.md](docs/文档总览.md) | 项目文档入口、整理结论和后续使用指引 |
| [docs/项目说明书.md](docs/项目说明书.md) | 项目背景、目标、范围、技术方案和路线图 |
| [docs/小程序与网页端开发计划.md](docs/小程序与网页端开发计划.md) | 双端定位、前端计划和图片识别路线 |
| [docs/数据库结构补齐说明.md](docs/数据库结构补齐说明.md) | 数据库现状、缺口和增量 SQL 说明 |
| [docs/毕设文档整合说明.md](docs/毕设文档整合说明.md) | 历史毕设资料到当前项目的映射 |
| [src/main/resources/相关文档/算法模块整体概况.md](src/main/resources/相关文档/算法模块整体概况.md) | 算法模块设计、数据结构和性能说明 |
| [src/main/resources/相关文档/算法接口文档.md](src/main/resources/相关文档/算法接口文档.md) | 算法求解接口请求和响应格式 |
| [frontend/README.md](frontend/README.md) | 网页端开发与构建说明 |
| [miniprogram/README.md](miniprogram/README.md) | 微信小程序导入和接口约定 |

## 测试

```powershell
mvn "-Dmaven.repo.local=F:\Code\Java\cutting-system\target\.m2" test
```

## 后续重点

- 补齐算法输入规模和尺寸上限，避免大输入造成资源压力。
- 打通排单、方案保存和历史复用。
- 增加余料库和排版结果持久化。
- 小程序端推进图片识别手写尺寸 MVP。
- 生产环境中将数据库密码、JWT 密钥等敏感配置迁移到环境变量。
