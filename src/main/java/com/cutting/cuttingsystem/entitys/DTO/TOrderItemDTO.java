package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TOrderItemDTO {
    @Positive(message = "orderId must be greater than 0")
    private Long orderId;

    @NotBlank(message = "partName is required")
    @Size(max = 100, message = "partName must be at most 100 characters")
    private String partName;

    @Size(max = 100, message = "partCode must be at most 100 characters")
    private String partCode;

    @Positive(message = "boardId must be greater than 0")
    private Long boardId;

    @Positive(message = "offcutId must be greater than 0")
    private Long offcutId;

    @NotNull(message = "width is required")
    @Positive(message = "width must be greater than 0")
    private Integer width;

    @NotNull(message = "length is required")
    @Positive(message = "length must be greater than 0")
    private Integer length;

    @NotNull(message = "thickness is required")
    @Positive(message = "thickness must be greater than 0")
    private Integer thickness;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    private Integer quantity;

    @Size(max = 100, message = "materialName must be at most 100 characters")
    private String materialName;

    @Size(max = 50, message = "color must be at most 50 characters")
    private String color;

    @Min(value = 0, message = "edgeLeft must be 0 or 1")
    @Max(value = 1, message = "edgeLeft must be 0 or 1")
    private Integer edgeLeft;

    @Min(value = 0, message = "edgeRight must be 0 or 1")
    @Max(value = 1, message = "edgeRight must be 0 or 1")
    private Integer edgeRight;

    @Min(value = 0, message = "edgeFront must be 0 or 1")
    @Max(value = 1, message = "edgeFront must be 0 or 1")
    private Integer edgeFront;

    @Min(value = 0, message = "edgeBack must be 0 or 1")
    @Max(value = 1, message = "edgeBack must be 0 or 1")
    private Integer edgeBack;

    @Min(value = 0, message = "edgeTop must be 0 or 1")
    @Max(value = 1, message = "edgeTop must be 0 or 1")
    private Integer edgeTop;

    @Min(value = 0, message = "edgeBottom must be 0 or 1")
    @Max(value = 1, message = "edgeBottom must be 0 or 1")
    private Integer edgeBottom;

    @Min(value = 0, message = "isTexture must be 0 or 1")
    @Max(value = 1, message = "isTexture must be 0 or 1")
    private Integer isTexture;

    @Min(value = 0, message = "allowRotation must be 0 or 1")
    @Max(value = 1, message = "allowRotation must be 0 or 1")
    private Integer allowRotation;

    @Size(max = 100, message = "label must be at most 100 characters")
    private String label;

    @Size(max = 255, message = "remark must be at most 255 characters")
    private String remark;
}
