package com.cutting.cuttingsystem.service;

import com.cutting.cuttingsystem.entitys.DTO.SplitConfirmRequest;
import com.cutting.cuttingsystem.entitys.DTO.SplitExecuteRequest;
import com.cutting.cuttingsystem.entitys.VO.SplitConfirmResultVO;
import com.cutting.cuttingsystem.entitys.VO.SplitItemVO;

import java.util.List;

public interface OrderSplitService {
    List<SplitItemVO> execute(SplitExecuteRequest request);
    SplitConfirmResultVO confirm(SplitConfirmRequest request);
}
