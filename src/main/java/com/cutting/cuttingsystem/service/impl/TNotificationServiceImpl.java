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
