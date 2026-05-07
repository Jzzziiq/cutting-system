package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.AlgorithmTask;
import com.cutting.cuttingsystem.entitys.algorithm.DTO.InstanceDTO;
import com.cutting.cuttingsystem.entitys.algorithm.DTO.SolutionResponseDTO;
import com.cutting.cuttingsystem.entitys.algorithm.Instance;
import com.cutting.cuttingsystem.entitys.algorithm.PlaceSquare;
import com.cutting.cuttingsystem.entitys.algorithm.Solution;
import com.cutting.cuttingsystem.entitys.algorithm.Square;
import com.cutting.cuttingsystem.mapper.AlgorithmTaskMapper;
import com.cutting.cuttingsystem.model.CuttingAlgorithm;
import com.cutting.cuttingsystem.service.AlgorithmService;
import com.cutting.cuttingsystem.util.ReadDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class AlgorithmServiceImpl extends ServiceImpl<AlgorithmTaskMapper, AlgorithmTask> implements AlgorithmService {

    @Autowired
    private com.cutting.cuttingsystem.model.AlgorithmRegistry algorithmRegistry;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public List<String> listAlgorithmNames() {
        return new ArrayList<>(algorithmRegistry.list().keySet());
    }

    public Map<String, String> listAlgorithms() {
        return algorithmRegistry.list();
    }

    @Override
    public AlgorithmTask submit(String algorithm, String inputJson, Long userId) {
        AlgorithmTask task = new AlgorithmTask();
        task.setTaskId(UUID.randomUUID().toString().substring(0, 8));
        task.setUserId(userId);
        task.setAlgorithm(algorithm);
        task.setInputJson(inputJson);
        task.setStatus(0);
        save(task);
        return task;
    }

    @Override
    public AlgorithmTask getTask(String taskId) {
        return getById(taskId);
    }

    @Override
    public List<AlgorithmTask> listTasksByUser(Long userId) {
        return list(new QueryWrapper<AlgorithmTask>()
                .eq("user_id", userId)
                .orderByDesc("create_time"));
    }

    @Override
    public void executeTask(String taskId) {
        doExecuteTask(taskId);
    }

    public void doExecuteTask(String taskId) {
        AlgorithmTask task = getById(taskId);
        if (task == null) return;
        task.setStatus(1);
        updateById(task);

        long start = System.currentTimeMillis();
        try {
            List<SolutionResponseDTO> results = executeSync(task.getAlgorithm(), task.getInputJson());
            double bestRate = results.stream().mapToDouble(SolutionResponseDTO::getRate).max().orElse(0);
            task.setResultJson(MAPPER.writeValueAsString(results));
            task.setBestRate(bestRate);
            task.setContainerCount(results.size());
            task.setDurationMs(System.currentTimeMillis() - start);
            task.setStatus(2);
        } catch (Exception e) {
            log.error("Algorithm task {} failed", taskId, e);
            task.setErrorMsg(e.getMessage() != null && e.getMessage().length() > 500
                    ? e.getMessage().substring(0, 500) : e.getMessage());
            task.setDurationMs(System.currentTimeMillis() - start);
            task.setStatus(-1);
        }
        task.setCompleteTime(new Date());
        updateById(task);
    }

    @Override
    public List<SolutionResponseDTO> executeSync(String algorithm, String inputJson) {
        List<Solution> solutions;
        try {
            solutions = ReadDataUtil.getSolution(inputJson, algorithm, algorithmRegistry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<SolutionResponseDTO> result = new ArrayList<>();
        for (Solution solution : solutions) {
            SolutionResponseDTO dto = new SolutionResponseDTO();
            Instance inst = solution.getInstance();
            dto.setContainerLength(inst != null ? inst.getL() : 0);
            dto.setContainerWidth(inst != null ? inst.getW() : 0);
            dto.setRate(solution.getRate());
            dto.setSquareList(solution.getSquareList());
            dto.setPlaceSquareList(solution.getPlaceSquareList());
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<List<SolutionResponseDTO>> compare(String inputJson, List<String> algorithms) {
        List<List<SolutionResponseDTO>> results = new ArrayList<>();
        for (String algo : algorithms) {
            results.add(executeSync(algo, inputJson));
        }
        return results;
    }
}
