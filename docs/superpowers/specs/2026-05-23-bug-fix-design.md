# Bug Fix Design: 订单/生产任务 + 排样核心流程

日期: 2026-05-23
状态: 待审批

## 背景

用户反馈多个功能"实现了但用不了"：数据未正确保存、功能无法触发、页面跳转异常、数据隔离问题。
代码审查发现 16 个独立 bug（3 Critical, 4 High, 6 Medium, 3 Low），按根因分 4 组修复。

---

## 第 1 组：租户过滤导致跨用户操作失效

**涉及 bug**: C1, C2, C3, M3

**根因**: `TenantLineInnerInterceptor` 自动给查询加 `WHERE user_id = currentUser`，但生产任务需要被分配人可见、管理员可操作。

### 修改清单

| 文件 | 改动 |
|------|------|
| `TProductionTaskServiceImpl.java` | `kanbanData()` 改用 `baseMapper.selectAllIgnoreTenant()`；`getTaskDetail()`/`updateTask()`/`assignTask()`/`transitionStatus()` 改用 `baseMapper.selectByIdIgnoreTenant(taskId)` |
| `TProductionTaskMapper.java` | 确认/补充 `selectAllIgnoreTenant()` 方法（`@InterceptorIgnore(tenantLine = "true")` + `@Select`） |
| `ProductionTaskController.java` | `deleteTask()` 改用 ignore-tenant 删除 |
| `OrderController.java` | `deleteOrder()` 改用 ignore-tenant 删除，失败时返回明确错误 |
| `ProductionKanbanView.vue` | `onDrop` 增加客户端状态机校验 `{0:[1], 1:[2], 2:[]}` |

### 注意事项

- ignore-tenant 查询需配合业务层权限校验（当前系统 admin/operator 均可操作，后续 F-010 再细化）
- 看板查询改为全量后需确认性能（当前数据量小，无问题）

---

## 第 2 组：数据保存丢失关键字段

**涉及 bug**: H1, H2, H3, M2

**根因**: `saveLayoutInput` 先删后建但 DTO 缺字段；`ReadDataUtil` 用 UUID 替换原始 ID；`getLayoutInput` 硬编码算法参数。

### 修改清单

| 文件 | 改动 |
|------|------|
| `LayoutInputSaveDTO.java` | `ItemDTO` 增加 `partCode`, `isTexture`, `allowRotation`, `edgeLeft`, `edgeRight`, `edgeTop`, `edgeBottom` |
| `TOrderServiceImpl.java` | `saveLayoutInput()` 重建 item 时设置新增字段；`getLayoutInput()` 不再硬编码 gapDistance/allowRotation，改为仅在无值时给默认值 |
| `ReadDataUtil.java` | `getInstanceFromJson()` 保留原始 square ID（null 时才用 UUID）；`getSolution()` 中复用 `sq.getId()` |
| `useBoardWorkpieceGroups.js` | `buildSavePayload()` 输出携带 partCode/isTexture/edge 等字段 |
| `DataInputView.vue` | `onConfirm` 从 boardGroups 中检测 isTexture 决定 `isRotateEnable`，gapDistance 从设置读取 |

### 数据流（修复后）

```
拆单 → TOrderItem(partCode, isTexture, edge*) 保存到 DB
  ↓
getLayoutInput → 读取完整字段 → 返回给前端
  ↓
前端编辑 → buildSavePayload 携带全部字段
  ↓
saveLayoutInput → 重建 item 保留所有字段
  ↓
算法提交 → ReadDataUtil 保留原始 ID → 结果可追溯
```

---

## 第 3 组：前端 UX / 数据流问题

**涉及 bug**: M1, M4, M5, M6, L1, L2

### 修改清单

| 文件 | 改动 |
|------|------|
| `LayoutWorkbenchView.vue` | `onStartLayout` 检查 `loadFromOrder` 返回值，失败时不显示成功消息 |
| `DataInputView.vue` | `OffcutPanel` 绑定 `v-model`，将选中余料传入 `buildAlgorithmJobs` |
| `LayoutResultController.java` | `toVO()` 中 `orderName` 改为 `order.getProcessName()` |
| `useLayoutDataLoader.js` | `loadFromDraft` 的 totalRate 改为面积加权计算 |
| `useLayoutDataLoader.js` | `loadFromRoute` 在 `onActivated` 时清除 `lastRouteLoadKey` |
| `OrderController.java` | `toVO()` 补充 `statusLabel = OrderStatus.fromCode(...).getLabel()` |

---

## 第 4 组：订单搜索

**涉及 bug**: H4

### 修改清单

| 文件 | 改动 |
|------|------|
| `QueryDTO.java` | 增加 `search` 字段（String，可选） |
| `OrderController.java` | `pageQuery()` 当 search 非空时加 `QueryWrapper` 的 `like` 条件（order_no, process_name） |

---

## 验证方案

每组修复后按以下方式验证：

1. **第 1 组**: 创建任务 → 分配给其他用户 → 用被分配人登录 → 看板可见、可操作任务；拖拽非法状态应被拦截
2. **第 2 组**: 拆单 → 进入数据输入 → 保存 → 重新加载 → 确认 partCode/isTexture/edge 完整；算法结果中工件标签正确显示
3. **第 3 组**: 空 orderId 进入排版工作台 → 不应显示成功；选择余料 → 保存 → 确认余料参与算法；排版历史 orderName 显示正确
4. **第 4 组**: 看板创建任务弹窗 → 搜索订单号 → 结果应过滤

无法自动运行验证时，提供手动测试步骤。
