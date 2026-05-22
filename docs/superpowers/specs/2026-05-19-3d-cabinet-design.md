# 3D 柜体设计与拆单功能 — 设计规格文档

> 编写日期: 2026-05-19 | 修订日期: 2026-05-21 | 状态: MVP 0-3 已实现并按调研资料补充参数化建模约束，MVP 4 待实施
> 关联: 拓展路线图 第4层 "3D 排样预览" → 扩展为完整的 3D 柜体建模与拆单

---

## 一、功能概述

在现有柜门板材切割排版系统中新增 **3D 柜体设计** 功能。用户通过 Three.js 在浏览器中围绕同一个订单搭建多个柜体三维模型，全部建模确认后再统一拆单，自动提取板件切割尺寸、封边信息、铰链孔位，写入订单明细并进入现有排样流程。

**核心价值：** 打通"订单 → 多柜体设计 → 统一拆单 → 整单排样"全链路，替代传统的手工测量录入。

**调研落地结论：** 参考 `docs/user-manual/参考资料/柜门板材生产软件调研参考文档_格式修复版.md` 后，本功能第一阶段应对齐主流柜体生产软件的工作方式：以柜型模板和 W/D/H 参数驱动自动生成板件为主，左侧管理柜体/模板，中间显示 3D 预览，右侧承载当前柜体与选中板件属性。自由拖拽拼装保留为后续增强能力，不作为当前主流程前置条件。

---

## 二、需求决策汇总

| 维度 | 决策 |
|------|------|
| 建模方式 | 参数化模板优先（柜型 + W/D/H + 层板/门板数量）+ 3D 微调；自由拖拽拼装后置到 MVP 4 |
| 订单内流程 | 一个订单可维护多个柜体草稿，建模阶段不强制立即拆单；用户完成全屋柜体后再统一拆单 |
| 起步品类 | 平开门衣柜、开门地柜 |
| 板件生成约束 | 侧板按整柜高/深生成；顶板、底板、层板按内宽 `W - 2T` 放入两侧板之间；背板按内宽/内高生成，避免与柜体板重叠 |
| 界面组织 | 左侧订单柜体/模板库，中间 Three.js 画布，右侧当前柜体/选中板件/板材映射属性面板 |
| 拆单输出 | 板材清单 + 封边标注 + 铰链孔 + 柜体属性（板材类型/房间用途/工件编号） |
| 与现有系统关系 | 可选前置步骤，不取代 DataInputView 手工录入 |
| 平台 | 仅网页端，集成到现有 Vue 3 前端 |
| 板件库 | 系统内置固定类型（侧板/层板/门板/背板/顶板/底板），本期不做管理员扩展。材质类型复用现有 `TBoard` 板材管理 |
| 五金管理 | 不做，聚焦板件 |
| 封边处理 | 系统自动换算切割尺寸（每条封边扣除 1mm） |
| 预设模板 | 用户可保存设计为模板，系统自带官方预设 |
| 架构方案 | 前后端分离拆单：前端 Three.js 渲染交互，后端拆单服务计算切割尺寸/封边/孔位 |
| 数据表策略 | 新增 `cabinet_template`（预设模板表）和 `cabinet_order_item`（拆单辅助表），不改动 `TOrderItem` |

---

## 三、系统架构与模块边界

```
┌────────────────────────────────────────────────────────┐
│                 网页端 (Vue 3 + Three.js)                │
│  ┌───────────────────┐  ┌─────────────────────────────┐ │
│  │  3D 设计页 (新增)   │  │  现有页面（DataInput、排版等）│ │
│  │  Three.js 建模+交互 │  │                             │ │
│  │  预设向导+自由微调   │  │                             │ │
│  └────────┬──────────┘  └─────────────────────────────┘ │
└───────────┼────────────────────────────────────────────┘
            │ 柜体结构 JSON
┌───────────▼────────────────────────────────────────────┐
│                   后端 (Spring Boot)                      │
│  ┌───────────────────┐  ┌─────────────────────────────┐ │
│  │  拆单服务 (新增)    │  │  现有服务（订单/排样/生产等） │ │
│  │  - 封边尺寸换算     │  │                             │ │
│  │  - 铰链孔位计算     │  │                             │ │
│  │  - 工件编号生成     │  │                             │ │
│  │  - 模板管理 CRUD    │  │                             │ │
│  └────────┬──────────┘  └─────────────────────────────┘ │
│           │                                              │
│  ┌────────▼──────────┐                                  │
│  │  数据库 (MySQL)     │                                  │
│  │  - cabinet_template │                                 │
│  │  - cabinet_order_item│                                │
│  │  - (复用 TBoard)    │                                  │
│  └───────────────────┘                                  │
└──────────────────────────────────────────────────────────┘
```

### 新增后端模块

| 模块 | 职责 |
|------|------|
| `CabinetTemplateController` | 预设模板 CRUD + 列表查询 |
| `OrderSplitController` | 接收柜体 JSON → 返回拆分明细 / 确认写入 |
| `CabinetTemplateService` | 模板管理业务逻辑 |
| `OrderSplitService` | 拆单计算核心 |

### 新增前端模块

| 模块 | 职责 |
|------|------|
| `CabinetDesignView.vue` | 订单级 3D 设计页主容器，路由 `/cutting/cabinet-design` |
| 左侧面板组件 | 订单内柜体清单 + 预设列表 + 板件库拖拽源 |
| 中央画布组件 | Three.js 场景渲染与交互 |
| 右侧面板组件 | 选中板件属性编辑 + 当前柜体全局信息 |
| `useThreeScene` composable | Three.js 场景初始化、OrbitControls、Raycaster 交互封装 |

