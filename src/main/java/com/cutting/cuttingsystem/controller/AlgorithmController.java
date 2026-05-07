package com.cutting.cuttingsystem.controller;

import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.AlgorithmTask;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.algorithm.DTO.InstanceDTO;
import com.cutting.cuttingsystem.service.AlgorithmService;
import com.cutting.cuttingsystem.service.impl.TaskRunner;
import com.cutting.cuttingsystem.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/algorithm")
@RequirePermission("algorithm:execute")
public class AlgorithmController {

    @Autowired
    private AlgorithmService algorithmService;

    @Autowired
    private TaskRunner taskRunner;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @PostMapping("/submit")
    public Result submit(@RequestBody @Valid InstanceDTO input, @RequestParam(defaultValue = "tabu_search") String algorithm) {
        try {
            String json = MAPPER.writeValueAsString(input);
            AlgorithmTask task = algorithmService.submit(algorithm, json, UserContext.getCurrentUserId());
            taskRunner.run(task.getTaskId());
            return Result.success(Map.of("taskId", task.getTaskId(), "status", task.getStatus()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/result/{taskId}")
    public Result getResult(@PathVariable String taskId) {
        AlgorithmTask task = algorithmService.getTask(taskId);
        if (task == null) return Result.error("任务不存在");
        return Result.success(task);
    }

    @PostMapping("/compare")
    public Result compare(@RequestBody @Valid InstanceDTO input,
                          @RequestParam(defaultValue = "tabu_search,genetic_algorithm") String algorithms) {
        try {
            String json = MAPPER.writeValueAsString(input);
            List<String> algoList = List.of(algorithms.split(","));
            List<?> results = algorithmService.compare(json, algoList);
            return Result.success(results);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/algorithms")
    public Result algorithms() {
        var impl = (com.cutting.cuttingsystem.service.impl.AlgorithmServiceImpl) algorithmService;
        return Result.success(impl.listAlgorithms());
    }
}
