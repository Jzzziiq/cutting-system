package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.TBoard;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TBoardService extends IService<TBoard> {
    boolean removeByIdIfUnused(Long id);

    boolean removeByIdsIfUnused(Collection<Long> ids);

    Map<String, List<Object>> listBoardOptions();
}
