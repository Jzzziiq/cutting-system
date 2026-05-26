# 小程序重构 + Web 生产员页面清理 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完全重写小程序为生产员工作流（任务查看、状态流转、通知），从 Web 前端移除生产员专属页面，新增后端通知功能和 `/users/me` 端点。

**Architecture:** 后端新增 `t_notification` 表和通知 API，补充 `/users/me` 端点，修改任务分配逻辑触发通知。小程序完全重写，保留 `utils/request.js` 认证机制，新建登录、任务列表、任务详情、通知列表、个人设置页面。Web 前端删除 `ProducerTasksView` 和 `ProductionMyOrdersView`，修改路由守卫将 viewer 重定向到个人设置。

**Tech Stack:** Java 17, Spring Boot 3.5.11, MyBatis-Plus 3.5.x, MySQL 8.0+, 微信小程序原生框架

---

## 后端任务

### Task 1: 创建通知表 SQL 迁移

**Files:**
- Create: `src/main/resources/db/migration/20260525_notification.sql`

- [ ] **Step 1: 创建迁移文件**

```sql
-- 20260525_notification.sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
```

- [ ] **Step 2: 执行迁移**

使用 `src/main/resources/application-local.yml` 中的数据库凭据，直接对本地 MySQL 执行此 SQL。执行后用 `SHOW COLUMNS FROM t_notification;` 验证。

---

### Task 2: 创建 TNotification 实体和 VO

**Files:**
- Create: `src/main/java/com/cutting/cuttingsystem/entitys/TNotification.java`
- Create: `src/main/java/com/cutting/cuttingsystem/entitys/VO/TNotificationVO.java`

- [ ] **Step 1: 创建实体**

```java
package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_notification")
public class TNotification implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Long taskId;
    private Integer isRead;
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
```

- [ ] **Step 2: 创建 VO**

```java
package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;
import java.util.Date;

@Data
public class TNotificationVO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Long taskId;
    private Integer isRead;
    private Date createdAt;
}
```

---

### Task 3: 创建 TNotificationMapper

**Files:**
- Create: `src/main/java/com/cutting/cuttingsystem/mapper/TNotificationMapper.java`

- [ ] **Step 1: 创建 Mapper 接口**

```java
package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cutting.cuttingsystem.entitys.TNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TNotificationMapper extends BaseMapper<TNotification> {

    @Update("UPDATE t_notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    @InterceptorIgnore(tenantLine = "true")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE t_notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    @InterceptorIgnore(tenantLine = "true")
    int markAllRead(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM t_notification WHERE user_id = #{userId} AND is_read = 0")
    @InterceptorIgnore(tenantLine = "true")
    int countUnread(@Param("userId") Long userId);
}
```

---

### Task 4: 创建 TNotificationService 和实现

**Files:**
- Create: `src/main/java/com/cutting/cuttingsystem/service/TNotificationService.java`
- Create: `src/main/java/com/cutting/cuttingsystem/service/impl/TNotificationServiceImpl.java`

- [ ] **Step 1: 创建 Service 接口**

```java
package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.TNotification;
import com.cutting.cuttingsystem.entitys.VO.TNotificationVO;

public interface TNotificationService extends IService<TNotification> {
    IPage<TNotificationVO> listMyNotifications(Long userId, int pageNum, int pageSize);
    boolean markRead(Long id, Long userId);
    boolean markAllRead(Long userId);
    int countUnread(Long userId);
    void createNotification(Long userId, String title, String content, Long taskId);
}
```

- [ ] **Step 2: 创建 Service 实现**

```java
package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TNotification;
import com.cutting.cuttingsystem.entitys.VO.TNotificationVO;
import com.cutting.cuttingsystem.mapper.TNotificationMapper;
import com.cutting.cuttingsystem.service.TNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TNotificationServiceImpl extends ServiceImpl<TNotificationMapper, TNotification>
        implements TNotificationService {

    @Override
    public IPage<TNotificationVO> listMyNotifications(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<TNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TNotification::getUserId, userId)
               .orderByDesc(TNotification::getCreatedAt);
        IPage<TNotification> page = baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.convert(this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markRead(Long id, Long userId) {
        return baseMapper.markRead(id, userId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllRead(Long userId) {
        return baseMapper.markAllRead(userId) > 0;
    }

    @Override
    public int countUnread(Long userId) {
        return baseMapper.countUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Long userId, String title, String content, Long taskId) {
        TNotification notification = new TNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTaskId(taskId);
        notification.setIsRead(0);
        save(notification);
    }

    private TNotificationVO toVO(TNotification entity) {
        TNotificationVO vo = new TNotificationVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
```

---

### Task 5: 创建 NotificationController

