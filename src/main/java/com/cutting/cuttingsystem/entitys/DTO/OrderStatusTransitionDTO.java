package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderStatusTransitionDTO {
    @NotNull(message = "目标状态不能为空")
    @Min(value = -3, message = "无效的状态码")
    @Max(value = 6, message = "无效的状态码")
    private Integer targetStatus;

    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}
