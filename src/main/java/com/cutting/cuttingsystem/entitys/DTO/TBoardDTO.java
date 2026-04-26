package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TBoardDTO {
    @NotBlank(message = "板材品牌不能为空")
    private String brand;

    @NotBlank(message = "板材材质不能为空")
    private String materialType;

    @NotBlank(message = "板材颜色不能为空")
    private String color;

    @NotBlank(message = "板材尺寸类型不能为空")
    private String sizeType;

    @NotNull(message = "板材宽度不能为空")
    @Positive(message = "板材宽度必须大于0")
    private Integer width;

    @NotNull(message = "板材长度不能为空")
    @Positive(message = "板材长度必须大于0")
    private Integer length;

    @NotNull(message = "板材厚度不能为空")
    @Positive(message = "板材厚度必须大于0")
    private Integer thickness;

    private String remark;
}