**Files:**
- Create: `src/main/java/com/cutting/cuttingsystem/controller/NotificationController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.VO.TNotificationVO;
import com.cutting.cuttingsystem.service.TNotificationService;
import com.cutting.cuttingsystem.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/notifications")
@Validated
public class NotificationController {

    @Autowired
    private TNotificationService notificationService;

    @GetMapping
    @RequirePermission({})
    public Result list(@RequestParam(defaultValue = "1") @Positive int pageNum,
                       @RequestParam(defaultValue = "20") @Positive int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        IPage<TNotificationVO> page = notificationService.listMyNotifications(userId, pageNum, pageSize);
        return Result.success(page);
    }

    @PutMapping("/{id}/read")
    @RequirePermission({})
    public Result markRead(@PathVariable @Positive Long id) {
        Long userId = UserContext.getCurrentUserId();
        boolean ok = notificationService.markRead(id, userId);
        return ok ? Result.success() : Result.error("通知不存在");
    }

    @PutMapping("/read-all")
    @RequirePermission({})
    public Result markAllRead() {
        Long userId = UserContext.getCurrentUserId();
        notificationService.markAllRead(userId);
        return Result.success();
    }

    @GetMapping("/unread-count")
    @RequirePermission({})
    public Result unreadCount() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(notificationService.countUnread(userId));
    }
}
```

---

### Task 6: 新增 GET /users/me 端点

**Files:**
- Modify: `src/main/java/com/cutting/cuttingsystem/controller/UserController.java`

- [ ] **Step 1: 在 UserController 中添加 /me 端点**

在 `UserController.java` 的现有方法之后（`changeMyPassword` 方法附近），添加：

```java
@GetMapping("/me")
@RequirePermission({})
public Result me() {
    Long userId = UserContext.getCurrentUserId();
    TUser user = userService.getById(userId);
    if (user == null) return Result.error("用户不存在");
    return Result.success(toVO(user));
}
```

注意：`toVO` 方法已在 `UserController` 中定义（line 204-210），直接复用。

---

### Task 7: 补充任务详情板材品牌字段

**Files:**
- Modify: `src/main/java/com/cutting/cuttingsystem/entitys/VO/TOrderItemVO.java`
- Modify: `src/main/java/com/cutting/cuttingsystem/service/impl/TProductionTaskServiceImpl.java`

- [ ] **Step 1: 在 TOrderItemVO 中添加 brand 字段**

在 `TOrderItemVO.java` 中添加字段：

```java
private String brand;
```

- [ ] **Step 2: 在 getMyTaskDetail 中补充 brand 查询**

在 `TProductionTaskServiceImpl.java` 中，找到 `getOrderDetailIgnoreTenant` 方法（约 line 243-255）。该方法调用 `toItemVO` 转换每个 orderItem。需要在 `toItemVO` 之后，通过 `boardId` 查询 `t_board` 获取 `brand`。

在 `TProductionTaskServiceImpl` 中注入 `TBoardMapper`：

```java
@Autowired
private TBoardMapper boardMapper;
```

修改 `getOrderDetailIgnoreTenant` 方法中 items 的处理逻辑，在 `toItemVO` 之后为每个 item 补充 brand：

```java
private TOrderItemVO toItemVO(TOrderItem item) {
    TOrderItemVO vo = new TOrderItemVO();
    BeanUtils.copyProperties(item, vo);
    // 补充板材品牌
    if (item.getBoardId() != null) {
        TBoard board = boardMapper.selectById(item.getBoardId());
        if (board != null) {
            vo.setBrand(board.getBrand());
        }
    }
    return vo;
}
```

---

### Task 8: 修改任务分配逻辑触发通知

**Files:**
- Modify: `src/main/java/com/cutting/cuttingsystem/service/impl/TProductionTaskServiceImpl.java`

- [ ] **Step 1: 注入 TNotificationService**

在 `TProductionTaskServiceImpl` 中添加：

```java
@Autowired
private TNotificationService notificationService;
```

- [ ] **Step 2: 在 assignOrderTask 中添加通知**

找到 `assignOrderTask` 方法（约 line 97-126）。在方法末尾 `return toVO(task)` 之前，添加通知逻辑：

```java
// 创建通知
String taskName = task.getTaskName() != null ? task.getTaskName() : "新任务";
notificationService.createNotification(
    assigneeId,
    "新任务分配",
    "您有新的生产任务：" + taskName,
    task.getId()
);
```

- [ ] **Step 3: 在 assignTask 中添加通知**

找到 `assignTask` 方法（约 line 86-93）。在 `updateById(task)` 之后，添加通知逻辑：

```java
// 创建通知
String taskName = task.getTaskName() != null ? task.getTaskName() : "任务";
notificationService.createNotification(
    assigneeId,
    "任务分配变更",
    "任务已分配给您：" + taskName,
    task.getId()
);
```

---

## 小程序任务

### Task 9: 删除旧小程序文件

