# 小程序前端 UI 全面优化设计文档

## 背景

当前小程序（miniprogram/）视觉品质不足：纯手写 CSS、无组件库、无图标、无动画、硬编码颜色值、样式重复。用户希望全面提升至专业商业应用水准。

## 设计方向

- **风格**：简约专业，参考飞书/钉钉企业工具风格——克制、专业、不花哨
- **场景**：混合场景（工厂车间 + 办公室），需中偏大字号、高对比度、大触摸区域
- **实现**：引入 Vant Weapp 1.x 组件库全面迁移

## npm 构建链路

当前小程序无任何 npm 基础设施。`.gitignore` 已包含 `miniprogram/node_modules/` 和 `miniprogram/miniprogram_npm/`。

### 构建步骤

1. **创建 `miniprogram/package.json`**：
   ```json
   {
     "name": "cutting-system-miniprogram",
     "version": "1.0.0",
     "private": true,
     "dependencies": {
       "@vant/weapp": "^1.11.7"
     }
   }
   ```
   注意：必须用 `@vant/weapp` 1.x（微信小程序版），不能用 `vant` 2.x+（React 版）。

2. **安装依赖**：`cd miniprogram && npm install`

3. **配置 `project.config.json`**：将 `packNpmManually` 改为 `true`，添加 `packNpmRelationList`：
   ```json
   "packNpmManually": true,
   "packNpmRelationList": [
     {
       "packageJsonPath": "./package.json",
       "miniprogramNpmDistDir": "./"
     }
   ]
   ```

4. **微信开发者工具构建**：点击 "工具 > 构建 npm"，生成 `miniprogram_npm/` 目录。

5. **后续维护**：每次 `npm install` 或变更依赖后，需重新执行"构建 npm"。

## 设计规范（Design Tokens）

通过 Vant 的 `--van-*` 前缀 CSS 变量覆盖主题。在 `app.wxss` 的 `page` 选择器中设置：

```css
page {
  /* 品牌色 */
  --van-primary-color: #2563eb;
  --van-success-color: #10b981;
  --van-warning-color: #f59e0b;
  --van-danger-color: #ef4444;

  /* 字号 */
  --van-font-size-xs: 22rpx;
  --van-font-size-sm: 24rpx;
  --van-font-size-md: 28rpx;
  --van-font-size-lg: 32rpx;
  --van-font-size-xl: 36rpx;

  /* 圆角 */
  --van-border-radius-sm: 8rpx;
  --van-border-radius-md: 12rpx;
  --van-border-radius-lg: 16rpx;

  /* 按钮 */
  --van-button-border-radius: 12rpx;
  --van-button-normal-height: 88rpx;
  --van-button-font-size: 28rpx;
  --van-button-primary-background: #2563eb;
  --van-button-primary-border-color: #2563eb;
  --van-button-danger-background: #fee2e2;
  --van-button-danger-border-color: #fee2e2;
  --van-button-danger-color: #b91c1c;
  --van-button-default-background: #f3f4f6;
  --van-button-default-border-color: #f3f4f6;
  --van-button-default-color: #374151;

  /* 单元格 */
  --van-cell-group-background: transparent;
  --van-cell-background: transparent;
  --van-cell-border-color: #e5e7eb;
  --van-cell-font-size: 30rpx;
  --van-cell-label-font-size: 24rpx;
  --van-cell-label-color: #6b7280;

  /* 输入框 */
  --van-field-label-color: #374151;
  --van-field-input-text-color: #111827;
  --van-field-placeholder-text-color: #9ca3af;
  --van-field-border-color: #d1d5db;
  --van-field-background: #f9fafb;

  /* 标签 */
  --van-tag-border-radius: 4rpx;
  --van-tag-font-size: 22rpx;
  --van-tag-padding: 4rpx 12rpx;

  /* 弹窗 */
  --van-dialog-border-radius: 16rpx;
  --van-dialog-header-font-weight: 700;
  --van-dialog-message-font-size: 28rpx;

  /* 徽标 */
  --van-badge-background: #ef4444;
  --van-badge-font-size: 20rpx;

  /* 空状态 */
  --van-empty-description-color: #9ca3af;
  --van-empty-description-font-size: 28rpx;
}
```

这些变量直接覆盖 Vant 组件的内部样式，无需额外的自定义 CSS。

## TabBar 策略

**保持原生 tabBar，不使用 Vant TabBar。**

- 当前 2 个 Tab：「我的任务」和「个人设置」，数量和页面不变
- 原生 tabBar 不受 CSS 变量影响，颜色通过 `app.json` 的 `tabBar.color` / `tabBar.selectedColor` 配置
- 4 个图标文件（`assets/icons/`）保持不变
- 不创建 `custom-tab-bar/` 目录
- Vant 的 `--van-tabbar-*` 变量仅预留给未来可能的自定义 TabBar，当前不生效

## Vant 组件映射

