package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.TCustomer;

import java.util.Collection;

public interface TCustomerService extends IService<TCustomer> {
    boolean removeByIdIfUnused(Long id);

    boolean removeByIdsIfUnused(Collection<Long> ids);
}
