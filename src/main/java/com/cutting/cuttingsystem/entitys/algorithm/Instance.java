package com.cutting.cuttingsystem.entitys.algorithm;


import lombok.Data;

import java.util.List;

/**
 * 装箱问题实例实体类
 * 用于描述一个装箱问题的具体实例，包含容器的尺寸约束和待装入的物品列表
 *
 * @author Packing Algorithm
 * @version 1.0
 */
@Data
public class Instance {
    /**
     * 容器的长度和宽度
     * L: 容器的长度维度
     * W: 容器的宽度维度
     */
    private double L, W;

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
    private double gapDistance = 0;
    /**
     * 待装入的物品列表
     * 包含所有需要装入容器的 Square 对象
     */
    private List<Square> squareList;
}