---

## 四、核心数据模型

### 4.1 柜体结构 JSON（前端 → 后端）

3D 设计页以订单为工作上下文。前端状态中维护订单级柜体草稿清单，允许用户在同一 `orderId` 下连续新增、复制、编辑多个柜体（如衣柜、电视柜、吊柜、酒柜、鞋柜、精品柜），不要求每完成一个柜体就立刻拆单：

```json
{
  "orderId": 123,
  "cabinetDrafts": [
    { "clientCabinetId": "cab-001", "cabinetJson": { "cabinet": {}, "boards": [] } },
    { "clientCabinetId": "cab-002", "cabinetJson": { "cabinet": {}, "boards": [] } }
  ],
  "activeCabinetId": "cab-001"
}
```

`cabinetJson` 仍表示单个柜体的结构 JSON，供现有拆单接口计算。统一拆单时，前端按 `cabinetDrafts` 顺序逐柜调用拆单能力，并把结果追加到同一个订单。

```json
{
  "cabinet": {
    "name": "主卧衣柜",
    "room": "主卧",
    "purpose": "衣物收纳",
    "width": 1200,
    "height": 2200,
    "depth": 600
  },
  "boards": [
    {
      "id": "b-001",
      "type": "side",
      "displayName": "左侧板",
      "boardId": 5,
      "designLength": 2200,
      "designWidth": 600,
      "thickness": 18,
      "position": { "x": -591, "y": 1100, "z": 0 },
      "rotation": { "x": 0, "y": 0, "z": 0 },
      "placementFace": "left",
      "connectedTo": [],
      "grain": "vertical",
      "edgeBanding": { "left": false, "right": false, "top": true, "bottom": true },
      "edgeRole": { "left": "靠墙侧", "right": "前口", "top": "上端", "bottom": "下端" },
      "hingeHoles": []
    },
    {
      "id": "b-002",
      "type": "side",
      "displayName": "右侧板",
      "boardId": 5,
      "designLength": 2200,
      "designWidth": 600,
      "thickness": 18,
      "position": { "x": 591, "y": 1100, "z": 0 },
      "rotation": { "x": 0, "y": 0, "z": 0 },
      "placementFace": "right",
      "connectedTo": [],
      "grain": "vertical",
      "edgeBanding": { "left": false, "right": false, "top": true, "bottom": true },
      "edgeRole": { "left": "靠墙侧", "right": "前口", "top": "上端", "bottom": "下端" },
      "hingeHoles": []
    },
    {
      "id": "b-003",
      "type": "door",
      "displayName": "左门板",
      "boardId": 6,
      "designLength": 2150,
      "designWidth": 400,
      "thickness": 18,
      "position": { "x": -391, "y": 1075, "z": 10 },
      "rotation": { "x": 0, "y": 0, "z": 0 },
      "placementFace": "front",
      "connectedTo": ["b-001"],
      "grain": "vertical",
      "edgeBanding": { "left": true, "right": true, "top": true, "bottom": true },
      "hingeHoles": [
        { "edge": "left", "count": 3, "spacing": "even", "diameter": 35, "depth": 12, "doorGap": 2, "edgeDistance": 22, "direction": "height", "opening": "left" }
      ]
    }
  ]
}
```

- `boardId`：指向 `TBoard.boardId`，关联板材类型。**官方模板**此字段为 `null`，改用 `materialSlot`（如 `cabinet_body`/`door`/`back`）表示板材角色，用户拆单前为每个 slot 选择实际板材，通过接口入参 `materialSlotBoardMap` 传入
- `materialSlot`：官方模板使用的板材角色占位，用户模板不使用。取值：`cabinet_body`（柜体板）、`door`（门板）、`back`（背板）

**官方模板板件示例**（使用 `materialSlot`，不含 `boardId`）：

```json
{
  "id": "b-001",
  "type": "side",
  "displayName": "左侧板",
  "materialSlot": "cabinet_body",
  "boardId": null,
  "designLength": 2200,
  "designWidth": 600,
  "thickness": 18,
  "position": { "x": -591, "y": 1100, "z": 0 },
  "rotation": { "x": 0, "y": 0, "z": 0 },
  "placementFace": "left",
  "connectedTo": [],
  "grain": "vertical",
  "edgeBanding": { "left": false, "right": false, "top": true, "bottom": true },
  "edgeRole": { "left": "靠墙侧", "right": "前口", "top": "上端", "bottom": "下端" },
  "hingeHoles": []
}
```

拆单请求中，前端已将 `materialSlot` 解析为具体 `boardId`（通过 `materialSlotBoardMap`），不再含 `materialSlot`。
- `position`：板件在 3D 空间中的位置，原点为柜体中心底部，单位 mm
- `rotation`：欧拉角旋转，单位弧度
- `placementFace`：板件的放置面，取值 `left/right/front/back/top/bottom`
- `connectedTo`：关联的板件 ID 列表，如门板通过铰链连接到侧板
- `edgeBanding`：对象结构，板件自身 2D 坐标系四边，`true` 表示该边需封边：
  ```json
  { "left": true, "right": true, "top": true, "bottom": false }
  ```
- `edgeRole`：对象结构，保存边的业务语义（不参与尺寸扣减）：
  ```json
  { "left": "靠墙侧", "right": "前口", "top": "上端", "bottom": "下端" }
  ```
