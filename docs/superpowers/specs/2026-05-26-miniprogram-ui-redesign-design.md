# 小程序前端 UI 全面优化设计文档

## 背景

当前小程序（miniprogram/）视觉品质不足：纯手写 CSS、无组件库、无图标、无动画、硬编码颜色值、样式重复。用户希望全面提升至专业商业应用水准。

## 设计方向

- **风格**：简约专业，参考飞书/钉钉企业工具风格——克制、专业、不花哨
- **场景**：混合场景（工厂车间 + 办公室），需中偏大字号、高对比度、大触摸区域
- **实现**：引入 Vant Weapp 组件库全面迁移

## 设计规范（Design Tokens）

通过 Vant 主题变量系统统一管理。在 `app.wxss` 中用 `page` 选择器覆盖 Vant 默认主题变量，自定义 Token 以 `--app-` 前缀区分：

| 类别 | 变量 | 值 | 说明 |
|------|------|------|------|
| 主色 | `--primary-color` | `#2563eb` | 品牌蓝，专业感 |
| 成功色 | `--success-color` | `#10b981` | 完成状态 |
| 警告色 | `--warning-color` | `#f59e0b` | 待处理状态 |
| 危险色 | `--danger-color` | `#ef4444` | 删除/错误 |
| 主文字 | `--text-primary` | `#1f2937` | 深灰近黑 |
| 次文字 | `--text-secondary` | `#6b7280` | 辅助信息 |
| 占位文字 | `--text-placeholder` | `#9ca3af` | 输入框占位 |
| 页面背景 | `--bg-page` | `#f5f7fa` | 略偏蓝灰 |
| 卡片背景 | `--bg-card` | `#ffffff` | 白色 |
| 正文字号 | 基准 | `28rpx` | 正文 |
| 小字 | 辅助 | `24rpx` | 次要信息 |
| 区域标题 | 标题 | `32rpx` | 区域标题 |
| 页面标题 | 大标题 | `36rpx` | 页面标题 |
| 卡片圆角 | `--radius-card` | `16rpx` | 比当前 12rpx 更圆润 |
| 按钮圆角 | `--radius-btn` | `12rpx` | 比当前 8rpx 更现代 |
| 页面边距 | `--spacing-page` | `32rpx` | 比当前 24rpx 更宽松 |
| 卡片内边距 | `--spacing-card` | `28rpx` | 统一内边距 |

## Vant 组件映射

| 当前自定义 | 替换为 Vant 组件 | 说明 |
|------------|-----------------|------|
| `.primary` / `.secondary` / `.danger` / `.ghost` | `van-button` | type/size/loading/disabled |
| `.field` + 原生 input/textarea | `van-field` | 内置标签、校验、清除 |
| `.section` 白色卡片 | `van-cell-group` + `van-cell` | 标准列表卡片 |
| `.status-tag` | `van-tag` | primary/success/warning/danger |
| `.dialog-mask` / `.dialog` | `van-dialog` | 内置确认/取消 |
| 密码修改弹窗 | `van-dialog` + `van-field` | 组合使用 |
| `.badge` / `.dot` | `van-badge` | 数字和小红点 |
| 任务列表 `.list-item` | `van-cell` + `van-icon` | 图标 + 标题 + 描述 + 箭头 |
| `.empty` | `van-empty` | 内置空状态插图 |
| 标题栏 `.title` | `van-nav-bar` | 统一标题栏 |
| 加载状态 | `van-button` loading 属性 | 原生支持 |
| TabBar | Vant 自定义 TabBar | 图标 + 文字 + 徽标 |
| 搜索框 | `van-search` | 内置搜索图标和清除 |
| 状态筛选 | `van-tab` + `van-tabs` | 胶囊筛选标签 |
| 下拉刷新 | `van-pull-refresh` | 任务列表刷新 |
| 上拉加载 | `van-list` | 分页加载更多 |

## 图标方案

使用 Vant 内置图标（`vant-icon`）：

