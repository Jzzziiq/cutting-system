package com.cutting.cuttingsystem.service.impl;

import com.cutting.cuttingsystem.entitys.AlgorithmTask;
import com.cutting.cuttingsystem.entitys.algorithm.DTO.SolutionResponseDTO;
import com.cutting.cuttingsystem.service.AlgorithmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TaskRunner {

    @Autowired
    private AlgorithmService algorithmService;

    @Async("algorithmExecutor")
    public void run(String taskId) {
        ((AlgorithmServiceImpl) algorithmService).doExecuteTask(taskId);
    }
}