- `hingeHoles`：
  - `edge`：铰链所在的门边
  - `count`：铰链数量
  - `spacing`：`even`（均分）或 `fixed`（固定位置）
  - `diameter`：铰链杯孔径，默认 35mm
  - `depth`：孔深，默认 12mm
  - `doorGap`：门缝间隙，默认 2mm
  - `edgeDistance`：铰链中心距门边距离，默认 22mm
  - `direction`：沿长度方向( `height` )还是宽度方向( `width` )排布
  - `opening`：开门方向 `left` / `right`

### 4.2 拆单输出

```json
{
  "cabinetInfo": { "name": "主卧衣柜", "room": "主卧" },
  "items": [
    {
      "partCode": "ZG-001",
      "partName": "左侧板",
      "boardId": 5,
      "materialName": "18mm暖白柜体板",
      "color": "暖白",
      "length": 2198,
      "width": 600,
      "thickness": 18,
      "grain": "vertical",
      "boardType": "侧板",
      "quantity": 1,
      "edgeLeft": false,
      "edgeRight": false,
      "edgeTop": true,
      "edgeBottom": true,
      "edgeRole": { "left": "靠墙侧", "right": "前口", "top": "上端", "bottom": "下端" },
      "holeOperations": []
    },
    {
      "partCode": "ZG-003",
      "partName": "左门板",
      "boardId": 6,
      "materialName": "18mm暖白门板",
      "color": "暖白",
      "length": 2148,
      "width": 398,
      "thickness": 18,
      "grain": "vertical",
      "boardType": "门板",
      "quantity": 1,
      "edgeLeft": true,
      "edgeRight": true,
      "edgeTop": true,
      "edgeBottom": true,
      "holeOperations": [
        { "sourceBoardId": "b-003", "workpieceCode": "ZG-003", "type": "hinge_cup", "face": "inner", "x": 22, "y": 24, "diameter": 35, "depth": 12, "unit": "mm" },
        { "sourceBoardId": "b-003", "workpieceCode": "ZG-003", "type": "hinge_cup", "face": "inner", "x": 22, "y": 1075, "diameter": 35, "depth": 12, "unit": "mm" },
        { "sourceBoardId": "b-003", "workpieceCode": "ZG-003", "type": "hinge_cup", "face": "inner", "x": 22, "y": 2126, "diameter": 35, "depth": 12, "unit": "mm" }
      ]
    }
  ]
}
```

- `partCode`/`partName`：工件编号与名称，每个板件独立一条
- `length`/`width`/`thickness`：已扣除封边厚度的实际切割尺寸
- `edgeLeft/edgeRight/edgeTop/edgeBottom`：布尔值，对应板件 2D 坐标系四边
- `quantity`：第一版固定为 1
- 不自动合并：即使两块板尺寸材质完全一致，也各自生成独立明细

拆分后的 `SplitItemVO` 字段映射到现有 `TOrderItem`：

| SplitItemVO | TOrderItem | 说明 |
| --- | --- | --- |
| `partCode` | `partCode` | 工件编号 |
| `partName` | `partName` | 板件名称 |
| `boardId` | `boardId` | 原材料板材 |
| `length` | `length` | 切割后长度 |
| `width` | `width` | 切割后宽度 |
| `thickness` | `thickness` | 必须写入订单明细（`TOrderItem.thickness` 为必填），同时存辅助表 |
| `quantity` | `quantity` | 第一版固定 1 |
| `edgeLeft/edgeRight/edgeTop/edgeBottom` | `edgeLeft/edgeRight/edgeTop/edgeBottom` | 布尔值转 `0/1` |
| 无 | `edgeFront/edgeBack` | 第一版固定 0，业务语义写入辅助表 `edge_role` |
| `grain` | `isTexture` | `vertical/horizontal` → 1，`none` → 0 |
| 无 | `allowRotation` | 纹理板默认 0，非纹理板按全局参数 |

同时保留 `edge_banding` JSON 到 `cabinet_order_item`，用于追溯原始 2D 封边对象。`holeOperations` 写入 `cabinet_order_item.hole_operations`。

### 4.3 预设模板表 `cabinet_template`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) NOT NULL | 模板名称 |
| category | VARCHAR(50) NOT NULL | 品类枚举：wardrobe/base-cabinet |
| thumbnail | VARCHAR(255) | 缩略图 URL |
| cabinet_json | JSON NOT NULL | 柜体结构 JSON |
| is_official | TINYINT DEFAULT 0 | 官方预置=1，用户保存=0 |
| created_by | BIGINT | 创建人 ID，关联 TUser |
| create_time | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME DEFAULT NULL | 更新时间 |

### 4.4 拆单辅助表 `cabinet_order_item`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| order_item_id | BIGINT UQ NOT NULL | 关联 `TOrderItem.itemId`，通过 FK 级联删除 |
| order_id | BIGINT NOT NULL | 冗余 `t_order.order_id`，便于按订单查询拆单批次 |
| user_id | BIGINT NOT NULL | 数据隔离，与 `t_order_item.user_id` 一致 |
| split_batch_code | VARCHAR(50) NOT NULL | 拆单批次编号，如 `ZG-20260519-001` |
| source_board_id | VARCHAR(50) | 柜体 JSON 中的板件 `id`，用于反查 3D 模型原始板件 |
| workpiece_code | VARCHAR(50) | 工件编号，如 ZG-001 |
| cabinet_name | VARCHAR(100) | 所属柜体名称 |
| room | VARCHAR(50) | 房间 |
| purpose | VARCHAR(100) | 用途 |
| board_type | VARCHAR(50) | 板件类型：side/layer/door/back/top/bottom |
| thickness | INT | 厚度(mm) |
| grain_direction | VARCHAR(20) | 纹理方向：vertical/horizontal/none |
| design_length | INT | 设计长度(mm) |
| design_width | INT | 设计宽度(mm) |
| position_x | DOUBLE | 3D 位置 X 坐标(mm) |
| position_y | DOUBLE | 3D 位置 Y 坐标(mm) |
| position_z | DOUBLE | 3D 位置 Z 坐标(mm) |
| edge_banding | JSON | 封边对象 `{left,right,top,bottom: boolean}` |
| edge_role | JSON | 边的业务语义标注 |
| hole_operations | JSON | 孔加工信息（通用结构，见 6.2 ③） |
| source_board_json | JSON | 当前板件的原始 JSON 快照（非完整柜体） |
| create_time | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**租户隔离处理：**
- `cabinet_order_item` 包含 `user_id`，写入时与 `TOrderItem.user_id` 一致
- `cabinet_template` 加入 `UserIdHandler.IGNORE_TABLES` 忽略列表，查询时手动限定 `WHERE is_official = 1 OR created_by = ?`
- 模板编辑/删除：必须校验 `created_by = 当前用户` 且 `is_official = 0`

