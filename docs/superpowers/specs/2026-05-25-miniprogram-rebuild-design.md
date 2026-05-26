# 小程序重构 + Web 生产员页面清理

## 背景

生产员在车间操作，无法使用电脑，只能通过手机访问系统。当前小程序只有客户、板材、算法功能，与生产员工作无关。需要将生产员的工作流（任务查看、状态流转、通知）迁移到小程序，同时从 Web 前端移除生产员专属页面。

## 目标

1. 完全重写小程序，聚焦生产员工作流
2. 从 Web 前移除 ProducerTasksView 和 ProductionMyOrdersView
3. 新增后端通知功能

### 分阶段策略

- **第一阶段**（本次）：小程序核心功能 + 应用内通知 + Web 清理
- **第二阶段**（后续）：微信订阅消息推送（依赖微信认证、消息模板、openid 存储等外部流程）

## 范围

- **小程序**：完全重写，删除所有旧页面
- **Web 前端**：仅删除生产员专属页面和路由，管理员/操作员功能不动
- **后端**：新增通知表和 API、新增 `/users/me` 端点、补充任务详情板材品牌字段、修改任务分配逻辑触发通知、确认 NC 文件下载方式（可能需新增代理端点）

---

## 小程序设计

### 目录结构

```
miniprogram/
  app.js                  -- 入口，存储 token/userInfo
  app.json                -- 页面路由 + tabBar（2 tab）
  app.wxss                -- 全局样式
  project.config.json     -- 微信开发者工具配置
  sitemap.json
  utils/
    config.js             -- baseUrl 配置
    request.js            -- 请求封装（JWT、401、错误提示）
  services/
    api.js                -- 所有 API 函数
  pages/
    login/                -- 登录
    tasks/
      index               -- 我的任务列表（tabBar）
      detail              -- 任务详情
    profile/
      index               -- 个人设置（tabBar）
    notifications/
      index               -- 通知列表
```

### tabBar

| 标签 | 页面 | 图标 |
|------|------|------|
| 我的任务 | pages/tasks/index | task |
| 个人设置 | pages/profile/index | profile |

### 角色校验

**小程序端**：
- 登录后检查用户角色，只允许 `viewer`（生产员）角色使用
- 其他角色（admin、org_admin、operator）登录后显示提示："请使用电脑端管理系统"，不允许进入主界面

**Web 端**：
- `isProducer` getter 保留，用于路由守卫判断
- viewer 角色登录后重定向到 `/profile`，显示"请使用小程序"提示

### 页面设计

#### 1. 登录页 (`pages/login/login`)

- 用户名 + 密码表单
- 调用 `POST /auth/login`（form-encoded）
- 成功后存储 token 和 userInfo 到 Storage
- 跳转到"我的任务"
- 已有 token 时自动跳转
- 登录时通过 `wx.login` 获取 code 暂存（第二阶段用于换 openid）

#### 2. 我的任务 (`pages/tasks/index`)

- **标题栏**：右侧铃铛图标 + 未读数角标，点击进入通知列表
- **数据源**：`GET /production-tasks/my`
- **卡片布局**：任务名称、订单号、状态标签（待处理/生产中/已完成）、预计工时
- **交互**：点击卡片 → 任务详情，下拉刷新
- **排序**：按状态分组（待处理在前）或按时间倒序

#### 3. 任务详情 (`pages/tasks/detail`)

- **数据源**：`GET /production-tasks/my/{taskId}`
- **信息展示**：
  - 任务名称、订单号（标识当前任务）
  - 状态标签 + 操作按钮（核心流转）
  - **板材信息**：品牌、材质、颜色、厚度（领料核对，防止上错料）
  - 备注（工艺特殊要求：雕刻深度、纹理方向、特殊刀具等）
  - 预计工时（安排生产节奏）
  - 开始/完成时间（回溯用）
