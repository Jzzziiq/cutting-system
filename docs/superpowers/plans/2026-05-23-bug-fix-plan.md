# Bug Fix Implementation Plan: 订单/生产任务 + 排样核心流程

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 16 bugs (3 Critical, 4 High, 6 Medium, 3 Low) in order/production-task and layout-core-workflow modules.

**Architecture:** Fixes grouped by root cause: (1) tenant filtering breaks cross-user operations, (2) saveLayoutInput drops fields, (3) frontend UX/dataflow, (4) order search. Each task is a self-contained change.

**Tech Stack:** Java 17, Spring Boot 3.5.11, MyBatis-Plus 3.5.x, Vue 3, Element Plus

---

## File Map

| File | Change Type | Tasks |
|------|-------------|-------|
| `TProductionTaskServiceImpl.java` | Modify | 1 |
| `TProductionTaskMapper.java` | Modify | 1 |
| `ProductionTaskController.java` | Modify | 1 |
| `OrderController.java` | Modify | 1, 7 |
| `ProductionKanbanView.vue` | Modify | 2 |
| `LayoutInputSaveDTO.java` | Modify | 3 |
| `TOrderServiceImpl.java` | Modify | 3, 4 |
| `ReadDataUtil.java` | Modify | 5 |
| `useBoardWorkpieceGroups.js` | Modify | 6 |
| `DataInputView.vue` | Modify | 6, 9 |
| `LayoutWorkbenchView.vue` | Modify | 8 |
| `LayoutResultController.java` | Modify | 10 |
| `useLayoutDataLoader.js` | Modify | 11, 12 |
| `QueryDTO.java` | Modify | 13 |

---

## Task 1: Fix tenant filtering for production tasks and order deletion

**Files:**
- Modify: `src/main/java/com/cutting/cuttingsystem/service/impl/TProductionTaskServiceImpl.java`
- Modify: `src/main/java/com/cutting/cuttingsystem/mapper/TProductionTaskMapper.java`
- Modify: `src/main/java/com/cutting/cuttingsystem/controller/ProductionTaskController.java`
- Modify: `src/main/java/com/cutting/cuttingsystem/controller/OrderController.java`

**Bug refs:** C1, C2, C3

- [ ] **Step 1: Add `selectAllIgnoreTenant` to TProductionTaskMapper**

The mapper already has `selectByIdIgnoreTenant` but no method to list all tasks. Add it after line 49:

```java
@InterceptorIgnore(tenantLine = "true")
@Select("""
        SELECT *
        FROM t_production_task
        ORDER BY create_time DESC
        """)
List<TProductionTask> selectAllIgnoreTenant();
```

Also add an ignore-tenant delete method (needed for controller delete):

```java
@InterceptorIgnore(tenantLine = "true")
@Update("""
        DELETE FROM t_production_task
        WHERE task_id = #{taskId}
        """)
int deleteByIdIgnoreTenant(@Param("taskId") Long taskId);
```

- [ ] **Step 2: Fix `kanbanData()` in TProductionTaskServiceImpl**

Replace line 183:
```java
List<TProductionTask> all = list(new QueryWrapper<TProductionTask>().orderByDesc("create_time"));
```
With:
```java
List<TProductionTask> all = baseMapper.selectAllIgnoreTenant();
```

- [ ] **Step 3: Fix `getTaskDetail()` in TProductionTaskServiceImpl**

Replace line 54:
```java
TProductionTask task = getById(taskId);
```
With:
```java
TProductionTask task = baseMapper.selectByIdIgnoreTenant(taskId);
```

- [ ] **Step 4: Fix `updateTask()` in TProductionTaskServiceImpl**

Replace line 71:
```java
TProductionTask task = getById(taskId);
```
With:
```java
TProductionTask task = baseMapper.selectByIdIgnoreTenant(taskId);
```

- [ ] **Step 5: Fix `assignTask()` in TProductionTaskServiceImpl**

Replace line 86:
```java
TProductionTask task = getById(taskId);
```
With:
```java
TProductionTask task = baseMapper.selectByIdIgnoreTenant(taskId);
```

- [ ] **Step 6: Fix `transitionStatus()` in TProductionTaskServiceImpl**

