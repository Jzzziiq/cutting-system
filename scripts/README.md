# 本地开发脚本

## 启动项目

在项目根目录执行：

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1
```

脚本会启动：

- 后端：http://127.0.0.1:8080
- 前端：http://127.0.0.1:5173

首次运行时，如果 `frontend/node_modules` 不存在，脚本会自动执行前端依赖安装。

## 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 20.19+ 或 22.12+（当前 Vite 7 / @vitejs/plugin-vue 6 的要求）
- npm 10+

## 跳过依赖安装

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -SkipInstall
```

## 自定义端口

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -BackendPort 8090 -FrontendPort 5174
```

脚本会同时设置 Spring Boot `server.port` 和前端 `VITE_BACKEND_URL`，确保 `/api` 代理跟随后端端口。

## 重启项目

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -Restart
```

## 停止项目

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts\stop-dev.ps1
```

## 日志位置

```text
logs\dev\backend.log
logs\dev\frontend.log
```

## PID 记录

```text
target\dev-services.json
```

停止脚本会读取该文件，只清理本脚本启动的开发服务。
