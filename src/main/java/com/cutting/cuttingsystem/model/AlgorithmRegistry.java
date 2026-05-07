package com.cutting.cuttingsystem.model;

import com.cutting.cuttingsystem.entitys.algorithm.Instance;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AlgorithmRegistry {

    public CuttingAlgorithm create(String name, Instance instance) {
        try {
            return switch (name) {
                case "tabu_search" -> new TabuSearch(instance);
                case "genetic_algorithm" -> new GeneticAlgorithm(instance);
                default -> throw new IllegalArgumentException("Unknown algorithm: " + name);
            };
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create algorithm: " + name, e);
        }
    }

    public Map<String, String> list() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("tabu_search", "禁忌搜索");
        map.put("genetic_algorithm", "遗传算法");
        return map;
    }
}
