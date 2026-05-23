package com.cutting.cuttingsystem.controller;

import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.DTO.OrderStatusTransitionDTO;
import com.cutting.cuttingsystem.entitys.DTO.ProductionTaskAssignDTO;
import com.cutting.cuttingsystem.entitys.DTO.TProductionTaskDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TProductionTask;
import com.cutting.cuttingsystem.entitys.VO.TProductionTaskDetailVO;
import com.cutting.cuttingsystem.entitys.VO.TProductionTaskVO;
import com.cutting.cuttingsystem.service.TProductionTaskService;
import com.cutting.cuttingsystem.util.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/production-tasks")
@Validated
public class ProductionTaskController {

    @Autowired
    private TProductionTaskService productionTaskService;

    @GetMapping("/kanban")
    @RequirePermission("order:write")
    public Result kanbanData() {
        Map<Integer, List<TProductionTaskVO>> data = productionTaskService.kanbanData();
        return Result.success(data);
    }

    @GetMapping("/my")
    @RequirePermission("order:read")
    public Result myTasks() {
        return Result.success(productionTaskService.listMyTasks(UserContext.getCurrentUserId()));
    }

    @GetMapping("/my/{taskId}")
    @RequirePermission("order:read")
    public Result myTaskDetail(@PathVariable @Positive Long taskId) {
        TProductionTaskDetailVO detail = productionTaskService.getMyTaskDetail(taskId, UserContext.getCurrentUserId());
        return detail != null ? Result.success(detail) : Result.error("任务不存在或未分配给当前用户");
    }

    @GetMapping("/order/{orderId}")
    @RequirePermission("order:read")
    public Result listByOrderId(@PathVariable @Positive Long orderId) {
        return Result.success(productionTaskService.listByOrderId(orderId));
    }

    @GetMapping("/{id}")
    @RequirePermission("order:write")
    public Result getById(@PathVariable @Positive Long id) {
        TProductionTaskVO vo = productionTaskService.getTaskDetail(id);
        return vo != null ? Result.success(vo) : Result.error("任务不存在");
    }

    @PostMapping
    @RequirePermission("order:write")
    @AuditLog(module = "生产任务", action = "新增")
    public Result save(@RequestBody @Valid TProductionTaskDTO dto) {
        TProductionTask task = new TProductionTask();
        BeanUtils.copyProperties(dto, task);
        return Result.success(productionTaskService.createTask(task));
    }

    @PutMapping("/{id}")
    @RequirePermission("order:write")
    @AuditLog(module = "生产任务", action = "编辑")
    public Result update(@PathVariable @Positive Long id,
                         @RequestBody TProductionTaskDTO dto) {
        TProductionTask task = new TProductionTask();
        BeanUtils.copyProperties(dto, task);
        TProductionTaskVO vo = productionTaskService.updateTask(id, task);
        return vo != null ? Result.success(vo) : Result.error("任务不存在");
    }

    @PutMapping("/{id}/status")
    @RequirePermission("order:write")
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
    @RequirePermission("order:write")
    @AuditLog(module = "生产任务", action = "分配工人")
    public Result assign(@PathVariable @Positive Long id,
                         @RequestBody Map<String, Object> body) {
        Long assigneeId = body.get("assigneeId") != null
                ? ((Number) body.get("assigneeId")).longValue() : null;
        String assigneeName = body.get("assigneeName") != null
                ? body.get("assigneeName").toString() : null;
        return Result.success(productionTaskService.assignTask(id, assigneeId, assigneeName));
    }

    @PutMapping("/order/{orderId}/assign")
    @RequirePermission("order:write")
    @AuditLog(module = "生产任务", action = "按订单分配生产")
    public Result assignByOrder(@PathVariable @Positive Long orderId,
                                @RequestBody @Valid ProductionTaskAssignDTO dto) {
        try {
            return Result.success(productionTaskService.assignOrderTask(orderId, dto.getAssigneeId()));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission("order:write")
    @AuditLog(module = "生产任务", action = "删除")
    public Result deleteById(@PathVariable @Positive Long id) {
        int deleted = productionTaskService.deleteByIdIgnoreTenant(id);
        return deleted > 0 ? Result.success() : Result.error("删除失败，任务不存在");
    }
}
