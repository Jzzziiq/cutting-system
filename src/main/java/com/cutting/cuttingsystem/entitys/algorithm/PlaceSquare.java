package com.cutting.cuttingsystem.entitys.algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 放置方块实体类
 * 用于表示一个已放置的方块对象，包含位置坐标和尺寸信息
 *
 * @author Packing Algorithm
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaceSquare {
    /**
     * 方块的位置和尺寸属性
     * x: X 轴坐标（水平位置）
     * y: Y 轴坐标（垂直位置）
     * l: 方块的长度
     * w: 方块的宽度
     */
    private double x, y, l, w;
}
