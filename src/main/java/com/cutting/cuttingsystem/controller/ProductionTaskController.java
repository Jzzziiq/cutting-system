package com.cutting.cuttingsystem.controller;

import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.DTO.OrderStatusTransitionDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TProductionTask;
import com.cutting.cuttingsystem.entitys.VO.TProductionTaskVO;
import com.cutting.cuttingsystem.service.TProductionTaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/production-tasks")
@Validated
@RequirePermission({"order:read", "order:write"})
public class ProductionTaskController {

    @Autowired
    private TProductionTaskService productionTaskService;

    @GetMapping("/kanban")
    public Result kanbanData() {
        Map<Integer, List<TProductionTaskVO>> data = productionTaskService.kanbanData();
        return Result.success(data);
    }

    @GetMapping("/order/{orderId}")
    public Result listByOrderId(@PathVariable @Positive Long orderId) {
        return Result.success(productionTaskService.listByOrderId(orderId));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable @Positive Long id) {
        TProductionTaskVO vo = productionTaskService.getTaskDetail(id);
        return vo != null ? Result.success(vo) : Result.error("任务不存在");
    }

    @PostMapping
    @AuditLog(module = "生产任务", action = "新增")
    public Result save(@RequestBody @Valid com.cutting.cuttingsystem.entitys.DTO.TProductionTaskDTO dto) {
        TProductionTask task = new TProductionTask();
        BeanUtils.copyProperties(dto, task);
        return Result.success(productionTaskService.createTask(task));
    }

    @PutMapping("/{id}")
    @AuditLog(module = "生产任务", action = "编辑")
    public Result update(@PathVariable @Positive Long id,
                         @RequestBody com.cutting.cuttingsystem.entitys.DTO.TProductionTaskDTO dto) {
        TProductionTask task = new TProductionTask();
        BeanUtils.copyProperties(dto, task);
        TProductionTaskVO vo = productionTaskService.updateTask(id, task);
        return vo != null ? Result.success(vo) : Result.error("任务不存在");
    }

    @PutMapping("/{id}/status")
    @AuditLog(module = "生产任务", action = "状态变更")
    public Result transitionStatus(@PathVariable @Positive Long id,
                                   @RequestBody @Valid OrderStatusTransitionDTO dto) {
        try {
            TProductionTaskVO vo = productionTaskService.transitionStatus(id, dto.getTargetStatus(), dto.getRemark());
            return Result.success(vo);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/assign")
    @AuditLog(module = "生产任务", action = "分配工人")
    public Result assign(@PathVariable @Positive Long id,
                         @RequestBody Map<String, Object> body) {
        Long assigneeId = body.get("assigneeId") != null
                ? ((Number) body.get("assigneeId")).longValue() : null;
        String assigneeName = body.get("assigneeName") != null
                ? body.get("assigneeName").toString() : null;
        return Result.success(productionTaskService.assignTask(id, assigneeId, assigneeName));
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "生产任务", action = "删除")
    public Result deleteById(@PathVariable @Positive Long id) {
        boolean removed = productionTaskService.removeById(id);
        return removed ? Result.success() : Result.error("删除失败");
    }
}
