package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.entitys.TOffcut;
import com.cutting.cuttingsystem.entitys.TOrderItem;
import com.cutting.cuttingsystem.mapper.TOffcutMapper;
import com.cutting.cuttingsystem.mapper.TBoardMapper;
import com.cutting.cuttingsystem.mapper.TOrderItemMapper;
import com.cutting.cuttingsystem.service.TBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class TBoardServiceImpl extends ServiceImpl<TBoardMapper, TBoard> implements TBoardService {
    private static final String SINGLE_REFERENCED_MESSAGE =
            "该板材已被订单明细或余料引用，不能删除；如暂不使用，请改为禁用";
    private static final String BATCH_REFERENCED_MESSAGE =
            "存在已被订单明细或余料引用的板材，不能删除；如暂不使用，请改为禁用";

    @Autowired
    private TOrderItemMapper orderItemMapper;

    @Autowired
    private TOffcutMapper offcutMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIdIfUnused(Long id) {
        assertNotReferenced(List.of(id), SINGLE_REFERENCED_MESSAGE);
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIdsIfUnused(Collection<Long> ids) {
        assertNotReferenced(ids, BATCH_REFERENCED_MESSAGE);
        return removeByIds(ids);
    }

    private void assertNotReferenced(Collection<Long> ids, String message) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Long orderItemCount = orderItemMapper.selectCount(
                new LambdaQueryWrapper<TOrderItem>().in(TOrderItem::getBoardId, ids));
        Long offcutCount = offcutMapper.selectCount(
                new LambdaQueryWrapper<TOffcut>().in(TOffcut::getBoardId, ids));
        if (hasRows(orderItemCount) || hasRows(offcutCount)) {
            throw new IllegalStateException(message);
        }
    }

    private boolean hasRows(Long count) {
        return count != null && count > 0;
    }
}
