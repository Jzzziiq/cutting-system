package com.cutting.cuttingsystem.entitys.algorithm.DTO;

import com.cutting.cuttingsystem.entitys.algorithm.PlaceSquare;
import com.cutting.cuttingsystem.entitys.algorithm.Square;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 装箱问题解决方案响应数据传输对象
 * 用于向客户端返回完整的解决方案信息，包含容器尺寸、利用率和放置方案
 *
 * @author Packing Algorithm
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolutionResponseDTO {
    /**
     * 容器的长度
     */
    private double containerLength;

    /**
     * 容器的宽度
     */
    private double containerWidth;

    /**
     * 空间利用率
     */
    private double rate;

    /**
     * 待装入的物品列表
     */
    private List<Square> squareList;

    /**
     * 已放置的物品列表
     */
    private List<PlaceSquare> placeSquareList;
}
