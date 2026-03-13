package com.cutting.cuttingsystem.entitys.algorithm;


import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 放置点实体类
 * 用于表示二维平面上的一个放置点位置，支持按 Y 坐标优先、X 坐标次之的排序规则
 * 实现了 Comparable 接口，用于放置点的优先级排序
 *
 * @author Packing Algorithm
 * @version 1.0
 */
@Data
@NoArgsConstructor
public class PlacePoint implements Comparable<PlacePoint> {
    /**
     * 放置点的坐标
     * x: X 轴坐标（水平方向）
     * y: Y 轴坐标（垂直方向）
     */
    private double x, y, len;

    public PlacePoint(double x, double y, double len) {
        this.x = x;
        this.y = y;
        this.len = len;
    }

    public PlacePoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 比较当前放置点与另一个放置点的优先级
     * 排序规则：优先按 Y 坐标升序排列（Y 值小的在前），Y 坐标相同时按 X 坐标升序排列（X 值小的在前）
     * 该排序策略确保放置点优先从下往上排列，同一水平线上优先从左往右排列
     *
     * @param o 待比较的另一个 PlacePoint 对象
     * @return 比较结果：负数表示当前对象优先级高，零表示优先级相同，正数表示参数对象优先级高
     */
    @Override
    public int compareTo(PlacePoint o) {
        // 优先往下排 然后优先往左排
        int compare_y = Double.compare(y, o.y);
        if (compare_y != 0) {
            return compare_y;
        } else {
            return Double.compare(x, o.x);
        }
    }
}
