package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TProductionTaskDTO {

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称不能超过100字")
    private String taskName;

    @Positive(message = "订单ID必须大于0")
    private Long orderId;

    private Long layoutResultId;

    private Long assigneeId;

    @Size(max = 50, message = "工人姓名不能超过50字")
    private String assigneeName;

    @Positive(message = "预估工时必须大于0")
    private BigDecimal estimatedHours;

    private BigDecimal actualHours;

    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}
