package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

import java.util.List;

@Data
public class SplitConfirmResultVO {
    private Long orderId;
    private String splitBatchCode;
    private String cabinetName;
    private List<Long> createdItemIds;
    private String nextAction;
}
