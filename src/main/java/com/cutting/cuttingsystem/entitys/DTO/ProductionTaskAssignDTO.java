package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProductionTaskAssignDTO {
    @NotNull(message = "assigneeId不能为空")
    @Positive(message = "assigneeId必须大于0")
    private Long assigneeId;
}