**服务层 created_by 规则：**
- `POST /cabinet-templates`：强制 `created_by = UserContext.currentUserId`，`is_official = 0`
- 官方模板仅通过初始化 SQL 插入，普通用户接口不能创建 `is_official = 1`
- `created_by` 为空的模板只能是官方模板

**与 TOrderItem 的关系：**
- 拆单写入时，每个板件创建一个 `TOrderItem`（`length/width` 为切割尺寸），再插入对应的 `cabinet_order_item`
- 查询时 LEFT JOIN，手工录入的 `TOrderItem` 不产生 `cabinet_order_item` 记录
- 删除 `TOrderItem` 时，通过外键 `ON DELETE CASCADE` 自动级联删除对应 `cabinet_order_item`

---

## 五、3D 设计页布局与交互

### 5.1 页面布局（三栏）

```
┌──────────┬────────────────────────┬──────────────┐
│  左侧面板  │    中央 Three.js 画布    │  右侧属性面板  │
│  (280px) │                        │  (300px)    │
│          │                        │              │
│ ┌──────┐ │                        │  当前柜体属性   │
│ │柜体清单│ │                        │  ┌─────────┐ │
│ │主卧衣柜│ │                        │  │名称/房间  │ │
│ │电视柜  │ │                        │  │类型: 侧板  │ │
│ │鞋柜   │ │                        │  │材质: ...  │ │
│ └──────┘ │                        │  │长×宽×厚  │ │
│          │                        │  │铰链孔     │ │
│ ┌──────┐ │                        │  └─────────┘ │
│ │预设/板件│ │                        │              │
│ │衣柜/地柜│ │                        │              │
│ │侧板/层板│ │                        │              │
│ │门板/背板│ │                        │              │
│ └──────┘ │                        │              │
│          │                        │              │
│ [新增柜体]│ [拆单预览]              │              │
└──────────┴────────────────────────┴──────────────┘
```

### 5.2 参数化模板生成规则

调研资料中酷家乐、三维家、云熙等软件均采用参数驱动建模：用户先选柜型，再输入宽 W、深 D、高 H 及门板/层板等配置，系统按公式生成板件。当前项目的预设衣柜/地柜应遵循同一优先级：

| 板件 | 生成规则 | 位置约束 |
|------|----------|----------|
| 左/右侧板 | 厚度 `T`、高度 `H`、深度 `D` | X = `±(W/2 - T/2)`，作为外侧边界 |
| 顶板/底板 | 长度 `W - 2T`、厚度 `T`、深度 `D` | X 居中，嵌入两块侧板之间，不与侧板重叠 |
| 层板 | 长度 `W - 2T`、厚度 `T`、深度 `D` | 按层板数量在内高区间均分 |
| 背板 | 宽度 `W - 2T`、高度 `H - 2T`、背板厚度独立 | 靠后放置，不参与侧板/顶底板外包尺寸 |
| 门板 | 单扇宽度按内宽和门板数量均分，高度按柜型默认门缝规则 | 放在柜体前方，铰链边按左右门规则生成 |

参数面板必须保留明确单位、默认值和上下限，禁止生成尺寸小于等于 0 的板件。板件公式变化需要同步检查 3D 预览、拆单尺寸、封边扣减和订单明细写入。

### 5.3 核心交互流程

**订单内多柜建模（主流程）：**

```
选择/创建订单 → 进入3D设计页 → [新增柜体] →
选择预设(衣柜/地柜等) → 参数向导(宽高深/层板数/门板数) →
系统程序化生成3D模型 → 3D视图微调 → 保存到订单柜体清单 →
继续新增下一个柜体 → 全部柜体完成后 [拆单预览] →
汇总拆分明细预览弹窗 → 确认写入订单 → 跳转排版工作台
```

**自由拼装（辅助流程）：**

```
在当前订单下新建空柜体 → 从板件库拖拽类型到画布 →
放置定位(吸附对齐) → 调整尺寸/封边 → 保存到订单柜体清单 →
继续建模或进入拆单预览
```

设计阶段只维护柜体草稿，不写入 `TOrderItem`。只有用户点击“确认拆单”后，才将一个订单内所有待拆单柜体转换为订单明细。

### 5.4 3D 场景操作

