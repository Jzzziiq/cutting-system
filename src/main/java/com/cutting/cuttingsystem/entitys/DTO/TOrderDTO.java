package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TOrderDTO {
    @Size(max = 50, message = "orderNo must be at most 50 characters")
    private String orderNo;

    @NotNull(message = "customerId is required")
    @Positive(message = "customerId must be greater than 0")
    private Long customerId;

    @Size(max = 100, message = "customerName must be at most 100 characters")
    private String customerName;

    @Size(max = 255, message = "customerAddress must be at most 255 characters")
    private String customerAddress;

    @NotBlank(message = "processName is required")
    @Size(max = 100, message = "processName must be at most 100 characters")
    private String processName;

    @Min(value = 0, message = "orderStatus must be at least 0")
    @Max(value = 9, message = "orderStatus must be at most 9")
    private Integer orderStatus;

    private String rawMaterialJson;

    private String remnantJson;

    private String configJson;

    @Positive(message = "layoutResultId must be greater than 0")
    private Long layoutResultId;

    @Size(max = 255, message = "remark must be at most 255 characters")
    private String remark;

    @Valid
    @Size(max = 200, message = "items must be at most 200 rows")
    private List<TOrderItemDTO> items;
}
