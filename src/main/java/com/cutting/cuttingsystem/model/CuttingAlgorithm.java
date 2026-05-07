package com.cutting.cuttingsystem.model;

import com.cutting.cuttingsystem.entitys.algorithm.Instance;
import com.cutting.cuttingsystem.entitys.algorithm.Solution;

public interface CuttingAlgorithm {

    String name();

    String displayName();

    Solution search() throws Exception;
}
