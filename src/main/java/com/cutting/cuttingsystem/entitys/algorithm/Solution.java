package com.cutting.cuttingsystem.entitys.algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 装箱问题解决方案实体类
 * 用于存储和表示装箱问题的一个完整解决方案，包含空间利用率和具体的放置方案
 *
 * @author Packing Algorithm
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Solution {
    /**
     * 空间利用率
     * 表示已使用空间占总可用空间的比例，用于评估解决方案的优劣
     */
    private double rate;

    /**
     * 对应的装箱问题实例
     * 包含该解决方案所针对的原始问题实例数据
     */
    private Instance instance;

    /**
     * 待装入的物品列表
     * 包含所有需要装入容器的 Square 对象
     */
    private List<Square> squareList;

    /**
     * 已放置的物品列表
     * 包含所有已成功放置到容器中的 PlaceSquare 对象
     */
    private List<PlaceSquare> placeSquareList;
}
