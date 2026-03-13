package com.cutting.cuttingsystem.util;

import com.cutting.cuttingsystem.entitys.algorithm.DTO.InstanceDTO;
import com.cutting.cuttingsystem.entitys.algorithm.Instance;
import com.cutting.cuttingsystem.entitys.algorithm.Solution;
import com.cutting.cuttingsystem.entitys.algorithm.Square;
import com.cutting.cuttingsystem.model.TabuSearch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReadDataUtil {
    private static Instance originInstance;

    /**
     * 从 JSON 字符串解析 Instance 对象（新增方法，用于接收前端传来的 JSON）
     *
     * @param jsonStr 前端传来的 JSON 字符串
     * @return Instance 对象
     */
    public Instance getInstanceFromJson(String jsonStr) {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. 解析 JSON 为 DTO 对象
        InstanceDTO dto = null;
        try {
            dto = objectMapper.readValue(jsonStr, InstanceDTO.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // 2. 将 DTO 转换为 Instance 对象
        Instance instance = new Instance();
        instance.setL(dto.getL());
        instance.setW(dto.getW());
        instance.setRotateEnable(dto.isRotateEnable());
        instance.setGapDistance(dto.getGapDistance());

        // 3. 转换 Square 列表，为每个 Square 生成 UUID
        List<Square> squareList = new ArrayList<>();
        for (com.cutting.cuttingsystem.entitys.algorithm.Square square : dto.getSquareList()) {
            squareList.add(new Square(
                    UUID.randomUUID().toString(),  // 自动生成唯一 ID
                    square.getL(),
                    square.getW()
            ));
        }
        instance.setSquareList(squareList);

        return instance;
    }

    public List<Solution> getSolution(String jsonStr) throws JsonProcessingException {
        originInstance = getInstanceFromJson(jsonStr);
        // 初始化剩余矩形列表（深拷贝，避免修改原数据）
        List<Square> remainingSquares = new ArrayList<>();
        for (Square sq : originInstance.getSquareList()) {
            remainingSquares.add(new Square(UUID.randomUUID().toString(), sq.getL(), sq.getW()));
        }

        // 2. 初始化变量：存储所有容器的装箱结果、容器计数
        List<Solution> allContainerSolutions = new ArrayList<>();
        int containerCount = 0;
        // 3. 迭代装箱：直到剩余矩形为空
        while (!remainingSquares.isEmpty()) {
            containerCount++;
            System.out.println("===== 正在装箱第 " + containerCount + " 个容器（剩余矩形数：" + remainingSquares.size() + "） =====");

            // 3.1 构建当前容器的 Instance（400*400，禁用旋转）
            Instance currentInstance = new Instance();
            currentInstance.setL(originInstance.getL());
            currentInstance.setW(originInstance.getW());
            currentInstance.setRotateEnable(originInstance.isRotateEnable());
            currentInstance.setGapDistance(originInstance.getGapDistance());
            currentInstance.setSquareList(new ArrayList<>(remainingSquares));

            // 3.2 用 TabuSearch 找当前容器的最优装箱方案
            TabuSearch tabuSearch = null;
            try {
                tabuSearch = new TabuSearch(currentInstance);
                Solution bestSolution = tabuSearch.search();
                // 3.3 从剩余列表中移除当前容器已装的矩形
                removePackedSquares(remainingSquares, bestSolution);

                // 3.4 保存当前容器的装箱结果
                allContainerSolutions.add(bestSolution);
                // 3.5 打印当前容器的装箱信息
                System.out.println("第 " + containerCount + " 个容器利用率：" + bestSolution.getRate());
                System.out.println("第 " + containerCount + " 个容器装入矩形数：" + bestSolution.getPlaceSquareList().size());
                System.out.println("----------------------------------------");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return allContainerSolutions;
    }

    /**
     * 从剩余矩形列表中移除已装入当前容器的矩形
     * （通过尺寸匹配，因为原 Square 的 ID 是随机生成的，尺寸是唯一标识）
     */
    private static void removePackedSquares(List<Square> remainingSquares, Solution solution) {
        // 提取当前容器已装矩形的尺寸（l,w）
        List<Square> packedSquares = new ArrayList<>();
        solution.getPlaceSquareList().forEach(placeSq -> {
            packedSquares.add(new Square("", placeSq.getL(), placeSq.getW()));
        });

        // 从剩余列表中移除匹配的矩形（注意：尺寸完全一致才移除，且只移除一次）
        List<Square> toRemove = new ArrayList<>();
        for (Square packed : packedSquares) {
            for (Square remain : remainingSquares) {
                if ((remain.getL() == packed.getL() && remain.getW() == packed.getW())
                        || (originInstance.isRotateEnable() && remain.getL() == packed.getW() && remain.getW() == packed.getL())) {
                    toRemove.add(remain);
                    break; // 每个已装矩形只移除一个匹配项
                }
            }
        }
        remainingSquares.removeAll(toRemove);
    }
}
