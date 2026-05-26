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
