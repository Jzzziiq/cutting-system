package com.cutting.cuttingsystem.service;

import com.cutting.cuttingsystem.entitys.AlgorithmTask;
import com.cutting.cuttingsystem.entitys.algorithm.DTO.SolutionResponseDTO;

import java.util.List;

public interface AlgorithmService {

    List<String> listAlgorithmNames();

    AlgorithmTask submit(String algorithm, String inputJson, Long userId);

    AlgorithmTask getTask(String taskId);

    List<AlgorithmTask> listTasksByUser(Long userId);

    void executeTask(String taskId);

    List<SolutionResponseDTO> executeSync(String algorithm, String inputJson);

    List<List<SolutionResponseDTO>> compare(String inputJson, List<String> algorithms);
}
