package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TCustomer;
import com.cutting.cuttingsystem.mapper.TCustomerMapper;
import com.cutting.cuttingsystem.service.TCustomerService;
import org.springframework.stereotype.Service;

@Service
public class TCustomerServiceImpl extends ServiceImpl<TCustomerMapper, TCustomer> implements TCustomerService {
}
