# 项目说明

## AI 工具约定

- AI 工具在本仓库开始分析、修改、测试或文档维护前，应先读取本文件。
- 用户提到“需求文档”时，指 `docs/user-manual/需求文档.md`，不是其他文档。
- 本文件是轻量项目入口，只放每次任务都应默认知道的事实、硬约束和文档导航；详细规则按任务读取 `docs/ai/` 下的专题文档。
- 如果本文件与用户当前明确指令冲突，以用户当前指令为准；如果文档与局部代码事实冲突，先读取代码并在回复中说明差异。
- 不要把接口清单、数据模型、目录百科或长变更记录继续堆进本文件；需要维护时更新对应专题文档。

project_name: cutting-system
project_type: 柜门板材切割排版系统
primary_language: Java
backend_framework: Spring Boot 3.5.11
java_version: 17
database: MySQL 8.0+
orm: MyBatis-Plus 3.5.x
auth: JWT + Spring MVC Interceptor
frontend_web: Vue 3 + Vite + Pinia + Vue Router + Axios + Element Plus + Canvas 2D
frontend_miniprogram: 微信小程序原生框架
algorithm_entry: `POST /algorithm/answer`
algorithm_strategy: 禁忌搜索 + 天际线放置算法
last_updated: 2026-05-20

本项目是柜门板材切割排版系统。后端负责认证、客户、板材、订单、余料、排样结果、算法接口和静态资源托管；网页端负责后台操作和排样可视化；小程序端负责现场录入和结果展示。

## 必读硬约束

1. 优先做小步、明确、可验证的修改；不要顺手大范围重构。
2. 保持接口返回结构稳定：业务接口默认返回 `Result { code, msg, data }`，`POST /algorithm/answer` 例外，直接返回 `List<SolutionResponseDTO>`。
3. 不要随意修改已有包名、表字段名、接口路径和 DTO/VO 字段名。
4. 跨后端、网页端、小程序端的任务，先确认接口契约，再分层修改。
5. 新增 API、修改数据模型、调整算法逻辑或引入新依赖时，必须更新对应 `docs/ai/` 专题文档和 `docs/ai/change-log.md`。
6. 不要提交本地运行产物、日志和敏感配置；数据库密码、JWT 密钥等生产配置应使用环境变量或外部配置。
7. 中文注释只解释业务约束、算法意图或非显然逻辑，避免无意义注释。
8. 修改后说明改了哪些文件、为什么改、如何验证；无法运行验证时写明原因和风险。
9. Codex 默认不要主动执行全量测试、冒烟测试、浏览器自动化、Playwright/E2E 或长时间服务联调；除非用户明确授权，否则提供可交给其他 agent 执行的测试方案。

环境依赖：JDK 17+、Maven 3.6+、MySQL 8.0+、Node.js 20.19+ 或 22.12+、微信开发者工具。

## 任务文档入口

| 任务类型 | 先读文档 |
| --- | --- |
| 代码目录、模块职责、修改位置 | `docs/ai/code-navigation.md` |
| REST 路径、认证、返回结构、前后端契约 | `docs/ai/api-contract.md` |
| 实体、DTO/VO、Mapper、数据库脚本 | `docs/ai/data-model.md` |
| 禁忌搜索、天际线放置、算法输入输出 | `docs/ai/algorithm.md` |
| Vue 网页端、Element Plus、Canvas、小程序 | `docs/ai/frontend.md` |
| 单测、MockMvc、构建、高成本验证策略 | `docs/ai/testing.md` |
| 文档变更记录与维护格式 | `docs/ai/change-log.md` |
| 本地启动/停止脚本细节 | `scripts/README.md` |

## 子目录专属规则

后端、前端、小程序的专属约束和常用命令已拆到对应子目录 `CLAUDE.md`，按需加载：
- 后端（`src/`）：认证、表结构迁移、算法单测、接口同步检查
- 前端（`frontend/`）：核心页面保护、API 同步检查
- 小程序（`miniprogram/`）：API 同步检查

## 维护原则

- `AGENTS.md` 目标控制在 60 行左右；跨端通用规则留本文件，端专属规则拆到子目录 `CLAUDE.md`。
- 只有”每次任务都必须默认知道”的规则才放在本文件。
- 专题文档也要保持短、准、可执行；长篇说明应继续拆到业务文档、架构文档或测试方案中。
