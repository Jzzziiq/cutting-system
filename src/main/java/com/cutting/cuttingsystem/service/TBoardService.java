package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.TBoard;

import java.util.Collection;

public interface TBoardService extends IService<TBoard> {
    boolean removeByIdIfUnused(Long id);

    boolean removeByIdsIfUnused(Collection<Long> ids);
}