Replace line 126:
```java
TProductionTask task = getById(taskId);
```
With:
```java
TProductionTask task = baseMapper.selectByIdIgnoreTenant(taskId);
```

- [ ] **Step 7: Fix `deleteById` in ProductionTaskController**

Replace lines 132-134:
```java
boolean removed = productionTaskService.removeById(id);
return removed ? Result.success() : Result.error("删除失败");
```
With:
```java
int deleted = productionTaskService.getBaseMapper().deleteByIdIgnoreTenant(id);
return deleted > 0 ? Result.success() : Result.error("删除失败，任务不存在");
```

- [ ] **Step 8: Fix `deleteById` in OrderController**

Add to `TOrderMapper.java` an ignore-tenant delete method (check if it already exists first). If not, add:

```java
@InterceptorIgnore(tenantLine = "true")
@Update("DELETE FROM t_order WHERE order_id = #{orderId}")
int deleteByIdIgnoreTenant(@Param("orderId") Long orderId);
```

Then replace lines 87-88 in `OrderController.java`:
```java
boolean removed = orderService.removeById(id);
return removed ? Result.success() : Result.error("delete order failed");
```
With:
```java
int deleted = orderService.getBaseMapper().deleteByIdIgnoreTenant(id);
return deleted > 0 ? Result.success() : Result.error("删除失败，订单不存在");
```

- [ ] **Step 9: Run existing tests**

Run: `mvn test -Dtest=ProductionTaskModuleTest,OrderModuleTest -Dmaven.repo.local=target/.m2`
Expected: All tests pass (existing tests use admin user so tenant filtering was not blocking them).

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/cutting/cuttingsystem/mapper/TProductionTaskMapper.java \
        src/main/java/com/cutting/cuttingsystem/mapper/TOrderMapper.java \
        src/main/java/com/cutting/cuttingsystem/service/impl/TProductionTaskServiceImpl.java \
        src/main/java/com/cutting/cuttingsystem/controller/ProductionTaskController.java \
        src/main/java/com/cutting/cuttingsystem/controller/OrderController.java
