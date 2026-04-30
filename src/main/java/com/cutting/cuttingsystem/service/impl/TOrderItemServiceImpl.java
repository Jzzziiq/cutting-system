package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TOrderItem;
import com.cutting.cuttingsystem.mapper.TOrderItemMapper;
import com.cutting.cuttingsystem.service.TOrderItemService;
import org.springframework.stereotype.Service;

@Service
public class TOrderItemServiceImpl extends ServiceImpl<TOrderItemMapper, TOrderItem> implements TOrderItemService {
}