**Files:**
- Delete: `miniprogram/pages/customers/` (整个目录)
- Delete: `miniprogram/pages/boards/` (整个目录)
- Delete: `miniprogram/pages/algorithm/` (整个目录)

- [ ] **Step 1: 删除旧页面目录**

```bash
rm -rf miniprogram/pages/customers
rm -rf miniprogram/pages/boards
rm -rf miniprogram/pages/algorithm
```

---

### Task 10: 重写小程序入口文件

**Files:**
- Rewrite: `miniprogram/app.js`
- Rewrite: `miniprogram/app.json`
- Rewrite: `miniprogram/app.wxss`

- [ ] **Step 1: 重写 app.js**

```javascript
App({
  globalData: {
    token: '',
    userInfo: null
  },
  onLaunch() {
    this.globalData.token = wx.getStorageSync('token') || '';
    this.globalData.userInfo = wx.getStorageSync('userInfo') || null;
  }
});
```

- [ ] **Step 2: 重写 app.json**

```json
{
  "pages": [
    "pages/login/login",
    "pages/tasks/index",
    "pages/tasks/detail",
    "pages/profile/index",
    "pages/notifications/index"
  ],
  "window": {
    "navigationBarTitleText": "板材切割系统",
    "navigationBarBackgroundColor": "#1f2937",
    "navigationBarTextStyle": "white",
    "backgroundColor": "#f6f7f9"
  },
  "tabBar": {
    "color": "#6b7280",
    "selectedColor": "#2563eb",
    "backgroundColor": "#ffffff",
    "list": [
      {
        "pagePath": "pages/tasks/index",
        "text": "我的任务",
        "iconPath": "assets/icons/task.png",
        "selectedIconPath": "assets/icons/task-active.png"
      },
      {
        "pagePath": "pages/profile/index",
        "text": "个人设置",
        "iconPath": "assets/icons/profile.png",
        "selectedIconPath": "assets/icons/profile-active.png"
      }
    ]
  },
  "style": "v2"
}
```

注意：需要准备 tabBar 图标文件放在 `miniprogram/assets/icons/` 目录下。可以使用简单的 PNG 图标（81x81 像素）。

- [ ] **Step 3: 保留 app.wxss**

`app.wxss` 的全局样式已经很好，直接保留不动。它提供了 `.page`、`.section`、`.between`、`.row`、`.title`、`.muted`、`.label`、`.list-item`、`.empty`、`.primary`、`.secondary`、`.danger`、`.ghost` 等所有需要的工具类。

---

### Task 11: 重写工具层（config.js 和 request.js）

**Files:**
- Rewrite: `miniprogram/utils/config.js`
- Rewrite: `miniprogram/utils/request.js`

- [ ] **Step 1: config.js 保持不变**

```javascript
module.exports = {
  baseUrl: 'http://localhost:8080'
};
```

- [ ] **Step 2: request.js 保持不变**

现有的 `request.js` 已经完善，包含 JWT 认证、401 处理、错误提示，直接保留。

---

### Task 12: 重写 services/api.js

**Files:**
- Rewrite: `miniprogram/services/api.js`

- [ ] **Step 1: 编写新的 api.js**

```javascript
const request = require('../utils/request');

function login(username, password) {
  return request({
    url: '/auth/login',
    method: 'POST',
    data: { username, password },
    auth: false,
    form: true
  });
}

function listMyTasks() {
  return request({ url: '/production-tasks/my' });
}

function getMyTaskDetail(taskId) {
  return request({ url: `/production-tasks/my/${taskId}` });
}

function transitionTask(taskId, status) {
  return request({
    url: `/production-tasks/my/${taskId}/status`,
    method: 'PUT',
    data: { status }
  });
}

function listNotifications(pageNum = 1, pageSize = 20) {
  return request({ url: '/notifications', data: { pageNum, pageSize } });
}

function markNotificationRead(id) {
  return request({ url: `/notifications/${id}/read`, method: 'PUT' });
}

function markAllNotificationsRead() {
  return request({ url: '/notifications/read-all', method: 'PUT' });
}

function getUnreadCount() {
  return request({ url: '/notifications/unread-count' });
}

function getProfile() {
  return request({ url: '/users/me' });
}

function changePassword(oldPassword, newPassword) {
  return request({
    url: '/users/me/password',
    method: 'PUT',
    data: { oldPassword, newPassword }
  });
}

module.exports = {
  login,
  listMyTasks,
  getMyTaskDetail,
  transitionTask,
  listNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  getUnreadCount,
  getProfile,
  changePassword
};
```

---

### Task 13: 创建登录页

**Files:**
- Create: `miniprogram/pages/login/login.wxml`
- Create: `miniprogram/pages/login/login.wxss`
- Create: `miniprogram/pages/login/login.js`
- Create: `miniprogram/pages/login/login.json`

