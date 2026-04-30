package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.DTO.TLayoutResultDTO;
import com.cutting.cuttingsystem.entitys.TLayoutResult;

public interface TLayoutResultService extends IService<TLayoutResult> {
    TLayoutResult createResult(TLayoutResultDTO layoutResultDTO);

    boolean updateResult(Long resultId, TLayoutResultDTO layoutResultDTO);
}
