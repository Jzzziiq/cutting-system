package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TBoardDTO {
    @NotBlank(message = "brand is required")
    @Size(max = 50, message = "brand must be at most 50 characters")
    private String brand;

    @NotBlank(message = "materialType is required")
    @Size(max = 50, message = "materialType must be at most 50 characters")
    private String materialType;

    @NotBlank(message = "color is required")
    @Size(max = 50, message = "color must be at most 50 characters")
    private String color;

    @NotBlank(message = "sizeType is required")
    @Size(max = 50, message = "sizeType must be at most 50 characters")
    private String sizeType;

    @NotNull(message = "width is required")
    @Positive(message = "width must be greater than 0")
    private Integer width;

    @NotNull(message = "length is required")
    @Positive(message = "length must be greater than 0")
    private Integer length;

    @NotNull(message = "thickness is required")
    @Positive(message = "thickness must be greater than 0")
    private Integer thickness;

    @Size(max = 255, message = "remark must be at most 255 characters")
    private String remark;
}