- [ ] **Step 1: login.js**

```javascript
const api = require('../../services/api');
const app = getApp();

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },
  onShow() {
    const token = wx.getStorageSync('token');
    if (token) {
      wx.switchTab({ url: '/pages/tasks/index' });
    }
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value });
  },
  async onLogin() {
    const { username, password } = this.data;
    if (!username || !password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    try {
      const res = await api.login(username, password);
      wx.setStorageSync('token', res.token);
      wx.setStorageSync('userInfo', res.userInfo || res);
      app.globalData.token = res.token;
      app.globalData.userInfo = res.userInfo || res;

      // 角色校验：只允许 viewer 角色
      const userInfo = res.userInfo || res;
      const roles = userInfo.roles || [];
      if (!roles.includes('viewer')) {
        wx.showToast({ title: '请使用电脑端管理系统', icon: 'none', duration: 3000 });
        wx.removeStorageSync('token');
        wx.removeStorageSync('userInfo');
        app.globalData.token = '';
        app.globalData.userInfo = null;
        return;
      }

      wx.switchTab({ url: '/pages/tasks/index' });
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ loading: false });
    }
  }
});
```

- [ ] **Step 2: login.wxml**

```xml
<view class="login-page">
  <view class="login-card">
    <view class="title" style="text-align:center;margin-bottom:40rpx;">板材切割系统</view>
    <view class="field">
      <view class="label">用户名</view>
      <input data-field="username" bindinput="onInput" placeholder="请输入用户名" value="{{username}}" />
    </view>
    <view class="field">
      <view class="label">密码</view>
      <input data-field="password" bindinput="onInput" type="password" placeholder="请输入密码" value="{{password}}" />
    </view>
    <button class="primary" loading="{{loading}}" bindtap="onLogin">登录</button>
  </view>
</view>
```

- [ ] **Step 3: login.wxss**

```css
.login-page {
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  padding: 48rpx;
  background: #f6f7f9;
}
.login-card {
  width: 100%;
  background: #fff;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 48rpx;
}
```

- [ ] **Step 4: login.json**

```json
{
  "navigationBarTitleText": "登录"
}
```

---

### Task 14: 创建我的任务页（tabBar）

**Files:**
- Create: `miniprogram/pages/tasks/index.wxml`
- Create: `miniprogram/pages/tasks/index.wxss`
- Create: `miniprogram/pages/tasks/index.js`
- Create: `miniprogram/pages/tasks/index.json`

- [ ] **Step 1: index.js**

```javascript
const api = require('../../services/api');

Page({
  data: {
    tasks: [],
    loading: false,
    unreadCount: 0
  },
  onShow() {
    this.loadTasks();
    this.loadUnreadCount();
  },
  async loadTasks() {
    this.setData({ loading: true });
    try {
      const tasks = await api.listMyTasks();
      this.setData({ tasks: Array.isArray(tasks) ? tasks : (tasks.records || []) });
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ loading: false });
    }
  },
  async loadUnreadCount() {
    try {
      const count = await api.getUnreadCount();
      this.setData({ unreadCount: count || 0 });
    } catch (err) {
      // 静默失败
    }
  },
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/tasks/detail?id=${id}` });
  },
  goNotifications() {
    wx.navigateTo({ url: '/pages/notifications/index' });
  },
  onPullDownRefresh() {
    this.loadTasks().finally(() => wx.stopPullDownRefresh());
  },
  getStatusClass(status) {
    if (status === 0) return 'status-pending';
    if (status === 1) return 'status-progress';
    return 'status-done';
  },
  getStatusLabel(status) {
    if (status === 0) return '待处理';
    if (status === 1) return '生产中';
    return '已完成';
  }
});
```

- [ ] **Step 2: index.wxml**

```xml
<view class="page">
  <!-- 标题栏 -->
  <view class="between section">
    <view class="title">我的任务</view>
    <view class="bell-btn" bindtap="goNotifications">
      <text class="bell-icon">🔔</text>
      <text wx:if="{{unreadCount > 0}}" class="badge">{{unreadCount > 99 ? '99+' : unreadCount}}</text>
    </view>
  </view>

  <!-- 任务列表 -->
  <view class="section" wx:if="{{tasks.length}}">
    <view class="list-item" wx:for="{{tasks}}" wx:key="taskId" data-id="{{item.taskId}}" bindtap="goDetail">
      <view class="between">
        <view class="item-title">{{item.taskName || '未命名任务'}}</view>
        <view class="status-tag {{item.status === 0 ? 'status-pending' : item.status === 1 ? 'status-progress' : 'status-done'}}">
          {{item.statusLabel || (item.status === 0 ? '待处理' : item.status === 1 ? '生产中' : '已完成')}}
        </view>
      </view>
      <view class="item-meta">订单号：{{item.orderNo || '-'}}</view>
      <view class="item-meta" wx:if="{{item.estimatedHours}}">预计工时：{{item.estimatedHours}}h</view>
    </view>
  </view>

  <!-- 空状态 -->
  <view class="section empty" wx:if="{{!loading && !tasks.length}}">暂无任务</view>

  <!-- 加载中 -->
  <view class="section empty" wx:if="{{loading}}">加载中...</view>