- **部件明细表**：部件名称、尺寸（长×宽）、数量（加工完成后逐件核对）
- **NC 文件下载**：显示下载按钮，操作员下载后传到雕刻机。下载方式需实施时确认：
  - 如果 `layoutResult.ncFilePath` 是 HTTP 可访问的相对路径 → 直接 `wx.downloadFile`
  - 如果是本地磁盘路径 → 后端需新增代理下载端点 `GET /layout-results/{id}/nc`
- **不展示**：排样利用率、容器数量（管理指标）、客户地址/联系方式（配送环节）
- **底部操作按钮**：
  - 待处理（status=0）→ "开始生产"按钮，调用 `PUT /production-tasks/my/{id}/status`，body: `{ "status": 1 }`
  - 生产中（status=1）→ "完成任务"按钮，调用 `PUT /production-tasks/my/{id}/status`，body: `{ "status": 2 }`
  - 已完成（status=2）→ 只读，无按钮

#### 4. 个人设置 (`pages/profile/index`)

- 显示当前用户信息（用户名、角色、组织）
- 修改密码功能（需后端支持，见后端改动）
- 退出登录：清除 Storage，跳转登录页

#### 5. 通知列表 (`pages/notifications/index`)

- **数据源**：`GET /notifications`
- **列表项**：标题、内容摘要、时间、已读/未读状态（未读加圆点）
- **交互**：
  - 点击通知 → 标记已读 + 跳转任务详情
  - 顶部"全部已读"按钮
- **空状态**：无通知时显示空状态提示

### API 清单

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `login(username, password)` | POST | `/auth/login` | form-encoded |
| `listMyTasks()` | GET | `/production-tasks/my` | 我的任务列表 |
| `getMyTaskDetail(taskId)` | GET | `/production-tasks/my/{taskId}` | 任务详情 |
| `transitionTask(taskId, status)` | PUT | `/production-tasks/my/{taskId}/status` | 状态流转 |
| `listNotifications(page, size)` | GET | `/notifications` | 通知列表 |
| `markNotificationRead(id)` | PUT | `/notifications/{id}/read` | 标记已读 |
| `markAllNotificationsRead()` | PUT | `/notifications/read-all` | 全部已读 |
| `getProfile()` | GET | `/users/me` | 获取个人信息（需新增端点） |
| `changePassword(data)` | PUT | `/users/me/password` | 修改密码（已有，UserController:183） |
| `downloadNc(layoutResultId)` | GET | 待确认 | NC 文件下载（见下方说明） |

---

## 后端改动

### 新增：通知表 `t_notification`

```sql
CREATE TABLE t_notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '接收用户ID',
  title VARCHAR(100) NOT NULL COMMENT '通知标题',
  content VARCHAR(500) COMMENT '通知内容',
  task_id BIGINT COMMENT '关联任务ID',
  is_read TINYINT DEFAULT 0 COMMENT '0=未读 1=已读',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_read (user_id, is_read),
  INDEX idx_user_created (user_id, created_at)
);
```

### 新增：通知 API

| 端点 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `/notifications` | GET | 登录用户 | 分页查询自己的通知 |
| `/notifications/{id}/read` | PUT | 登录用户 | 标记单条已读 |
| `/notifications/read-all` | PUT | 登录用户 | 全部已读 |

### 新增：用户个人信息 API

当前后端没有 `GET /users/me` 端点。需要新增：

| 端点 | 方法 | 说明 |
|------|------|------|
| `/users/me` | GET | 获取当前用户信息，从 JWT 中取 userId，返回用户基本信息 |

> `PUT /users/me/password` 已存在（`UserController:183`），无需新增。
> `GET /users/me` 实现方式：通过 `UserContext.getCurrentUserId()` 获取当前用户 ID，复用现有 `UserService` 查询逻辑，约 10 行代码。

### 确认：NC 文件下载方式

