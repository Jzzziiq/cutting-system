# 柜门板材切割排版系统

基于 Spring Boot 的切割优化与生产管理平台。核心能力：禁忌搜索/遗传算法双策略排样、订单生命周期状态机、RBAC 权限体系、操作审计、Docker 部署。

## 快速开始

```powershell
# 一键启动后端 + 前端
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1

# 或分别启动
mvn "-Dmaven.repo.local=target\.m2" spring-boot:run   # 后端 :8080
cd frontend && npm install && npm run dev              # 前端 :5173
```

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.5 + MyBatis-Plus + MySQL 8.0 |
| 前端 | Vue 3 + Vite + Element Plus + Pinia |
| 小程序 | 微信小程序原生框架 |
| 算法 | 禁忌搜索 + 天际线放置 + 遗传算法 |
| 部署 | Docker Compose (MySQL + Nginx + Backend) |

## 文档导航

| 文档 | 说明 |
|------|------|
| `AGENTS.md` | 完整接口地图、架构、数据模型、变更记录 |
| `docs/user-manual/操作手册.md` | 安装部署、使用指南、接口参考、故障排查 |
| `docs/planning/expansion-plan.md` | 功能规划与进度 |
| `docs/planning/known-issues.md` | 开发踩坑记录 |
| `docs/user-manual/文档总览.md` | 全部文档索引 |

## 测试

```powershell
mvn test
```