</view>
```

- [ ] **Step 3: index.wxss**

```css
.bell-btn {
  position: relative;
  padding: 8rpx;
}
.bell-icon {
  font-size: 40rpx;
}
.badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #ef4444;
  color: #fff;
  font-size: 20rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  border-radius: 16rpx;
  padding: 0 6rpx;
}
.status-tag {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
  white-space: nowrap;
}
.status-pending {
  background: #fef3c7;
  color: #92400e;
}
.status-progress {
  background: #dbeafe;
  color: #1e40af;
}
.status-done {
  background: #d1fae5;
  color: #065f46;
}
```

- [ ] **Step 4: index.json**

```json
{
  "enablePullDownRefresh": true,
  "navigationBarTitleText": "我的任务"
}
```

---

### Task 15: 创建任务详情页

**Files:**
- Create: `miniprogram/pages/tasks/detail.wxml`
- Create: `miniprogram/pages/tasks/detail.wxss`
- Create: `miniprogram/pages/tasks/detail.js`
- Create: `miniprogram/pages/tasks/detail.json`

- [ ] **Step 1: detail.js**

```javascript
const api = require('../../services/api');

Page({
  data: {
    id: '',
    detail: null,
    task: null,
    order: null,
    layoutResult: null,
    items: [],
    loading: false,
    transitioning: false
  },
  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id });
      this.loadDetail(options.id);
    }
  },
  async loadDetail(taskId) {
    this.setData({ loading: true });
    try {
      const detail = await api.getMyTaskDetail(taskId);
      const task = detail.task || {};
      const order = detail.order || {};
      const layoutResult = detail.layoutResult || {};
      const items = order.items || [];
      this.setData({ detail, task, order, layoutResult, items });
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ loading: false });
    }
  },
  async onStart() {
    await this.transition(1, '开始生产');
  },
  async onComplete() {
    await this.transition(2, '完成任务');
  },
  async transition(status, label) {
    this.setData({ transitioning: true });
    try {
      await api.transitionTask(this.data.id, status);
      wx.showToast({ title: label + '成功' });
      this.loadDetail(this.data.id);
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ transitioning: false });
    }
  },
  onDownloadNc() {
    const ncFilePath = this.data.layoutResult.ncFilePath;
    if (!ncFilePath) {
      wx.showToast({ title: 'NC文件不存在', icon: 'none' });
      return;
    }
    // ncFilePath 存储的是相对路径，拼接为完整的 HTTP URL
    const url = 'http://localhost:8080/uploads/' + ncFilePath;
    wx.downloadFile({
      url,
      success(res) {
        if (res.statusCode === 200) {
          wx.openDocument({
            filePath: res.tempFilePath,
            showMenu: true,
            fail() {
              wx.showToast({ title: '无法打开文件', icon: 'none' });
            }
          });
        }
      },
      fail() {
        wx.showToast({ title: '下载失败', icon: 'none' });
      }
    });
  }
});
```

- [ ] **Step 2: detail.wxml**

```xml
<view class="page">
  <!-- 加载中 -->
  <view class="section empty" wx:if="{{loading}}">加载中...</view>

  <block wx:if="{{task && !loading}}">
    <!-- 基本信息 -->
    <view class="section">
      <view class="between">
        <view class="title">{{task.taskName || '未命名任务'}}</view>
        <view class="status-tag {{task.status === 0 ? 'status-pending' : task.status === 1 ? 'status-progress' : 'status-done'}}">
          {{task.statusLabel}}
        </view>
      </view>
      <view class="field">
        <view class="label">订单号</view>
        <view>{{task.orderNo || '-'}}</view>
      </view>
      <view class="field" wx:if="{{task.estimatedHours}}">
        <view class="label">预计工时</view>
        <view>{{task.estimatedHours}}h</view>
      </view>
      <view class="field" wx:if="{{task.startTime}}">
        <view class="label">开始时间</view>
        <view>{{task.startTime}}</view>
      </view>
      <view class="field" wx:if="{{task.completeTime}}">
        <view class="label">完成时间</view>
        <view>{{task.completeTime}}</view>
      </view>
      <view class="field" wx:if="{{task.remark}}">
        <view class="label">备注</view>
        <view>{{task.remark}}</view>
      </view>
    </view>

    <!-- 板材信息 -->
    <view class="section" wx:if="{{items.length}}">
      <view class="section-title">板材信息</view>
      <view class="board-info">
        <view class="field">
          <view class="label">品牌</view>
          <view>{{items[0].brand || '-'}}</view>
        </view>
        <view class="field">
          <view class="label">材质</view>
          <view>{{items[0].materialName || '-'}}</view>
        </view>
        <view class="field">
          <view class="label">颜色</view>
          <view>{{items[0].color || '-'}}</view>
        </view>
        <view class="field">
          <view class="label">厚度</view>
          <view>{{items[0].thickness ? items[0].thickness + 'mm' : '-'}}</view>
        </view>
      </view>
    </view>

    <!-- 部件明细 -->
    <view class="section" wx:if="{{items.length}}">
      <view class="section-title">部件明细</view>
      <view class="table-header">
        <view class="col-name">名称</view>
        <view class="col-size">尺寸</view>
        <view class="col-qty">数量</view>
      </view>
      <view class="table-row" wx:for="{{items}}" wx:key="itemId">
        <view class="col-name">{{item.partName || '-'}}</view>
        <view class="col-size">{{item.length}}×{{item.width}}</view>
        <view class="col-qty">{{item.quantity}}</view>
      </view>
    </view>

    <!-- NC 文件下载 -->
    <view class="section" wx:if="{{layoutResult.ncFilePath}}">
      <view class="section-title">NC 文件</view>
      <button class="secondary" bindtap="onDownloadNc">下载 NC 文件</button>
    </view>

    <!-- 操作按钮 -->
    <view class="action-bar" wx:if="{{task.status === 0 || task.status === 1}}">
      <button wx:if="{{task.status === 0}}" class="primary" loading="{{transitioning}}" bindtap="onStart">开始生产</button>
      <button wx:if="{{task.status === 1}}" class="primary" loading="{{transitioning}}" bindtap="onComplete">完成任务</button>
    </view>
  </block>
