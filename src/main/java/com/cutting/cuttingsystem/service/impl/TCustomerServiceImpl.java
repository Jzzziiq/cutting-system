package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TCustomer;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.mapper.TCustomerMapper;
import com.cutting.cuttingsystem.mapper.TOrderMapper;
import com.cutting.cuttingsystem.service.TCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class TCustomerServiceImpl extends ServiceImpl<TCustomerMapper, TCustomer> implements TCustomerService {
    private static final String SINGLE_REFERENCED_MESSAGE =
            "该客户已有订单引用，不能删除；如暂不使用，请改为禁用";
    private static final String BATCH_REFERENCED_MESSAGE =
            "存在已有订单引用的客户，不能删除；如暂不使用，请改为禁用";

    @Autowired
    private TOrderMapper orderMapper;

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
        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<TOrder>().in(TOrder::getCustomerId, ids));
        if (orderCount != null && orderCount > 0) {
            throw new IllegalStateException(message);
        }
    }
}