| 当前自定义 | 替换为 Vant 组件 | 说明 |
|------------|-----------------|------|
| `<button class="primary">` | `<van-button type="primary">` | 直接替换 |
| `<button class="secondary">` | `<van-button type="default">` | 默认样式 |
| `<button class="danger">` | `<van-button type="danger">` | 直接替换 |
| `<button class="ghost">` | `<van-button type="default" plain>` | 朴素按钮 |
| `<input>` / `<textarea>` | `<van-field>` | 内置标签、校验 |
| `.dialog-mask` + `.dialog` | `<van-dialog>` | 内置遮罩和确认/取消 |
| `.status-tag` | `<van-tag type="warning/primary/success">` | 状态标签 |
| `.badge` | `<van-badge>` | 数字徽标 |
| `.empty` | `<van-empty>` | 空状态插图 |
| 加载文字 | `<van-loading vertical>` | 加载动画 |

**不替换的自定义类**（Vant 无等价组件）：
- `.page` / `.section` / `.title` — 布局容器
- `.list-item` / `.item-title` / `.item-meta` — 自定义列表布局
- `.field` / `.label` — 只读信息展示（非表单输入）
- `.muted` / `.row` / `.between` — 辅助工具类

## 现有全局类处置清单

### app.wxss — 删除 7 个，保留 11 个

| 类 | 处置 | 原因 |
|----|------|------|
| `page` | 保留 | 全局页面样式，所有页面使用 |
| `.page` | 保留 | 页面容器 padding |
| `.section` | 保留 | 卡片容器，Vant 无等价组件 |
| `.title` | 保留 | 区域标题，5 个页面均使用 |
| `.muted` | 保留 | 辅助文字颜色 |
| `.row` | 保留 | flex 行布局 |
| `.between` | 保留 | flex 两端对齐 |
| `.field` | 保留 | 只读信息字段容器 |
| `.label` | 保留 | 只读标签文字 |
| `.list-item` | 保留 | 自定义列表项 |
| `.item-title` | 保留 | 列表项标题 |
| `.item-meta` | 保留 | 列表项元信息 |
| `input, textarea` | **删除** | 被 `van-field` 替代 |
| `button` | **删除** | 被 `van-button` 替代 |
| `.primary` | **删除** | 被 `van-button type="primary"` 替代 |
| `.secondary` | **删除** | 被 `van-button type="default"` 替代 |
| `.danger` | **删除** | 被 `van-button type="danger"` 替代 |
| `.ghost` | **删除** | 被 `van-button plain` 替代 |
| `.empty` | **删除** | 被 `van-empty` 替代 |

### 页面专属类处置

| 页面 | 类 | 处置 |
|------|-----|------|
| login | `.login-page` / `.login-card` | 保留（布局），圆角改为 16rpx |
| tasks/index | `.bell-btn` / `.bell-icon` | 保留（无 Vant 等价） |
| tasks/index | `.badge` / `.status-tag/*` | **删除**（van-badge / van-tag 替代） |
| tasks/detail | `.section-title` / `.board-info` / `.table-*` / `.col-*` / `.action-bar` | 保留（自定义表格布局） |
| tasks/detail | `.status-tag/*` | **删除**（van-tag 替代） |
| profile | `.dialog-mask` / `.dialog` | **删除**（van-dialog 替代） |
| notifications | `.dot` | **删除**（van-badge dot 模式替代） |

## 渐进迁移策略

### 阶段一：构建链路 + 登录页验证

目标：建立 npm 基础设施，用最简单的登录页验证 Vant 集成全链路。

1. 创建 `package.json`，`npm install @vant/weapp`
2. 配置 `project.config.json` 的 `packNpmManually`
3. 微信开发者工具执行"构建 npm"
4. `app.wxss` 注入 `--van-*` 主题变量（保留所有现有类）
5. 登录页引入 `van-field` + `van-button`，替换原生 input 和 button
6. 验证：登录流程正常、Vant 组件渲染正确

### 阶段二：核心页面迁移

目标：迁移优先级最高的 3 个页面。

1. **任务列表**：`van-tag`（状态标签）、`van-badge`（通知徽标）、`van-empty`（空状态）、`van-loading`（加载态）
2. **任务详情**：`van-tag`、`van-button`（操作按钮）、`van-loading`
3. **个人中心**：`van-dialog`（密码修改弹窗）、`van-field`（弹窗内表单）、`van-button`

每个页面迁移后独立验证，确保功能不退化。

### 阶段三：通知页 + 清理

1. **通知页**：`van-button`（全部已读）、`van-empty`、`van-loading`
2. **清理 app.wxss**：删除 7 个被替代的全局类
3. **清理页面 wxss**：删除重复的 `.status-tag` 类（tasks/index + tasks/detail）、删除 `.dialog-mask`/`.dialog`（profile）、删除 `.badge`/`.dot`
4. 全量回归验证

## 事件处理注意事项

Vant 组件的事件绑定与原生组件不同：

| 对比 | 原生 | Vant |
|------|------|------|
| 事件绑定 | `bindinput` | `bind:input`（带冒号） |
| 取值方式 | `e.detail.value` | `e.detail`（直接是值） |

所有 `onInput` 处理函数需从 `e.detail.value` 改为 `e.detail`。

## 验证方案

每个阶段完成后验证：

1. **阶段一**：微信开发者工具预览登录页，Vant field/button 渲染正常，登录流程通过
2. **阶段二**：任务列表状态标签、通知徽标、空状态显示正常；任务详情操作按钮交互正常；个人中心密码修改弹窗流程通过
3. **阶段三**：通知页功能正常；app.wxss 精简后无样式丢失；全页面回归通过