</view>
```

- [ ] **Step 3: detail.wxss**

```css
.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16rpx;
}
.board-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
}
.table-header {
  display: flex;
  background: #f3f4f6;
  padding: 12rpx 16rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #374151;
  border-radius: 8rpx 8rpx 0 0;
}
.table-row {
  display: flex;
  padding: 12rpx 16rpx;
  font-size: 24rpx;
  border-bottom: 1rpx solid #e5e7eb;
}
.col-name { flex: 2; }
.col-size { flex: 2; text-align: center; }
.col-qty { flex: 1; text-align: center; }
.action-bar {
  padding: 24rpx;
}
.status-tag {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
  white-space: nowrap;
}
.status-pending { background: #fef3c7; color: #92400e; }
.status-progress { background: #dbeafe; color: #1e40af; }
.status-done { background: #d1fae5; color: #065f46; }
```

- [ ] **Step 4: detail.json**

```json
{
  "navigationBarTitleText": "任务详情"
}
```

---

### Task 16: 创建个人设置页（tabBar）

**Files:**
- Create: `miniprogram/pages/profile/index.wxml`
- Create: `miniprogram/pages/profile/index.wxss`
- Create: `miniprogram/pages/profile/index.js`
- Create: `miniprogram/pages/profile/index.json`

- [ ] **Step 1: index.js**

```javascript
const api = require('../../services/api');
const app = getApp();

Page({
  data: {
    userInfo: null,
    showPasswordDialog: false,
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    saving: false
  },
  onShow() {
    this.loadProfile();
  },
  async loadProfile() {
    try {
      const userInfo = await api.getProfile();
      this.setData({ userInfo });
    } catch (err) {
      // 如果 /users/me 不可用，从 storage 读取
      const userInfo = wx.getStorageSync('userInfo');
      if (userInfo) this.setData({ userInfo });
    }
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value });
  },
  showChangePassword() {
    this.setData({ showPasswordDialog: true, oldPassword: '', newPassword: '', confirmPassword: '' });
  },
  hideChangePassword() {
    this.setData({ showPasswordDialog: false });
  },
  async onChangePassword() {
    const { oldPassword, newPassword, confirmPassword } = this.data;
    if (!oldPassword || !newPassword) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }
    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '两次密码不一致', icon: 'none' });
      return;
    }
    this.setData({ saving: true });
    try {
      await api.changePassword(oldPassword, newPassword);
      wx.showToast({ title: '密码修改成功' });
      this.hideChangePassword();
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ saving: false });
    }
  },
  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success(res) {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          app.globalData.token = '';
          app.globalData.userInfo = null;
          wx.reLaunch({ url: '/pages/login/login' });
        }
      }
    });
  }
});
```

- [ ] **Step 2: index.wxml**

```xml
<view class="page">
  <!-- 用户信息 -->
  <view class="section">
    <view class="title">个人信息</view>
    <view class="field">
      <view class="label">用户名</view>
      <view>{{userInfo.username || '-'}}</view>
    </view>
    <view class="field" wx:if="{{userInfo.realName}}">
      <view class="label">姓名</view>
      <view>{{userInfo.realName}}</view>
    </view>
    <view class="field" wx:if="{{userInfo.orgRole}}">
      <view class="label">角色</view>
      <view>{{userInfo.orgRole === 'viewer' ? '生产员' : userInfo.orgRole}}</view>
    </view>
  </view>

  <!-- 操作 -->
  <view class="section">
    <button class="secondary" bindtap="showChangePassword">修改密码</button>
    <button class="danger" style="margin-top:16rpx;" bindtap="onLogout">退出登录</button>
  </view>

  <!-- 修改密码弹窗 -->
  <view class="dialog-mask" wx:if="{{showPasswordDialog}}" bindtap="hideChangePassword">
    <view class="dialog" catchtap="">
      <view class="title">修改密码</view>
      <view class="field">
        <view class="label">旧密码</view>
        <input data-field="oldPassword" bindinput="onInput" type="password" placeholder="请输入旧密码" />
      </view>
      <view class="field">
        <view class="label">新密码</view>
        <input data-field="newPassword" bindinput="onInput" type="password" placeholder="请输入新密码" />
      </view>
      <view class="field">
        <view class="label">确认密码</view>
        <input data-field="confirmPassword" bindinput="onInput" type="password" placeholder="请再次输入新密码" />
      </view>
      <view class="row" style="margin-top:24rpx;">
        <button class="ghost" bindtap="hideChangePassword">取消</button>
        <button class="primary" loading="{{saving}}" bindtap="onChangePassword">确定</button>
      </view>
    </view>
  </view>
