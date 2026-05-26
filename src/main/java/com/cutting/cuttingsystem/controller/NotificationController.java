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
