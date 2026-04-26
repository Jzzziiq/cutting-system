package com.cutting.cuttingsystem.entitys.algorithm;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物品方块实体类
 * 用于表示一个待装入容器的物品，包含唯一标识符和尺寸信息
 *
 * @author Packing Algorithm
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Square {
    /**
     * 物品的唯一标识符
     * 用于区分不同的物品对象
     */
    private String id;

    /**
     * 物品的尺寸属性
     * l: 物品的长度
     * w: 物品的宽度
     */
    @Positive(message = "物品长度必须大于0")
    private double l;

    @Positive(message = "物品宽度必须大于0")
    private double w;
}