</view>
```

- [ ] **Step 3: index.wxss**

```css
.dialog-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.dialog {
  width: 80%;
  background: #fff;
  border-radius: 12rpx;
  padding: 32rpx;
}
```

- [ ] **Step 4: index.json**

```json
{
  "navigationBarTitleText": "个人设置"
}
```

---

### Task 17: 创建通知列表页

**Files:**
- Create: `miniprogram/pages/notifications/index.wxml`
- Create: `miniprogram/pages/notifications/index.wxss`
- Create: `miniprogram/pages/notifications/index.js`
- Create: `miniprogram/pages/notifications/index.json`

- [ ] **Step 1: index.js**

```javascript
const api = require('../../services/api');

Page({
  data: {
    notifications: [],
    pageNum: 1,
    pageSize: 20,
    total: 0,
    loading: false
  },
  onShow() {
    this.loadNotifications();
  },
  async loadNotifications() {
    this.setData({ loading: true });
    try {
      const res = await api.listNotifications(this.data.pageNum, this.data.pageSize);
      const records = res.records || res || [];
      this.setData({
        notifications: records,
        total: res.total || records.length
      });
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ loading: false });
    }
  },
  async onTapNotification(e) {
    const item = e.currentTarget.dataset.item;
    // 标记已读
    if (!item.isRead) {
      try {
        await api.markNotificationRead(item.id);
        // 更新本地状态
        const notifications = this.data.notifications.map(n => {
          if (n.id === item.id) return { ...n, isRead: 1 };
          return n;
        });
        this.setData({ notifications });
      } catch (err) {
        // 静默失败
      }
    }
    // 跳转到任务详情
    if (item.taskId) {
      wx.navigateTo({ url: `/pages/tasks/detail?id=${item.taskId}` });
    }
  },
  async onMarkAllRead() {
    try {
      await api.markAllNotificationsRead();
      const notifications = this.data.notifications.map(n => ({ ...n, isRead: 1 }));
      this.setData({ notifications });
      wx.showToast({ title: '全部已读' });
    } catch (err) {
      // 错误已在 request.js 中处理
    }
  },
  onPullDownRefresh() {
    this.loadNotifications().finally(() => wx.stopPullDownRefresh());
  }
});
```

- [ ] **Step 2: index.wxml**

```xml
<view class="page">
  <view class="between section">
    <view class="title">通知</view>
    <button class="ghost" style="font-size:24rpx;padding:8rpx 16rpx;" bindtap="onMarkAllRead">全部已读</button>
  </view>

  <view class="section" wx:if="{{notifications.length}}">
    <view class="list-item" wx:for="{{notifications}}" wx:key="id"
          data-item="{{item}}" bindtap="onTapNotification">
      <view class="between">
        <view class="item-title" style="{{item.isRead ? 'color:#6b7280' : ''}}">
          <text wx:if="{{!item.isRead}}" class="dot">●</text>
          {{item.title}}
        </view>
        <view class="muted">{{item.createdAt}}</view>
      </view>
      <view class="item-meta">{{item.content}}</view>
    </view>
  </view>

  <view class="section empty" wx:if="{{!loading && !notifications.length}}">暂无通知</view>
  <view class="section empty" wx:if="{{loading}}">加载中...</view>