- **旋转/缩放/平移**：Three.js OrbitControls
- **选中板件**：Raycaster 射线检测，高亮轮廓
- **拖拽移动**：在约束方向（X/Y/Z 轴）上拖拽
- **吸附对齐**：边缘间距 < 5mm 时自动吸附
- **预设生成**：根据宽高深、层板数参数程序化构建柜体 mesh
- **柜体切换**：左侧柜体清单切换当前编辑柜体，中央画布仅编辑当前柜体，避免多个柜体空间重叠导致误操作
- **新增/复制/删除柜体**：新增柜体默认进入未拆单草稿状态；复制柜体时生成新的 `clientCabinetId`；删除仅移除未拆单草稿，不影响已写入的订单明细
- **右侧属性联动**：点击 3D 板件后，右侧面板同步展示板件类型、尺寸、纹理方向、放置面和材料映射状态
- **快捷操作规划**：复制/删除、视角重置、撤销/重做等高频操作后续应优先以图标按钮或快捷键呈现，保持与主流设计软件习惯一致

### 5.5 目标订单绑定与入口

3D 设计页需要明确目标订单来源。`confirm` 要求 `orderId`。当前前端没有订单列表/详情页，第一版入口：

**从加工数据输入页进入**：`DataInputView` 顶栏或底栏增加"3D 柜体设计"按钮，用户先在 `DataInputView` 选择或创建订单（沿用现有订单创建流程），再跳转：

```
/cutting/cabinet-design?orderId=123
```

页面顶部显示订单信息区（订单编号、客户名称），用户在已有订单下维护多个柜体草稿并统一拆单。若未携带 `orderId`，提示用户先从加工数据输入页进入。

同一订单可包含一套房屋内的全部柜体，例如衣柜、电视柜、吊柜、酒柜、鞋柜、精品柜等。建模阶段不要求每完成一个柜体就拆单；用户可在柜体清单中自由切换、继续新增，最终以整单视角确认拆单和排样。

后续可扩展独立订单列表页作为入口来源，不在本期范围。

---

## 六、拆单服务逻辑

### 6.1 处理流程

```
订单柜体草稿清单 → 逐柜取 cabinetJson → ①板材校验 → ②封边尺寸换算 → ③铰链孔位计算 → ④工件编号生成 → 汇总输出
```

### 6.2 各环节说明

**① 板材校验**
- 逐个 `boardId` 查 `TBoard`，验证存在且启用
- 板件 `thickness` 必须等于 `TBoard.thickness`，不一致则校验失败
- 尺寸校验考虑旋转：`(length <= board.length AND width <= board.width) OR (length <= board.width AND width <= board.length)`
- 柜体拆单阶段默认不允许旋转（纹理板件有方向约束）；旋转允许与否由排样阶段 `allowRotation` 决定

**② 封边尺寸换算**

尺寸换算仅使用板件自身 2D 坐标系（`left/right` 为宽度方向，`top/bottom` 为长度方向）：

```
cutWidth  = designWidth  - (leftEdge ? edgeThickness : 0) - (rightEdge ? edgeThickness : 0)
cutLength = designLength - (topEdge ? edgeThickness : 0)  - (bottomEdge ? edgeThickness : 0)
```

- `edgeThickness` 默认 1mm
- `edgeRole`（如"靠墙侧""前口"等）仅为业务语义标注，不参与尺寸扣减
- 扣除后尺寸不得 ≤ 0，否则校验失败

示例：侧板设计尺寸 600 × 2200，封边 `{top: true, bottom: true, left: false, right: false}`：
- `cutWidth = 600 - 0 - 0 = 600mm`
- `cutLength = 2200 - 1 - 1 = 2198mm`

门板设计尺寸 400 × 2150，四边封边 `{left: true, right: true, top: true, bottom: true}`：
- `cutWidth = 400 - 1 - 1 = 398mm`
- `cutLength = 2150 - 1 - 1 = 2148mm`

**③ 孔加计算（本期仅门板铰链杯孔）**

拆单输出中的孔信息收敛为通用结构 `holeOperations`：

```json
{
  "holeOperations": [
    {
      "sourceBoardId": "b-003",
      "workpieceCode": "ZG-003",
      "type": "hinge_cup",
      "face": "inner",
      "x": 22,
      "y": 24,
      "diameter": 35,
      "depth": 12,
      "unit": "mm"
    }
  ]
}
```

核心参数：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `type` | `hinge_cup` | 孔类型，本期仅此一种 |
| `diameter` | 35mm | 铰链杯孔径 |
| `depth` | 12mm | 孔深 |
| `edgeDistance` | 22mm | 铰链中心距门边水平距离 |
| `doorGap` | 2mm | 门缝间隙（铰链侧） |
| `direction` | `height` | 沿长度方向排布 |
| `opening` | — | 开门方向：`left` / `right` |

`even` 均分模式：沿 `direction` 方向，首孔距板件端部 `edgeDistance + doorGap`，末孔同理，剩余孔位均分。

示例：门板设计尺寸 2150×400，`count: 3`：
- 有效排布范围：2150 - 24×2 = 2102mm
- 孔位置（距顶部）：24mm, 1075mm, 2126mm

**本期范围明确：不计算侧板铰链安装孔**，`holeOperations` 仅作为门板开孔提示。

**④ 工件编号生成**
- 格式：`{柜体简称}-{序号}`，如 `ZG-001`、`DG-001`
- 编号范围限定在订单内：统一拆单时按柜体清单顺序逐柜生成；追加拆单时从该订单现有最大序号继续
- 示例：订单已有 ZG-001~003，本次统一拆单新增电视柜和鞋柜时，从各自柜体简称的下一个序号开始生成
- `splitBatchCode`（批次编号，如 `ZG-20260519-001`）用于标识单个柜体的一次拆单写入；同一订单的一次统一拆单可产生多个 `splitBatchCode`

### 6.3 业务边界

