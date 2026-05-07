package com.cutting.cuttingsystem.entitys.DTO;

import lombok.Data;

@Data
public class OrderStatusTransitionDTO {
    private Integer targetStatus;
    private String remark;
}