</view>
```

- [ ] **Step 3: index.wxss**

```css
.dot {
  color: #ef4444;
  font-size: 20rpx;
  margin-right: 8rpx;
}
```

- [ ] **Step 4: index.json**

```json
{
  "enablePullDownRefresh": true,
  "navigationBarTitleText": "通知"
}
```

---

## Web 前端清理任务

### Task 18: 删除生产员专属视图文件

**Files:**
- Delete: `frontend/src/views/ProducerTasksView.vue`
- Delete: `frontend/src/views/ProductionMyOrdersView.vue`

- [ ] **Step 1: 删除文件**

```bash
rm frontend/src/views/ProducerTasksView.vue
rm frontend/src/views/ProductionMyOrdersView.vue
```

---

### Task 19: 清理路由配置

**Files:**
- Modify: `frontend/src/router/index.js`

- [ ] **Step 1: 移除 lazy imports**

删除以下两行（约 line 16 和 18）：
```js
const ProductionMyOrdersView = () => import('@/views/ProductionMyOrdersView.vue');
const ProducerTasksView = () => import('@/views/ProducerTasksView.vue');
```

- [ ] **Step 2: 移除路由定义**

删除 `producer-tasks` 路由对象（约 line 57-61）：
```js
{
  path: 'producer/tasks',
  name: 'producer-tasks',
  component: ProducerTasksView,
  meta: { title: '我的任务', perm: 'production:read' }
},
```

删除 `production-my-orders` 路由对象（约 line 104-109）：
```js
{
  path: 'production/my-orders',
  name: 'production-my-orders',
  component: ProductionMyOrdersView,
  meta: { title: '我的生产订单', perm: 'order:read' }
},
```

- [ ] **Step 3: 修改 beforeEach 守卫中的 producer 重定向**

将 line 152 的：
```js
if (auth.isProducer) return { name: 'producer-tasks' };
```
改为：
```js
if (auth.isProducer) return { name: 'profile' };
```

将 line 164 的：
```js
if (auth.isProducer) return { name: 'producer-tasks' };
```
改为：
```js
if (auth.isProducer) return { name: 'profile' };
```

---

### Task 20: 清理 AppShell 侧边栏

**Files:**
- Modify: `frontend/src/components/AppShell.vue`

- [ ] **Step 1: 移除 producer-tasks 导航项**

删除（约 line 28）：
```js
{ name: 'producer-tasks', label: '我的任务', perm: 'production:read', producerOnly: true },
```

- [ ] **Step 2: 移除 producerOnly 过滤分支**

删除（约 line 37）：
```js
if (item.producerOnly) return auth.isProducer;
```

---

### Task 21: 清理 production-tasks.js API

**Files:**
- Modify: `frontend/src/api/production-tasks.js`

- [ ] **Step 1: 移除仅被删除视图使用的函数**

删除以下三个函数：
- `listMyTasks`（约 line 47）
- `getMyTaskDetail`（约 line 51）
- `myTransitionTask`（约 line 27）

保留 `kanbanData`、`deleteTask`、`assignTask`、`assignOrderTask` 等被其他视图使用的函数。

---

### Task 22: 在 ProfileView 添加生产员提示

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: 添加生产员提示条**

在 `ProfileView.vue` 的模板顶部（`<template>` 内的第一个 `<div>` 之后），添加：

```html
<div v-if="auth.isProducer" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4 text-yellow-800">
  生产员请使用微信小程序处理生产任务
</div>
```

确保在 `<script setup>` 中导入了 auth store：
```js
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
```

---

## 验证

### 后端验证

1. 执行 SQL 迁移，`SHOW COLUMNS FROM t_notification;` 确认表结构
2. 启动后端，用 curl 测试：
   - `GET /notifications` — 应返回空列表
   - `GET /users/me` — 应返回当前用户信息
   - `PUT /users/me/password` — 应正常工作
3. 分配一个生产任务，验证 `t_notification` 表中有新记录

### 小程序验证

1. 用微信开发者工具打开 `miniprogram/` 目录
2. viewer 角色登录 → 跳转我的任务
3. 非 viewer 角色登录 → 显示"请使用电脑端"
4. 查看任务列表 → 点击进入详情
5. 详情页显示板材信息和部件明细
6. 开始生产 → 状态变为生产中
7. 完成任务 → 状态变为已完成
8. 铃铛图标 → 通知列表 → 点击通知跳转详情
9. 个人设置 → 修改密码 → 退出登录

### Web 前端验证

1. viewer 角色登录 → 重定向到个人设置，显示"请使用小程序"提示
2. operator 角色登录 → 正常功能不受影响
3. 确认侧边栏不再显示"我的任务"
