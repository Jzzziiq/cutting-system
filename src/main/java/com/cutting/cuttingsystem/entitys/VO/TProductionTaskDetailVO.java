package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

@Data
public class TProductionTaskDetailVO {
    private TProductionTaskVO task;
    private TOrderVO order;
    private TLayoutResultVO layoutResult;
}