拆单服务分为两个阶段：
1. **execute**：只做拆单预览计算，不写库。对每个板件独立生成一条明细，不合并
2. **confirm**：用户确认后，以事务方式批量写入 `TOrderItem` 和 `cabinet_order_item`

前端统一拆单是订单级编排：对 `cabinetDrafts` 中待拆单的柜体逐个执行预览并汇总展示；确认后按柜体顺序逐个追加到同一订单。后端单次拆单仍以单个 `cabinetJson` 为计算单位，避免一次性改动现有接口契约。

第一版统一拆单不是跨柜体全局事务。若某个柜体 confirm 失败，前端停止后续柜体写入，展示已成功批次与失败柜体，用户修正后可对失败草稿重试。后续如需要严格“全屋柜体全部成功或全部回滚”，再扩展批量 confirm 接口。

- 封边扣除量 1mm 初期硬编码，后续可改为系统配置项
- 每个板件独立生成一条 `TOrderItem`，不做数量合并（即使尺寸材质完全一致），保证位置、封边方向、连接关系等差异信息不丢失
- 第一版不提供批次级撤销接口。用户拆错柜体时，可在订单明细中逐条删除，外键自动清理 `cabinet_order_item`。批次级删除（`DELETE /order-split/batches/{splitBatchCode}`）不在本期范围

---

## 七、接口设计

### 7.1 预设模板接口

| 方法 | 路径 | 认证 | 权限 | 说明 |
|------|------|------|------|------|
| GET | `/cabinet-templates` | JWT | `order:read` | 分页查询，支持 `category` 筛选，手动限定 `is_official=1 OR created_by=?` |
| GET | `/cabinet-templates/{id}` | JWT | `order:read` | 获取模板详情，仅允许 `is_official=1` 或 `created_by=当前用户` |
| POST | `/cabinet-templates` | JWT | `order:write` | 保存当前设计为模板 |
| PUT | `/cabinet-templates/{id}` | JWT | `order:write` | 编辑自己的模板，校验 `created_by = 当前用户` 且 `is_official = 0` |
| DELETE | `/cabinet-templates/{id}` | JWT | `order:write` | 删除自己的模板，校验 `created_by = 当前用户` 且 `is_official = 0` |

### 7.2 拆单接口

| 方法 | 路径 | 认证 | 权限 | 审计 | 说明 |
|------|------|------|------|------|------|
| POST | `/order-split/execute` | JWT | `order:write` | — | 传入柜体 JSON，返回拆分明细（纯计算，不写库） |
| POST | `/order-split/confirm` | JWT | `order:write` | `@AuditLog(module="柜体拆单", action="确认拆单")` | 重新计算并写入 TOrderItem + cabinet_order_item |

### 7.3 接口详细

**POST /order-split/execute**

入参与 confirm 使用同一请求结构（不含 `orderId` 和 `confirmMode`）。该接口仍计算单个柜体；订单级拆单预览由前端对多个 `cabinetDrafts` 逐柜调用并汇总：

```json
{
  "cabinetJson": { /* 柜体结构 JSON，见 4.1 */ },
  "materialSlotBoardMap": { "cabinet_body": 5, "door": 6, "back": 7 }
}
```

- 返回：`Result<List<SplitItemVO>>`（见 4.2）
- execute 和 confirm 使用同一套板材解析与校验逻辑
- 多柜体订单预览时，前端按柜体清单顺序调用 execute，预览弹窗按柜体分组展示明细和校验错误

**POST /order-split/confirm**

入参为单个柜体 JSON（不是前端传回的明细列表，防止篡改切割尺寸）：

```json
{
  "orderId": 123,
  "confirmMode": "append",
  "cabinetJson": { /* 柜体结构 JSON，见 4.1 */ },
  "materialSlotBoardMap": {
    "cabinet_body": 5,
    "door": 6,
    "back": 7
  }
}
```

- `confirmMode`：第一版仅支持 `append`（向订单追加一个柜体的拆单明细）
- 一个订单可包含多个柜体。用户在建模阶段可先维护多个柜体草稿；统一确认拆单时，前端按柜体清单顺序多次调用 `append`，每次追加一个柜体的明细
- 响应返回 `splitBatchCode`（形如 `ZG-20260519-001`），标识本次写入的柜体批次

后端在事务内重新执行拆单计算并写库，校验项：
- `orderId` 属于当前用户
- 所有 `boardId` 属于当前用户且 `is_enabled = 1`
- 板件厚度必须等于 `TBoard.thickness`
- 切割尺寸不超过对应 `TBoard.length/width`（允许旋转后适配）
- `TOrderItem` 和 `cabinet_order_item` 在同一事务中写入

返回：
```json
{
  "code": 200,
  "data": {
    "orderId": 123,
    "splitBatchCode": "ZG-20260519-001",
    "cabinetName": "主卧衣柜",
    "createdItemIds": [11, 12, 13],
    "nextAction": "layout-workbench"
  }
}
```

订单级统一拆单完成后，前端汇总多个 confirm 响应，展示所有 `splitBatchCode` 和新增明细数量，再进入排版工作台。

### 7.4 排版工作台自动排版接口

新增 `GET /orders/{id}/layout-input`（JWT，`order:read`），返回排版工作台所需的分组结构：

```json
{
  "code": 200,
  "data": {
    "groups": [
      {
        "board": {
          "boardId": 5,
          "length": 2440,
          "width": 1220,
          "thickness": 18,
          "brand": "兔宝宝",
          "materialType": "柜体板",
          "color": "暖白",
          "sizeType": "标准板"
        },
        "items": [
          {
            "orderItemId": 11,
            "partCode": "ZG-001",
            "partName": "左侧板",
            "length": 2198,
            "width": 600,
            "quantity": 1
          }
        ]
      }
    ],
    "algorithmConfig": {
      "gapDistance": 3,
      "allowRotation": false
    }
  }
}
```

