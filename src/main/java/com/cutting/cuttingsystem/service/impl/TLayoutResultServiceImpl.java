package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.DTO.TLayoutResultDTO;
import com.cutting.cuttingsystem.entitys.TLayoutResult;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.mapper.TLayoutResultMapper;
import com.cutting.cuttingsystem.service.TLayoutResultService;
import com.cutting.cuttingsystem.service.TOrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TLayoutResultServiceImpl extends ServiceImpl<TLayoutResultMapper, TLayoutResult>
        implements TLayoutResultService {
    @Autowired
    private TOrderService orderService;

    @Override
    @Transactional
    public TLayoutResult createResult(TLayoutResultDTO layoutResultDTO) {
        TLayoutResult layoutResult = new TLayoutResult();
        BeanUtils.copyProperties(layoutResultDTO, layoutResult);
        save(layoutResult);
        updateCurrentLayoutResult(layoutResult.getOrderId(), layoutResult.getResultId());
        return layoutResult;
    }

    @Override
    @Transactional
    public boolean updateResult(Long resultId, TLayoutResultDTO layoutResultDTO) {
        TLayoutResult layoutResult = new TLayoutResult();
        BeanUtils.copyProperties(layoutResultDTO, layoutResult);
        layoutResult.setResultId(resultId);
        boolean updated = updateById(layoutResult);
        if (updated) {
            updateCurrentLayoutResult(layoutResult.getOrderId(), resultId);
        }
        return updated;
    }

    private void updateCurrentLayoutResult(Long orderId, Long resultId) {
        TOrder order = new TOrder();
        order.setOrderId(orderId);
        order.setLayoutResultId(resultId);
        orderService.updateById(order);
    }
}
