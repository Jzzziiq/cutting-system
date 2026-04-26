package com.cutting.cuttingsystem.entitys.algorithm.DTO;

import com.cutting.cuttingsystem.entitys.algorithm.Square;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.List;

/**
 * 装箱问题实例数据传输对象
 * 用于在系统中传递装箱问题实例的相关数据，包含容器尺寸、旋转启用标志和待装入物品列表
 *
 * @author Packing Algorithm
 * @version 1.0
 */
@Data
public class InstanceDTO {
    /**
     * 容器的长度和宽度
     * L: 容器的长度维度
     * W: 容器的宽度维度
     */
    @JsonProperty("L")
    @Positive(message = "容器长度必须大于0")
    private double L;

    @JsonProperty("W")
    @Positive(message = "容器宽度必须大于0")
    private double W;

    /**
     * 物品旋转启用标志
     * 用于控制物品在装箱过程中是否允许旋转操作
     * 默认值为 true，表示允许旋转
     */
    private boolean isRotateEnable = false;
    /**
     * 矩形之间的间隔距离
     * 用于控制装入容器的矩形之间需要保持的最小距离
     * 默认值为 0，表示不需要间隔
     */
    @PositiveOrZero(message = "间隙距离不能小于0")
    private double gapDistance = 0;
    /**
     * 待装入的物品列表
     * 包含所有需要装入容器的 Square 对象
     */
    @Valid
    @NotEmpty(message = "待装入物品列表不能为空")
    private List<Square> squareList;
}