| 位置 | 图标名 | 用途 |
|------|--------|------|
| 任务列表项 | `orders-o` | 任务图标 |
| 任务详情-板材 | `description` | 板材信息 |
| 通知列表 | `bell` | 通知图标 |
| 个人中心-用户 | `user-o` | 用户信息 |
| 个人中心-密码 | `lock` | 修改密码 |
| 个人中心-退出 | `power-off` | 退出登录 |
| 列表箭头 | `arrow` | 右侧导航 |
| 空状态 | `search` / `info-o` | 无数据提示 |
| 搜索框 | `search` | 搜索图标 |

## 视觉设计要点

### 任务列表页
- 顶部品牌蓝导航栏（`#2563eb` 渐变到 `#1d4ed8`）
- 搜索框：白色圆角卡片 + 搜索图标
- 状态筛选：胶囊标签栏（全部/待处理/进行中/已完成），选中态带蓝色阴影
- 任务卡片：左侧状态色竖条（蓝/黄/绿）+ 订单图标 + 信息区 + 状态胶囊标签
- 卡片底部：日期（灰色）+ "查看详情"蓝色链接，用分隔线隔开
- 底部 TabBar：图标放大、选中态加粗

### 任务详情页
- 顶部 Hero 卡片：品牌蓝渐变背景 + 半透明圆形装饰
- Hero 内容：订单号大字 + 状态胶囊 + 客户/板材/日期三列统计
- 板材清单：白色卡片 + 表头灰底 + 行分隔线
- 操作区：主按钮加大加投影，次按钮白底描边

### 个人中心页
- 头像卡片：顶部渐变蓝背景 + 白色边框圆形头像 + 姓名角色
- 菜单列表：图标（圆角方块背景）+ 文字 + 箭头，退出登录用红色

### 通知页
- 通知列表：圆角卡片包裹，每项左侧状态圆点
- 未读：蓝色圆点 + 光晕（box-shadow 扩散）
- 已读：灰色圆点
- 每项：标题（加粗）+ 时间（右对齐灰色）+ 描述（次级灰色）

### 通用改进
- 所有白色区域统一 16rpx 圆角 + box-shadow（`0 1px 4px rgba(0,0,0,0.04)`）
- 卡片触摸反馈：`active` 态缩小 0.98 + 阴影加深（微信小程序无 hover，用 `active` 伪类模拟）
- 信息层次：标题加粗 > 描述灰色 > 日期最淡
- 状态标签改为胶囊形（border-radius: 100px）

## 文件变更范围

### 新增依赖
- `miniprogram/package.json` — 添加 `@vant/weapp` 依赖

### 全局样式重构
- `miniprogram/app.wxss` — 删除大部分自定义类，改为 Vant 主题变量 + 少量全局覆盖
- `miniprogram/app.json` — 引用 Vant 组件、更新 TabBar 配置

### 页面重构（优先级排序）
1. `miniprogram/pages/tasks/index.*` — 任务列表（van-search, van-tabs, van-cell, van-pull-refresh, van-list）
2. `miniprogram/pages/tasks/detail.*` — 任务详情（van-cell-group, van-tag, van-button）
3. `miniprogram/pages/profile/index.*` — 个人中心（van-cell, van-dialog, van-field）
4. `miniprogram/pages/notifications/index.*` — 通知页（van-cell, van-badge）
5. `miniprogram/pages/login/login.*` — 登录页（van-field, van-button）

### 删除重复代码
- `tasks/index.wxss` 和 `tasks/detail.wxss` 中重复的 `.status-tag` 类

## 交互反馈改进

- 按钮点击：`van-button` 内置 loading 状态
- 表单提交：`van-dialog` 确认弹窗 + `van-toast` 成功提示
- 列表加载：`van-pull-refresh` 下拉刷新 + `van-list` 触底加载
- 空状态：`van-empty` 组件展示无数据插图
- 操作成功：`van-toast` 轻提示（2秒自动消失）

## 验证方案

1. 安装 Vant Weapp 后在微信开发者工具中预览各页面
2. 检查所有页面的 Vant 组件渲染是否正常
3. 测试任务列表的筛选、搜索、下拉刷新功能
4. 测试任务详情的操作按钮交互
5. 测试个人中心的密码修改弹窗
6. 测试通知页的未读/已读状态显示
7. 在不同屏幕尺寸下验证布局适配
