package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QueryDTO {
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize;
}
