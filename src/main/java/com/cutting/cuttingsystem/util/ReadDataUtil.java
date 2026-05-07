package com.cutting.cuttingsystem.util;

import com.cutting.cuttingsystem.entitys.algorithm.DTO.InstanceDTO;
import com.cutting.cuttingsystem.entitys.algorithm.Instance;
import com.cutting.cuttingsystem.entitys.algorithm.Solution;
import com.cutting.cuttingsystem.entitys.algorithm.Square;
import com.cutting.cuttingsystem.model.AlgorithmRegistry;
import com.cutting.cuttingsystem.model.CuttingAlgorithm;
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

    public static List<Solution> getSolution(String jsonStr, String algorithmName, AlgorithmRegistry registry) throws JsonProcessingException {
        ReadDataUtil util = new ReadDataUtil();
        Instance originInstance = util.getInstanceFromJson(jsonStr);
        List<Square> remainingSquares = new ArrayList<>();
        for (Square sq : originInstance.getSquareList()) {
            remainingSquares.add(new Square(UUID.randomUUID().toString(), sq.getL(), sq.getW()));
        }

        List<Solution> allContainerSolutions = new ArrayList<>();
        int containerCount = 0;

        while (!remainingSquares.isEmpty()) {
            containerCount++;
            log.info("正在装箱第 {} 个容器（剩余矩形数：{}，算法：{}）", containerCount, remainingSquares.size(), algorithmName);

            Instance currentInstance = new Instance();
            currentInstance.setL(originInstance.getL());
            currentInstance.setW(originInstance.getW());
            currentInstance.setRotateEnable(originInstance.isRotateEnable());
            currentInstance.setGapDistance(originInstance.getGapDistance());
            currentInstance.setSquareList(new ArrayList<>(remainingSquares));

            try {
                CuttingAlgorithm algorithm = registry.create(algorithmName, currentInstance);
                Solution bestSolution = algorithm.search();
                int removedCount = removePackedSquares(remainingSquares, bestSolution, originInstance.isRotateEnable());

                if (removedCount == 0) {
                    log.warn("第 {} 个容器未装入任何矩形，尝试单件兜底评估", containerCount);
                    bestSolution = findBestSingleSquareSolution(originInstance, remainingSquares, algorithmName, registry);
                    removedCount = removePackedSquares(remainingSquares, bestSolution, originInstance.isRotateEnable());
                }

                if (removedCount == 0) {
                    throw new IllegalArgumentException("存在无法装入容器的矩形");
                }

                allContainerSolutions.add(bestSolution);
                log.info("第 {} 个容器利用率：{}，装入矩形数：{}", containerCount, bestSolution.getRate(), removedCount);
            } catch (Exception e) {
                log.error("第 {} 个容器装箱失败", containerCount, e);
                throw new IllegalStateException("算法装箱失败", e);
            }
        }
        return allContainerSolutions;
    }

    public List<Solution> getSolution(String jsonStr) throws JsonProcessingException {
        return getSolution(jsonStr, "tabu_search", new AlgorithmRegistry());
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

    private static Solution findBestSingleSquareSolution(Instance originInstance, List<Square> remainingSquares, String algorithmName, AlgorithmRegistry registry) throws Exception {
        Solution bestSolution = null;
        for (Square square : remainingSquares) {
            Instance singleSquareInstance = new Instance();
            singleSquareInstance.setL(originInstance.getL());
            singleSquareInstance.setW(originInstance.getW());
            singleSquareInstance.setRotateEnable(originInstance.isRotateEnable());
            singleSquareInstance.setGapDistance(originInstance.getGapDistance());
            singleSquareInstance.setSquareList(List.of(square));

            CuttingAlgorithm alg = registry.create(algorithmName, singleSquareInstance);
            Solution solution = alg.search();
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