git commit -m "fix: use ignore-tenant queries for production task CRUD and order/task deletion"
```

---

## Task 2: Fix kanban drag-and-drop illegal state transitions

**Files:**
- Modify: `frontend/src/views/ProductionKanbanView.vue`

**Bug ref:** M3

- [ ] **Step 1: Add state machine validation to `onDrop`**

Replace lines 250-257:
```javascript
function onDrop(e, targetStatus) {
  e.preventDefault();
  const raw = e.dataTransfer.getData('text/plain');
  if (!raw) return;
  const { taskId, status: fromStatus } = JSON.parse(raw);
  if (fromStatus === targetStatus) return;
  doTransition(taskId, targetStatus);
}
```
With:
```javascript
function onDrop(e, targetStatus) {
  e.preventDefault();
  const raw = e.dataTransfer.getData('text/plain');
  if (!raw) return;
  const { taskId, status: fromStatus } = JSON.parse(raw);
  if (fromStatus === targetStatus) return;
  const validTransitions = { 0: [1], 1: [2], 2: [] };
  if (!validTransitions[fromStatus]?.includes(targetStatus)) {
    ElMessage.warning('该状态转换不被允许');
    return;
  }
  doTransition(taskId, targetStatus);
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/ProductionKanbanView.vue
git commit -m "fix: validate state transitions on kanban drag-and-drop"
```

---

## Task 3: Add missing fields to LayoutInputSaveDTO

**Files:**
- Modify: `src/main/java/com/cutting/cuttingsystem/entitys/DTO/LayoutInputSaveDTO.java`

**Bug ref:** H1

- [ ] **Step 1: Extend ItemDTO with missing fields**

Replace lines 25-38:
```java
@Data
public static class ItemDTO {
    private String partName;
    @NotNull(message = "length must not be null")
    @Positive(message = "length must be greater than 0")
    private Integer length;
    @NotNull(message = "width must not be null")
    @Positive(message = "width must be greater than 0")
    private Integer width;
    @NotNull(message = "quantity must not be null")
    @Positive(message = "quantity must be greater than 0")
    private Integer quantity;
    private String remark;
}
```
With:
```java
@Data
public static class ItemDTO {
    private String partName;
    private String partCode;
    @NotNull(message = "length must not be null")
    @Positive(message = "length must be greater than 0")
    private Integer length;
    @NotNull(message = "width must not be null")
    @Positive(message = "width must be greater than 0")
    private Integer width;
    @NotNull(message = "quantity must not be null")
    @Positive(message = "quantity must be greater than 0")
    private Integer quantity;
    private Integer isTexture;
    private Integer allowRotation;
    private Integer edgeLeft;
    private Integer edgeRight;
    private Integer edgeTop;
    private Integer edgeBottom;
    private String remark;
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/cutting/cuttingsystem/entitys/DTO/LayoutInputSaveDTO.java
git commit -m "fix: add partCode, isTexture, edge fields to LayoutInputSaveDTO.ItemDTO"
```

---

## Task 4: Fix saveLayoutInput to preserve all fields + fix getLayoutInput hardcoded config

**Files:**
- Modify: `src/main/java/com/cutting/cuttingsystem/service/impl/TOrderServiceImpl.java`

**Bug refs:** H1 (save), H3 (hardcoded config)

- [ ] **Step 1: Fix `saveLayoutInput` to set all fields**

Replace lines 282-292:
```java
for (LayoutInputSaveDTO.ItemDTO itemDTO : group.getItems()) {
    TOrderItem item = new TOrderItem();
    item.setOrderId(orderId);
    item.setBoardId(group.getBoardId());
    item.setThickness(thickness);
    item.setPartName(itemDTO.getPartName());
    item.setLength(itemDTO.getLength());
    item.setWidth(itemDTO.getWidth());
    item.setQuantity(itemDTO.getQuantity());
    item.setRemark(itemDTO.getRemark());
    items.add(item);
}
```
With:
```java
for (LayoutInputSaveDTO.ItemDTO itemDTO : group.getItems()) {
    TOrderItem item = new TOrderItem();
    item.setOrderId(orderId);
    item.setBoardId(group.getBoardId());
    item.setThickness(thickness);
    item.setPartName(itemDTO.getPartName());
    item.setPartCode(itemDTO.getPartCode());
    item.setLength(itemDTO.getLength());
    item.setWidth(itemDTO.getWidth());
    item.setQuantity(itemDTO.getQuantity());
    item.setIsTexture(itemDTO.getIsTexture());
    item.setAllowRotation(itemDTO.getAllowRotation());
    item.setEdgeLeft(itemDTO.getEdgeLeft());
    item.setEdgeRight(itemDTO.getEdgeRight());
    item.setEdgeTop(itemDTO.getEdgeTop());
    item.setEdgeBottom(itemDTO.getEdgeBottom());
    item.setRemark(itemDTO.getRemark());
    items.add(item);
}
```

- [ ] **Step 2: Fix `getLayoutInput` to not hardcode algorithm config**

Replace lines 255-258:
```java
LayoutInputVO.AlgorithmConfigVO algoConfig = new LayoutInputVO.AlgorithmConfigVO();
algoConfig.setGapDistance(3);
algoConfig.setAllowRotation(!hasTextureItems);
vo.setAlgorithmConfig(algoConfig);
```
With:
```java
LayoutInputVO.AlgorithmConfigVO algoConfig = new LayoutInputVO.AlgorithmConfigVO();
algoConfig.setGapDistance(null);
algoConfig.setAllowRotation(hasTextureItems ? false : null);
vo.setAlgorithmConfig(algoConfig);
```

This way the frontend's `?? settings.gapDistance ?? 3` will correctly fall through to user settings. For `allowRotation`, when `hasTextureItems` is true, the explicit `false` blocks rotation (since `false ?? x` returns `false`). When no texture, `null` defers to user settings.

- [ ] **Step 3: Run tests**

Run: `mvn test -Dtest=OrderModuleTest -Dmaven.repo.local=target/.m2`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/cutting/cuttingsystem/service/impl/TOrderServiceImpl.java
git commit -m "fix: preserve all order item fields in saveLayoutInput; stop hardcoding algorithm config"
```

---

## Task 5: Fix ReadDataUtil UUID replacement destroying piece IDs

**Files:**
- Modify: `src/main/java/com/cutting/cuttingsystem/util/ReadDataUtil.java`

**Bug ref:** H2

- [ ] **Step 1: Fix `getInstanceFromJson` to preserve original IDs**

Replace lines 43-50:
```java
List<Square> squareList = new ArrayList<>();
for (com.cutting.cuttingsystem.entitys.algorithm.Square square : dto.getSquareList()) {
    squareList.add(new Square(
            UUID.randomUUID().toString(),
            square.getL(),
            square.getW()
    ));
}
```
With:
```java
List<Square> squareList = new ArrayList<>();
for (com.cutting.cuttingsystem.entitys.algorithm.Square square : dto.getSquareList()) {
    String id = square.getId() != null ? square.getId() : UUID.randomUUID().toString();
    squareList.add(new Square(id, square.getL(), square.getW()));
}
```

- [ ] **Step 2: Fix `getSolution` to reuse original IDs instead of generating new UUIDs**

Replace lines 59-62:
```java
List<Square> remainingSquares = new ArrayList<>();
for (Square sq : originInstance.getSquareList()) {
    remainingSquares.add(new Square(UUID.randomUUID().toString(), sq.getL(), sq.getW()));
}
```
With:
```java
List<Square> remainingSquares = new ArrayList<>();
for (Square sq : originInstance.getSquareList()) {
    remainingSquares.add(new Square(sq.getId(), sq.getL(), sq.getW()));
}
```

- [ ] **Step 3: Run algorithm tests**

Run: `mvn test -Dtest=AlgorithmUnitTest -Dmaven.repo.local=target/.m2`
Expected: All 7 tests pass (the algorithm doesn't depend on ID format).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/cutting/cuttingsystem/util/ReadDataUtil.java
git commit -m "fix: preserve original square IDs in ReadDataUtil instead of replacing with UUIDs"
```

---

## Task 6: Fix frontend buildSavePayload to carry all fields + fix DataInputView hardcoded values

**Files:**
- Modify: `frontend/src/composables/useBoardWorkpieceGroups.js`
- Modify: `frontend/src/views/cutting/DataInputView.vue`

**Bug refs:** H1 (frontend side), M2

- [ ] **Step 1: Extend `buildSavePayload` to include missing fields**

Replace lines 298-306 in `useBoardWorkpieceGroups.js`:
```javascript
.map(item => ({
  partName: item.itemName || '',
  length: Number(item.length),
  width: Number(item.width),
  quantity: Number(item.quantity),
  remark: item.notes || ''
}))
```
With:
```javascript
.map(item => ({
  partName: item.itemName || '',
  partCode: item.partCode || '',
  length: Number(item.length),
  width: Number(item.width),
  quantity: Number(item.quantity),
  isTexture: item.isTexture ?? null,
  allowRotation: item.allowRotation ?? null,
  edgeLeft: item.edgeLeft ?? null,
  edgeRight: item.edgeRight ?? null,
  edgeTop: item.edgeTop ?? null,
  edgeBottom: item.edgeBottom ?? null,
  remark: item.notes || ''
}))
```

- [ ] **Step 2: Fix `onConfirm` in DataInputView.vue to use dynamic values**

Find the `submitAlgorithmJob` call (around lines 170-176). Replace:
```javascript
const result = await submitAlgorithmJob({
    L: job.board.length,
    W: job.board.width,
    isRotateEnable: true,
    gapDistance: 3,
    squareList: job.squareList
});
```
With:
```javascript
const hasTexture = job.squareList.some(s => s.isTexture === 1);
const result = await submitAlgorithmJob({
    L: job.board.length,
    W: job.board.width,
    isRotateEnable: !hasTexture,
    gapDistance: settings.gapDistance ?? 3,
    squareList: job.squareList
});
```

Note: Check if `settings` is accessible in this scope. If `onConfirm` doesn't have access to `settings`, the gapDistance default of 3 is acceptable as a fallback — the workbench's `loadFromOrder` path handles user settings correctly after Task 4's fix.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/composables/useBoardWorkpieceGroups.js \
        frontend/src/views/cutting/DataInputView.vue
git commit -m "fix: carry partCode/isTexture/edge fields in save payload; use dynamic isRotateEnable"
```

---

## Task 7: Fix order search and statusLabel

**Files:**
- Modify: `src/main/java/com/cutting/cuttingsystem/entitys/DTO/QueryDTO.java`
- Modify: `src/main/java/com/cutting/cuttingsystem/controller/OrderController.java`

**Bug refs:** H4, L2

- [ ] **Step 1: Add `search` field to QueryDTO**

Add after line 17 (`private Integer pageSize;`):
```java
private String search;
```

- [ ] **Step 2: Fix `pageQuery` to apply search filter and statusLabel**

Replace lines 38-43 in `OrderController.java`:
```java
@GetMapping
public Result pageQuery(@Valid QueryDTO query) {
    IPage<TOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
    IPage<TOrderVO> orderVOPage = orderService.page(page).convert(this::toVO);
    return Result.success(orderVOPage);
}
```
With:
```java
@GetMapping
public Result pageQuery(@Valid QueryDTO query) {
    IPage<TOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
    QueryWrapper<TOrder> qw = new QueryWrapper<>();
    if (query.getSearch() != null && !query.getSearch().isBlank()) {
        qw.and(w -> w.like("order_no", query.getSearch())
                .or().like("process_name", query.getSearch())
                .or().like("customer_name", query.getSearch()));
    }
    qw.orderByDesc("create_time");
    IPage<TOrderVO> orderVOPage = orderService.page(page, qw).convert(this::toVO);
    return Result.success(orderVOPage);
}
```

Add the missing import at the top of OrderController.java:
```java
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
```

- [ ] **Step 3: Fix `toVO` to include statusLabel**

Replace lines 107-111:
```java
private TOrderVO toVO(TOrder order) {
    TOrderVO orderVO = new TOrderVO();
    BeanUtils.copyProperties(order, orderVO);
    return orderVO;
}
```
With:
```java
private TOrderVO toVO(TOrder order) {
    TOrderVO orderVO = new TOrderVO();
    BeanUtils.copyProperties(order, orderVO);
    if (order.getOrderStatus() != null) {
        orderVO.setStatusLabel(OrderStatus.fromCode(order.getOrderStatus()).getLabel());
    }
    return orderVO;
}
```

- [ ] **Step 4: Run tests**

Run: `mvn test -Dtest=OrderModuleTest -Dmaven.repo.local=target/.m2`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/cutting/cuttingsystem/entitys/DTO/QueryDTO.java \
        src/main/java/com/cutting/cuttingsystem/controller/OrderController.java
git commit -m "fix: add order search filter and statusLabel to pageQuery"
```

---

## Task 8: Fix LayoutWorkbenchView false success message

**Files:**
- Modify: `frontend/src/views/cutting/LayoutWorkbenchView.vue`

**Bug ref:** M1

- [ ] **Step 1: Check `loadFromOrder` return value**

Find the block around lines 63-69:
```javascript
if (currentLayoutInput.value?.groups?.length) {
    await loadFromOrder(orderId, settings);
    ElMessage.success('排版计算完成');
    return;
}
```

Replace with:
```javascript
if (currentLayoutInput.value?.groups?.length) {
    const ok = await loadFromOrder(orderId, settings);
    if (ok !== false) {
        ElMessage.success('排版计算完成');
    }
    return;
}
```

Check `loadFromOrder` in `useLayoutDataLoader.js` — it returns `false` when orderId <= 0 or when the API returns no groups. After Task 4, it should also return `false` on API error. If it currently returns `undefined` on success, the `!== false` check handles that correctly.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/cutting/LayoutWorkbenchView.vue
git commit -m "fix: check loadFromOrder result before showing success message"
```

---

## Task 9: Bind OffcutPanel v-model in DataInputView

**Files:**
- Modify: `frontend/src/views/cutting/DataInputView.vue`

**Bug ref:** M4

- [ ] **Step 1: Add selectedOffcuts ref and bind v-model**

Near the top of the script (around other refs), add:
```javascript
const selectedOffcuts = ref([]);
```

Replace lines 252-254:
```html
<OffcutPanel
  :selected-boards="boardGroups.map(g => g.board)"
/>
```
With:
```html
<OffcutPanel
  v-model="selectedOffcuts"
  :selected-boards="boardGroups.map(g => g.board)"
/>
```

Note: The offcuts are now captured in `selectedOffcuts`. A full integration (passing offcuts to the algorithm as pre-placed pieces) is a separate feature. This fix ensures the selection state is at least tracked in the parent component.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/cutting/DataInputView.vue
git commit -m "fix: bind OffcutPanel v-model to track selected offcuts"
```

---

## Task 10: Fix LayoutResultController orderName showing orderNo

**Files:**
- Modify: `src/main/java/com/cutting/cuttingsystem/controller/LayoutResultController.java`

**Bug ref:** M5

- [ ] **Step 1: Fix `toVO` to use processName for orderName**

Replace line 102:
```java
vo.setOrderName(order.getOrderNo());
```
With:
```java
vo.setOrderName(order.getProcessName());
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/cutting/cuttingsystem/controller/LayoutResultController.java
git commit -m "fix: set orderName to processName instead of orderNo in layout result VO"
```

---

## Task 11: Fix draft utilization rate calculation (area-weighted)

**Files:**
- Modify: `frontend/src/composables/useLayoutDataLoader.js`

**Bug ref:** M6

- [ ] **Step 1: Fix totalRate to use area-weighted average**

Replace lines 113-115:
```javascript
const totalRate = boardResults.value.length
  ? boardResults.value.reduce((s, r) => s + (r.bestRate || 0), 0) / boardResults.value.length
  : 0;
```
With:
```javascript
let totalRate = 0;
if (boardResults.value.length) {
  let weightedSum = 0;
  let totalArea = 0;
  for (const r of boardResults.value) {
    const board = r.board || {};
    const area = (board.length || 0) * (board.width || 0);
    weightedSum += (r.bestRate || 0) * area;
    totalArea += area;
  }
  totalRate = totalArea > 0 ? weightedSum / totalArea : 0;
}
```

This matches the pattern in `useLayoutRunner.js` `summarizeBoardResults` (line 64-65): `result.board.length * result.board.width`.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/composables/useLayoutDataLoader.js
git commit -m "fix: use area-weighted average for draft utilization rate"
```

---

## Task 12: Fix stale data when returning to layout workbench

**Files:**
- Modify: `frontend/src/composables/useLayoutDataLoader.js`

**Bug ref:** L1

- [ ] **Step 1: Clear lastRouteLoadKey on activation**

Find the `loadFromRoute` function (line 133). Add a `resetRouteKey` function and export it:

```javascript
function resetRouteKey() {
  lastRouteLoadKey.value = '';
}
```

Make sure `resetRouteKey` is returned from the composable's return statement.

- [ ] **Step 2: Call resetRouteKey in LayoutWorkbenchView's onActivated**

In `LayoutWorkbenchView.vue`, find the `onActivated` call (around line 225). It should already call `loadFromRoute(settings)`. Before that call, add:

```javascript
resetRouteKey();
```

So the full block becomes:
```javascript
onActivated(() => {
  resetRouteKey();
  loadFromRoute(settings);
});
```

Destructure `resetRouteKey` from the composable alongside the other returned values.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/composables/useLayoutDataLoader.js \
        frontend/src/views/cutting/LayoutWorkbenchView.vue
git commit -m "fix: clear route load key on workbench activation to refresh stale data"
```

---

## Task 13: Run full test suite and verify

- [ ] **Step 1: Run all backend tests**

Run: `mvn test -Dmaven.repo.local=target/.m2`
Expected: All tests pass.

- [ ] **Step 2: Verify frontend builds**

Run: `cd frontend && npm run build`
Expected: Build succeeds with no errors.

- [ ] **Step 3: Manual smoke test checklist**

1. Login as admin → Production Kanban → create task → assign to another user → login as that user → kanban shows the task
2. Drag a PENDING task to COMPLETED column → shows "该状态转换不被允许" warning
3. Order list → search by order number → results filter correctly
4. Split order → Data Input → save → reload → partCode and edge fields preserved
5. Layout Workbench → change gapDistance in settings → start layout → algorithm uses the new value
6. Layout history → orderName shows process name, not order number

- [ ] **Step 4: Final commit with all remaining changes**

```bash
git add -A
git status
git commit -m "fix: address remaining test and build verification"
```
