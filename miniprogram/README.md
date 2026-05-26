# 板材切割系统微信小程序

## 目录结构

- `pages/login`：登录页，对接 `POST /auth/login`
- `services/api.js`：后端接口封装
- `utils/request.js`：统一请求、token、401 处理
- `utils/config.js`：后端服务地址配置

## 使用说明

1. 在微信开发者工具中导入 `miniprogram` 目录。
2. 修改 `utils/config.js` 中的 `baseUrl`。
   - 本机开发工具可使用 `http://localhost:8080`
   - 真机调试请改为电脑局域网 IP，并确保后端允许访问
3. 启动后端服务后，在登录页输入后端已有账号密码。

## 后端接口约定

- 登录接口使用表单参数：`username`、`password`
- 登录成功后保存 `token`，后续接口自动添加 `Authorization: Bearer <token>`
- 业务接口错误按后端 `Result` 结构提示
- 算法接口成功返回数组，失败时如果返回 `Result` 会按错误提示处理
