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
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSquare {
    private String id;
    private double x, y, l, w;

    public PlaceSquare(double x, double y, double l, double w) {
        this(null, x, y, l, w);
    }
}
