package com.cutting.cuttingsystem.util;

import com.cutting.cuttingsystem.entitys.algorithm.DTO.InstanceDTO;
import com.cutting.cuttingsystem.entitys.algorithm.Instance;
import com.cutting.cuttingsystem.entitys.algorithm.Solution;
import com.cutting.cuttingsystem.entitys.algorithm.Square;
import com.cutting.cuttingsystem.model.TabuSearch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class ReadDataUtil {

    /**
     * 从 JSON 字符串解析 Instance 对象（新增方法，用于接收前端传来的 JSON）
     *
     * @param jsonStr 前端传来的 JSON 字符串
     * @return Instance 对象
     */
    public Instance getInstanceFromJson(String jsonStr) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. 解析 JSON 为 DTO 对象
        InstanceDTO dto = objectMapper.readValue(jsonStr, InstanceDTO.class);

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
                    UUID.randomUUID().toString(),
                    square.getL(),
                    square.getW()
            ));
        }
        instance.setSquareList(squareList);

        return instance;
    }

    public List<Solution> getSolution(String jsonStr) throws JsonProcessingException {
        Instance originInstance = getInstanceFromJson(jsonStr);
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
            log.info("正在装箱第 {} 个容器（剩余矩形数：{}）", containerCount, remainingSquares.size());

            // 3.1 构建当前容器的 Instance
            Instance currentInstance = new Instance();
            currentInstance.setL(originInstance.getL());
            currentInstance.setW(originInstance.getW());
            currentInstance.setRotateEnable(originInstance.isRotateEnable());
            currentInstance.setGapDistance(originInstance.getGapDistance());
            currentInstance.setSquareList(new ArrayList<>(remainingSquares));

            // 3.2 用 TabuSearch 找当前容器的最优装箱方案
            try {
                TabuSearch tabuSearch = new TabuSearch(currentInstance);
                Solution bestSolution = tabuSearch.search();
                // 3.3 从剩余列表中移除当前容器已装的矩形
                int removedCount = removePackedSquares(remainingSquares, bestSolution, originInstance.isRotateEnable());

                if (removedCount == 0) {
                    log.warn("第 {} 个容器未装入任何矩形，尝试单件兜底评估", containerCount);
                    bestSolution = findBestSingleSquareSolution(originInstance, remainingSquares);
                    removedCount = removePackedSquares(remainingSquares, bestSolution, originInstance.isRotateEnable());
                }

                if (removedCount == 0) {
                    throw new IllegalArgumentException("存在无法装入容器的矩形，请检查矩形尺寸、容器尺寸或间隙距离");
                }

                // 3.4 保存当前容器的装箱结果
                allContainerSolutions.add(bestSolution);
                // 3.5 打印当前容器的装箱信息
                log.info("第 {} 个容器利用率：{}", containerCount, bestSolution.getRate());
                log.info("第 {} 个容器装入矩形数：{}", containerCount, removedCount);
            } catch (Exception e) {
                log.error("第 {} 个容器装箱失败", containerCount, e);
                throw new IllegalStateException("算法装箱失败", e);
            }
        }
        return allContainerSolutions;
    }

    /**
     * 从剩余矩形列表中移除已装入当前容器的矩形。
     */
    private static int removePackedSquares(List<Square> remainingSquares, Solution solution, boolean rotateEnable) {
        List<Square> packedSquares = new ArrayList<>();
        Set<String> matchedIds = new HashSet<>();

        solution.getPlaceSquareList().forEach(placeSq -> {
            for (Square sourceSquare : solution.getSquareList()) {
                if (matchedIds.contains(sourceSquare.getId())) {
                    continue;
                }
                if (isSameSize(sourceSquare, placeSq.getL(), placeSq.getW(), rotateEnable)) {
                    packedSquares.add(sourceSquare);
                    matchedIds.add(sourceSquare.getId());
                    break;
                }
            }
        });

        Set<String> packedIds = new HashSet<>();
        packedSquares.forEach(square -> packedIds.add(square.getId()));
        remainingSquares.removeIf(square -> packedIds.contains(square.getId()));
        return packedIds.size();
    }

    private static boolean isSameSize(Square square, double length, double width, boolean rotateEnable) {
        return (Double.compare(square.getL(), length) == 0 && Double.compare(square.getW(), width) == 0)
                || (rotateEnable && Double.compare(square.getL(), width) == 0 && Double.compare(square.getW(), length) == 0);
    }

    private static Solution findBestSingleSquareSolution(Instance originInstance, List<Square> remainingSquares) throws Exception {
        Solution bestSolution = null;
        for (Square square : remainingSquares) {
            Instance singleSquareInstance = new Instance();
            singleSquareInstance.setL(originInstance.getL());
            singleSquareInstance.setW(originInstance.getW());
            singleSquareInstance.setRotateEnable(originInstance.isRotateEnable());
            singleSquareInstance.setGapDistance(originInstance.getGapDistance());
            singleSquareInstance.setSquareList(List.of(square));

            Solution solution = new TabuSearch(singleSquareInstance).evaluate(singleSquareInstance.getSquareList());
            if (solution.getPlaceSquareList().isEmpty()) {
                continue;
            }
            if (bestSolution == null || solution.getRate() > bestSolution.getRate()) {
                bestSolution = solution;
            }
        }

        if (bestSolution != null) {
            return bestSolution;
        }

        Solution emptySolution = new Solution();
        emptySolution.setInstance(originInstance);
        emptySolution.setSquareList(new ArrayList<>(remainingSquares));
        emptySolution.setPlaceSquareList(new ArrayList<>());
        emptySolution.setRate(0);
        return emptySolution;
    }
}
