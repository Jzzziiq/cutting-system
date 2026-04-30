package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TOffcutDTO {
    @NotNull(message = "boardId is required")
    @Positive(message = "boardId must be greater than 0")
    private Long boardId;

    @Positive(message = "sourceOrderId must be greater than 0")
    private Long sourceOrderId;

    @NotNull(message = "width is required")
    @Positive(message = "width must be greater than 0")
    private Integer width;

    @NotNull(message = "length is required")
    @Positive(message = "length must be greater than 0")
    private Integer length;

    @NotNull(message = "thickness is required")
    @Positive(message = "thickness must be greater than 0")
    private Integer thickness;

    @Size(max = 50, message = "materialType must be at most 50 characters")
    private String materialType;

    @Size(max = 50, message = "brand must be at most 50 characters")
    private String brand;

    @Size(max = 50, message = "color must be at most 50 characters")
    private String color;

    @Min(value = 0, message = "status must be at least 0")
    @Max(value = 9, message = "status must be at most 9")
    private Integer status;

    @Min(value = 0, message = "isEnabled must be 0 or 1")
    @Max(value = 1, message = "isEnabled must be 0 or 1")
    private Integer isEnabled;

    @Size(max = 255, message = "remark must be at most 255 characters")
    private String remark;
}
