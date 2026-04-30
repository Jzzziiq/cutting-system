package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.DTO.TOrderDTO;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.entitys.VO.TOrderVO;

public interface TOrderService extends IService<TOrder> {
    TOrderVO createOrder(TOrderDTO orderDTO);

    boolean updateOrder(Long orderId, TOrderDTO orderDTO);

    TOrderVO getOrderDetail(Long orderId);
}
