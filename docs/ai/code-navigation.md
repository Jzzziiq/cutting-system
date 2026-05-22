# 代码导航

本文件记录仓库主要目录职责。修改前只读取和任务相关的目录，不要为了熟悉项目而全量扫描。

| 路径 | 职责 | 修改提示 |
| --- | --- | --- |
| `src/main/java/com/cutting/cuttingsystem/CuttingSystemApplication.java` | Spring Boot 启动入口 | 一般不需要修改 |
| `src/main/java/com/cutting/cuttingsystem/controller/` | REST 接口层 | 新增接口时同步补测试和接口文档 |
| `src/main/java/com/cutting/cuttingsystem/service/` | 服务接口 | 保持业务边界清晰 |
| `src/main/java/com/cutting/cuttingsystem/service/impl/` | 服务实现 | 数据写入逻辑优先放在这里 |
| `src/main/java/com/cutting/cuttingsystem/mapper/` | MyBatis-Plus Mapper | 表结构变化时同步实体、SQL 和测试 |
| `src/main/java/com/cutting/cuttingsystem/entitys/` | 数据实体、DTO、VO、算法模型 | `entitys` 为既有包名，不要无任务改名 |
| `src/main/java/com/cutting/cuttingsystem/model/` | 排样算法核心 | 修改禁忌搜索或天际线逻辑必须补算法单测 |
| `src/main/java/com/cutting/cuttingsystem/util/` | JWT、用户上下文、算法输入解析等工具 | 修改工具类需检查调用链 |
| `src/main/resources/mapper/` | XML Mapper | 与 Mapper 接口和实体保持一致 |
| `src/main/resources/db/migration/` | 数据库增量脚本 | 新增或调整表结构时维护 |
| `src/main/resources/static/` | 后端托管的静态页面产物 | 避免与独立 `frontend/` 开发源混淆 |
| `frontend/` | Vue 3 网页端 | API 代理前缀为 `/api`，默认转发到 `http://localhost:8080` |
| `miniprogram/` | 微信小程序端 | 后端地址在 `utils/config.js` 中配置 |
| `docs/` | 项目说明、计划、数据库和 AI 协作文档 | 重要业务变更应同步更新 |
| `scripts/` | 本地开发启动/停止脚本 | 修改脚本后在 Windows PowerShell 下验证 |