- 每组包含原材料 `TBoard` 的 `length/width/thickness`（算法任务需要的 `L/W`）
- 工件 `orderItemId` 用于前端构建 `Square.id = "${orderItemId}-${index}"`，排版结果中 `PlaceSquare.id` 回传此值
- `algorithmConfig` 默认值来自拆单上下文：纹理板件默认不允许旋转；前端仍可允许用户手动调整

**第一版排样行为：整单排样**。即 `layout-input` 返回订单全部明细，不按 `splitBatchCode` 过滤。如后续需要按批次预览，可通过查询参数 `?splitBatchCode=XXX` 扩展。

### 7.5 拆单后衔接排版工作台

订单级统一拆单完成后，前端以最后一次 `confirm` 返回的 `nextAction: "layout-workbench"` 为准，或在所有柜体均确认成功后直接跳转：

```
/cutting/layout-workbench?orderId=123&source=cabinet
```

前端自动排版权限策略：
- `CabinetDesignView` 菜单/入口需要 `order:write`
- `LayoutWorkbenchView` 检测 `source=cabinet` 时，前端检查 `algorithm:execute` 权限；无权限时仅展示拆单明细，不提交算法

`LayoutWorkbenchView` 扩展逻辑：
1. 检测 `source=cabinet` 时，调用 `GET /orders/{orderId}/layout-input` 获取分组结构
2. 前端按 `Square.id = "${orderItemId}-1"` 展开构建算法入参
3. 按组自动提交排样算法，渲染 Canvas 结果

此链路复用现有 `useAlgorithmSubmit` composable 和排版工作台渲染流程，无需新增前端草稿机制。

### 7.6 现有接口影响

- `/orders`、`/order-items` 保持不变
- `GET /orders/{id}/layout-input` 为本期新增接口，供排版工作台使用

### 7.7 后端实现约束

**权限注解规范：**
- 控制器类上不放权限注解，或仅放 `@RequirePermission("order:read")`
- 写接口必须在方法上单独标 `@RequirePermission("order:write")`
- `confirm` 必须在方法上标 `@RequirePermission("order:write")` + `@AuditLog(module="柜体拆单", action="确认拆单")`

