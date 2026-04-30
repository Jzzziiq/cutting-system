package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TLayoutResultDTO {
    @NotNull(message = "orderId is required")
    @Positive(message = "orderId must be greater than 0")
    private Long orderId;

    @NotNull(message = "usageRate is required")
    @DecimalMin(value = "0.0", message = "usageRate must be at least 0")
    @DecimalMax(value = "1.0", message = "usageRate must be at most 1")
    private BigDecimal usageRate;

    @DecimalMin(value = "0.0", message = "totalArea must be at least 0")
    private BigDecimal totalArea;

    @Min(value = 0, message = "containerCount must be at least 0")
    private Integer containerCount;

    @NotBlank(message = "resultJson is required")
    private String resultJson;

    @Size(max = 500, message = "imagePath must be at most 500 characters")
    private String imagePath;

    @Size(max = 500, message = "ncFilePath must be at most 500 characters")
    private String ncFilePath;

    @Size(max = 500, message = "labelFilePath must be at most 500 characters")
    private String labelFilePath;
}
