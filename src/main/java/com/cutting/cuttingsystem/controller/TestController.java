package com.cutting.cuttingsystem.controller;

import com.cutting.cuttingsystem.entitys.algorithm.DTO.InstanceDTO;
import com.cutting.cuttingsystem.entitys.algorithm.Solution;
import com.cutting.cuttingsystem.util.ReadDataUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/algorithm")
public class TestController {
    @PostMapping("/answer")
    public List<Solution> answer(@RequestBody InstanceDTO instanceDTO) throws JsonProcessingException {
        ReadDataUtil readDataUtil = new ReadDataUtil();
        
        // 将 DTO 对象转换为 JSON 字符串
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String jsonStr = objectMapper.writeValueAsString(instanceDTO);
        
        // 调用 ReadDataUtil 获得解
        List<Solution> solutions = readDataUtil.getSolution(jsonStr);
        
        // 返回解给前端
        return solutions;
    }
}