**算法输出模型扩展：**
算法输出 `PlaceSquare` 需新增 `id` 字段（来源板件的 `partCode`），以在排版结果中追溯工件身份：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSquare {
    private String id;     // 对应 Square.id（如 "11-1"），前端用其反查 partCode/partName
    private double x, y, l, w;

    // 兼容旧构造器（避免现有 new PlaceSquare(x,y,l,w) 编译失败）
    public PlaceSquare(double x, double y, double l, double w) {
        this(null, x, y, l, w);
    }
}
```

同步更新：算法放置逻辑必须把 `Square.id` 传入 `PlaceSquare.id`；算法单测断言补充 `id` 字段；前端 Canvas tooltip 用 `id` 反查 `partCode/partName`；小程序算法结果展示同步更新。

---

## 八、技术选型

| 层面 | 选择 | 说明 |
|------|------|------|
| 3D 引擎 | three npm 包 | 通过 Vite 构建，不通过 CDN 引入 |
| 3D 交互 | three/examples/jsm/controls/OrbitControls.js | 旋转缩放 + Raycaster 点击选中 |
| 前端状态 | Pinia store (`useCabinetDesign`) | 管理订单级柜体草稿清单、当前柜体、选中状态、撤销栈 |
| UI 框架 | Element Plus | 与现有前端一致 |
| 纹理方向 | 前端箭头标注 + JSON 字段 | vertical/horizontal/none |
| 后端框架 | Spring Boot + MyBatis-Plus | 与现有一致 |
| 数据库脚本 | MySQL 增量脚本 `YYYYMMDD_feature.sql` | 放入 `src/main/resources/db/migration/` |

---

## 九、分期实施计划

| 阶段 | 范围 | 可验收标准 |
|------|------|-----------|
| **MVP 0** | DataInputView 增加订单选择/创建能力，3D 设计入口只在已有 orderId 后可用 | 可从订单列表选择已有订单或创建新订单，跳转到 3D 设计页时携带有效 orderId |
| **MVP 1** | 订单内柜体清单 + 预设衣柜/地柜参数向导 → 参数化生成无重叠柜体 → 多柜体 3D 预览 → 统一拆单 → 写入订单 | 同一 `orderId` 下可连续新增至少 2 个柜体；顶板/底板/层板按 `W - 2T` 内嵌生成且不与侧板重叠；建模过程中不写入订单明细；统一拆单后 `TOrderItem/cabinet_order_item` 正确写入 |
| **MVP 2** | 统一拆单后自动衔接排版工作台 | 全部柜体 `confirm` 成功 → 跳转排版工作台 → 自动按整单分组提交算法 → Canvas 渲染排样结果 |
| **MVP 3** | 用户模板保存/复用 | 存为模板 → 模板列表可见 → 从模板创建新设计 → 仅可编辑/删除自己的模板 |
| **MVP 4** | 自由拼装、吸附对齐、更多柜型、快捷键/撤销重做增强 | 从板件库拖拽 → 3D 吸附放置 → 微调属性 → 拆单；支持常用复制/删除/视角复位/撤销重做操作 |

---

## 十、测试策略

### 10.1 OrderSplitServiceTest

- 单块板正常拆单（侧板，部分封边）
- 封边扣尺寸：验证 `cutWidth`/`cutLength` 按 2D 公式正确计算
- 扣除后尺寸非法（≤ 0）→ 校验失败
- 板材禁用或不存在 → 校验失败
- 板件尺寸超过板材规格 → 校验失败
- 铰链孔 even 均分计算正确性

### 10.2 OrderSplitControllerTest

- 未登录 → 401
- 无 `order:write` 权限 → 403
- `execute` 不写库
- `confirm` 事务写入 `t_order_item` 和 `cabinet_order_item`
- 同一订单连续 `append` 多个柜体时，新增明细均归属同一 `orderId`，编号从订单已有最大序号继续
- `confirm` 入参缺少 `orderId` 或 `cabinetJson` → HTTP 200，`Result.code=0`，data 中包含字段错误（与现有 `GlobalExceptionHandler` 一致）

### 10.3 CabinetTemplateControllerTest

- 官方模板对所有用户可见
- 用户只能编辑/删除自己的模板
- 普通用户不能编辑/删除官方模板（`is_official=1`）

### 10.4 前端验证

- `cd frontend && npm run build` 无报错
- 浏览器检查：Three.js canvas 非空、模板生成可见、顶板/底板/层板不与侧板重叠、右侧属性面板随选中板件联动、同一订单下新增多个柜体时不会强制拆单
- 统一拆单预览按柜体分组展示；确认后所有柜体明细写入同一订单，并能进入整单排版

### 10.5 集成验证

- `OrderSplitServiceTest` 验证拆单计算、封边、孔位、编号（必须使用真实服务实例，不能 mock 拆单逻辑）
- `OrderSplitControllerTest` 验证鉴权、参数校验、响应结构（可使用 MockMvc + `@MockitoBean` mock service）
- 至少一次在连接 MySQL 的环境中验证 `TOrderItem` 与 `cabinet_order_item` 事务写入和外键级联删除。若当前测试环境不连接 MySQL，在交付说明中标注"数据库级外键和级联删除需通过本地 MySQL 手动验证"

---

## 十一、交付清单

实施完成后必须同步更新：

- **`AGENTS.md`**：
  - 接口地图新增 `/cabinet-templates`、`/order-split/*`、`GET /orders/{id}/layout-input`
  - 数据模型新增 `cabinet_template`、`cabinet_order_item`
  - 前端约定新增 `CabinetDesignView.vue`、路由 `/cutting/cabinet-design`、订单级柜体草稿清单、`three` npm 依赖
  - 变更记录新增条目（日期、类型、范围、原因、影响、验证）
- **前端**：`frontend/package.json` 新增 `three` 依赖
- **数据库**：执行两条 `20260519_create_*.sql` 脚本
- **`cabinet_template` 初始化数据**：插入两条官方模板（衣柜 + 地柜），使用 `materialSlot` 占位，`is_official=1`

---

## 十二、后续扩展方向（不在本期范围）

- 更多柜体品类（吊柜、书柜、鞋柜等）
- 五金件管理
- 小程序端 3D 查看
- CAD 文件导入/导出（DXF/DWG）
- 封边厚度系统配置化
- 铰链孔 fixed 模式支持自定义位置
- 连接件孔位（三合一、层板托等）
- 拆单结果支持导出标签条码 PDF

---

## 十三、数据库迁移脚本

执行方式：手动在 `board_cutting_db` 中执行。先建 `cabinet_template`，再建 `cabinet_order_item`（依赖 `t_order_item` 和 `t_user` 表）。

```sql
USE board_cutting_db;

-- 20260519_create_cabinet_template.sql
CREATE TABLE cabinet_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    thumbnail VARCHAR(255),
    cabinet_json JSON NOT NULL,
    is_official TINYINT DEFAULT 0,
    created_by BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL,
    INDEX idx_category (category),
    INDEX idx_is_official (is_official),
    CONSTRAINT fk_cabinet_template_user
        FOREIGN KEY (created_by)
        REFERENCES t_user(user_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 20260519_create_cabinet_order_item.sql
CREATE TABLE cabinet_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    split_batch_code VARCHAR(50) NOT NULL,
    source_board_id VARCHAR(50),
    workpiece_code VARCHAR(50),
    cabinet_name VARCHAR(100),
    room VARCHAR(50),
    purpose VARCHAR(100),
    board_type VARCHAR(50),
    thickness INT,
    grain_direction VARCHAR(20),
    design_length INT,
    design_width INT,
    position_x DOUBLE,
    position_y DOUBLE,
    position_z DOUBLE,
    edge_banding JSON,
    edge_role JSON,
    hole_operations JSON,
    source_board_json JSON,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_item_id (order_item_id),
    INDEX idx_cabinet_order_batch (user_id, order_id, split_batch_code),
    INDEX idx_workpiece_code (workpiece_code),
    CONSTRAINT fk_cabinet_order_item_order_item
        FOREIGN KEY (order_item_id)
        REFERENCES t_order_item(item_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_cabinet_order_item_order
        FOREIGN KEY (order_id)
        REFERENCES t_order(order_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_cabinet_order_item_user
        FOREIGN KEY (user_id)
        REFERENCES t_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 实体字段对齐

- Java 实体使用 `createTime`/`updateTime`（匹配 `AutoFillHandler` 自动填充规则）
- `cabinet_template.createTime` 按需加 `@TableField(fill = FieldFill.INSERT)`
- `cabinet_template.updateTime` 按需加 `@TableField(fill = FieldFill.INSERT_UPDATE)`