`layoutResult.ncFilePath` 存储的路径类型需实施时确认：
- 如果是 HTTP 可访问的相对路径（如 `/ncfiles/order_123.nc`）→ 小程序直接 `wx.downloadFile` 下载
- 如果是本地磁盘路径（如 `/data/nc/order_123.nc`）→ 需新增代理下载端点 `GET /layout-results/{id}/nc`，后端读取文件并以流方式返回

### 补充：任务详情板材信息

当前 `GET /production-tasks/my/{taskId}` 返回 `TProductionTaskDetailVO`，包含：
- `task` — 任务基本信息
- `order` — 订单信息，含 `items`（`List<TOrderItemVO>`）
- `layoutResult` — 排样结果，含 `ncFilePath`

`TOrderItemVO` 已有 `materialName`、`color`、`thickness`（来自 `t_order_item` 表的冗余字段），但**缺少 `brand`**。

需要补充：
- 在 `TOrderItemVO` 中增加 `brand` 字段
- 在 `ProductionTaskServiceImpl.getMyTaskDetail` 中，通过 `boardId` 关联查询 `t_board` 表获取 `brand`，填充到 VO 中

### 修改：任务分配触发通知

两个分配方法都需要触发通知：

| 方法 | 触发点 | 说明 |
|------|--------|------|
| `assignOrderTask(orderId, assigneeId)` | 主入口 | 按订单分配，自动创建任务，最常用 |
| `assignTask(taskId, assigneeId, assigneeName)` | 单任务分配 | 底层方法，也需触发 |

实现：在两个方法的 `updateById` / `updateAssignmentIgnoreTenant` 之后，插入 `t_notification` 记录。通知内容包含任务名称和订单号。

### 第二阶段：微信订阅消息（本次不做）

- 依赖：微信认证、消息模板创建、openid 存储
- 实现：用户表新增 `openid` 字段，小程序 `wx.login` 获取 code 后端换 openid，任务分配时调用微信 `subscribeMessage.send` API
- 前置：小程序需完成微信认证（非个人主体）

---

## Web 前端清理

### 删除文件

- `frontend/src/views/ProducerTasksView.vue`
- `frontend/src/views/ProductionMyOrdersView.vue`

### 清理引用

| 文件 | 改动 |
|------|------|
| `frontend/src/router/index.js` | 移除 `producer-tasks` 和 `production-my-orders` 路由；viewer 登录后重定向到 `/profile` 并显示提示 |
| `frontend/src/components/AppShell.vue` | 移除 viewer 角色的侧边栏入口（"我的任务"、"我的生产订单"） |
| `frontend/src/api/production-tasks.js` | 移除 `listMyTasks`、`getMyTaskDetail`、`myTransitionTask`（确认无其他调用后） |
| `frontend/src/stores/auth.js` | 保留 `isProducer` getter（用于路由守卫判断） |

### viewer 角色 Web 端处理

viewer 登录 Web 端后：
- 路由守卫检测到 `isProducer`，重定向到 `/profile`（个人设置）
- 个人设置页面顶部显示提示条："生产员请使用微信小程序处理生产任务"
- 不删除 `isProducer` getter，路由守卫仍需要它来识别 viewer 角色

---

## 验证方案

1. **小程序**：用微信开发者工具测试
   - viewer 角色登录 → 跳转我的任务
   - 非 viewer 角色登录 → 显示"请使用电脑端"
   - 查看任务列表 → 点击进入详情
   - 详情页显示板材信息（品牌/材质/颜色/厚度）和部件明细
   - 开始生产 → 状态变为生产中
   - 完成任务 → 状态变为已完成
   - 铃铛图标 → 通知列表 → 点击通知跳转详情
   - 个人设置 → 修改密码 → 退出登录
2. **Web 前端**：
   - viewer 角色登录 → 重定向到个人设置，显示"请使用小程序"提示
   - operator 角色登录 → 正常功能不受影响
3. **后端**：
   - `GET /users/me` 返回当前用户信息
   - `PUT /users/me/password` 修改密码
   - 通知 API CRUD 正常工作
